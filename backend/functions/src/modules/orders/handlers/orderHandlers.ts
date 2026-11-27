import { onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { requireAuth, requireMerchant, toHttpsError } from "../../../shared/auth";
import { AppError } from "../../../shared/errors";
import { eventBus } from "../../../shared/eventBus";
import { admin, db } from "../../../shared/firebase";
import { catalogService } from "../../catalog";
import { DeliveryProviderFactory, DriverInfo } from "../../delivery";
import { PaymentGatewayFactory, PaymentMethod } from "../../payment";
import {
  calculatePriceBreakdown,
  computeCartLineKey,
  pricingService,
} from "../../pricing";
import { OrderStateMachine, OrderStatus } from "../domain/orderStateMachine";

// --- Subscribe Domain Events từ delivery & payment (Luật R4: không phụ thuộc vòng) ---
eventBus.subscribe("PaymentSucceeded", async (ev) => {
  const { orderId, zpTransId, appTransId } = ev.payload as {
    orderId: string;
    zpTransId: string;
    appTransId?: string;
  };
  const ref = db.collection("orders").doc(orderId);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return;
    const current = snap.data()!.status as OrderStatus;
    if (current !== "PENDING_PAYMENT") return;
    OrderStateMachine.assertTransition(current, "PLACED");
    tx.update(ref, {
      status: "PLACED",
      "payment.status": "PAID",
      "payment.gatewayTxnId": zpTransId,
      ...(appTransId ? { "payment.appTransId": appTransId } : {}),
      statusHistory: admin.firestore.FieldValue.arrayUnion({
        status: "PLACED",
        at: new Date().toISOString(),
        by: "ZALOPAY",
      }),
    });
  });
});

eventBus.subscribe("DeliveryStatusChanged", async (ev) => {
  const { orderId, externalOrderId, status, driver } = ev.payload as {
    orderId?: string;
    externalOrderId: string;
    status: OrderStatus;
    driver?: DriverInfo;
  };

  let targetRef = orderId ? db.collection("orders").doc(orderId) : null;
  if (!targetRef) {
    const q = await db
      .collection("orders")
      .where("delivery.externalId", "==", externalOrderId)
      .limit(1)
      .get();
    if (q.empty) return;
    targetRef = q.docs[0].ref;
  }

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(targetRef!);
    if (!snap.exists) return;
    const current = snap.data()!.status as OrderStatus;
    if (!OrderStateMachine.canTransition(current, status)) return;
    const updates: Record<string, unknown> = {
      status,
      statusHistory: admin.firestore.FieldValue.arrayUnion({
        status,
        at: new Date().toISOString(),
        by: "DELIVERY_WEBHOOK",
      }),
    };
    if (driver) {
      updates["delivery.driver"] = driver;
    }
    tx.update(targetRef!, updates);
  });
});

function buildOrderCode(): string {
  const vn = new Date(Date.now() + 7 * 60 * 60 * 1000);
  const yymmdd = `${String(vn.getUTCFullYear()).slice(-2)}${String(vn.getUTCMonth() + 1).padStart(2, "0")}${String(vn.getUTCDate()).padStart(2, "0")}`;
  const rnd = String(Math.floor(1000 + Math.random() * 9000));
  return `MS-${yymmdd}-${rnd}`;
}

export const placeOrder = onCall(async (request) => {
  try {
    const user = requireAuth(request.auth);
    const body = request.data as {
      idempotencyKey: string;
      storeId: string;
      address: {
        label?: string;
        fullText: string;
        lat: number;
        lng: number;
        recipientName: string;
        recipientPhone: string;
        deliveryNote?: string;
      };
      quotationId: string;
      shippingFee: number;
      voucherCode?: string | null;
      paymentMethod: PaymentMethod;
      items: Array<{
        menuItemId: string;
        sizeOptionId?: string | null;
        toppingOptionIds: string[];
        note?: string;
        quantity: number;
      }>;
    };

    // 1. Idempotency check
    const existing = await db
      .collection("orders")
      .where("customerId", "==", user.uid)
      .where("idempotencyKey", "==", body.idempotencyKey)
      .limit(1)
      .get();
    if (!existing.empty) {
      const d = existing.docs[0].data();
      return {
        ok: true,
        data: {
          orderId: existing.docs[0].id,
          orderCode: d.code,
          status: d.status,
          pricing: d.pricing,
          payment: d.payment,
        },
      };
    }

    // 2. Kiểm tra quán đang mở & resolve menu items
    const store = await catalogService.getStoreForOrder(body.storeId);
    const menuMap = await catalogService.resolveMenuItems(
      body.storeId,
      body.items.map((i) => i.menuItemId),
    );

    const snapshots: Array<Record<string, unknown>> = [];
    const pricedInputs: Array<{
      basePrice: number;
      sizeDelta: number;
      toppingsDelta: number[];
      quantity: number;
    }> = [];

    for (const reqItem of body.items) {
      const menuItem = menuMap.get(reqItem.menuItemId);
      if (!menuItem || !menuItem.isAvailable) {
        throw new AppError("ITEM_UNAVAILABLE", "Một số món hiện đã hết hàng.", {
          unavailableItemIds: [reqItem.menuItemId],
        });
      }

      let sizeDelta = 0;
      let sizeName = "";
      const toppingsDelta: number[] = [];
      const toppingNames: string[] = [];

      for (const group of menuItem.optionGroups) {
        if (group.type === "SINGLE") {
          const chosen = group.options.find((o) => o.id === reqItem.sizeOptionId);
          if (group.required && !chosen) {
            throw new AppError("INVALID_OPTIONS", `Vui lòng chọn ${group.name}.`, {
              groupId: group.id,
              groupName: group.name,
            });
          }
          if (chosen) {
            if (!chosen.isAvailable) {
              throw new AppError("ITEM_UNAVAILABLE", `${chosen.name} hiện đã hết.`);
            }
            sizeDelta += chosen.priceDelta;
            sizeName = chosen.name;
          }
        } else {
          const chosenList = group.options.filter((o) => reqItem.toppingOptionIds.includes(o.id));
          if (chosenList.length < group.min || (group.max > 0 && chosenList.length > group.max)) {
            throw new AppError("INVALID_OPTIONS", `Số lượng chọn trong ${group.name} không hợp lệ.`);
          }
          for (const opt of chosenList) {
            if (!opt.isAvailable) {
              throw new AppError("ITEM_UNAVAILABLE", `${opt.name} hiện đã hết.`);
            }
            toppingsDelta.push(opt.priceDelta);
            toppingNames.push(opt.name);
          }
        }
      }

      const unitPrice = menuItem.basePrice + sizeDelta + toppingsDelta.reduce((a, b) => a + b, 0);
      pricedInputs.push({
        basePrice: menuItem.basePrice,
        sizeDelta,
        toppingsDelta,
        quantity: reqItem.quantity,
      });
      snapshots.push({
        lineKey: computeCartLineKey({
          menuItemId: reqItem.menuItemId,
          sizeOptionId: reqItem.sizeOptionId,
          toppingOptionIds: reqItem.toppingOptionIds,
          note: reqItem.note,
        }),
        menuItemId: menuItem.id,
        itemName: menuItem.name,
        sizeOptionId: reqItem.sizeOptionId ?? null,
        sizeName,
        toppingOptionIds: reqItem.toppingOptionIds,
        toppingNames,
        note: reqItem.note ?? "",
        unitPrice,
        quantity: reqItem.quantity,
      });
    }

    // 3. Validate voucher & tính lại toàn bộ tiền ở server
    const prelimPricing = calculatePriceBreakdown(pricedInputs, null, body.shippingFee);
    let validatedVoucher = null;
    if (body.voucherCode) {
      validatedVoucher = await pricingService.validateVoucher.execute({
        storeId: body.storeId,
        code: body.voucherCode,
        subtotal: prelimPricing.subtotal,
        uid: user.uid,
      });
    }

    const finalPricing = calculatePriceBreakdown(
      pricedInputs,
      validatedVoucher
        ? {
            type: validatedVoucher.type,
            value: validatedVoucher.value,
            maxDiscount: validatedVoucher.maxDiscount,
            minOrder: 0,
          }
        : null,
      body.shippingFee,
    );

    // 4. Tạo giao dịch thanh toán (COD hoặc ZaloPay Sandbox)
    const orderRef = db.collection("orders").doc();
    const orderCode = buildOrderCode();
    const gateway = PaymentGatewayFactory.of(body.paymentMethod);
    const payInit = await gateway.createPayment({
      orderId: orderRef.id,
      orderCode,
      amountVnd: finalPricing.total,
      userId: user.uid,
      description: `MiaoSong thanh toan don #${orderCode}`,
    });
    const initialStatus: OrderStatus = body.paymentMethod === "COD" ? "PLACED" : "PENDING_PAYMENT";

    // 5. Transaction giữ voucher + ghi đơn hàng
    await db.runTransaction(async (tx) => {
      if (validatedVoucher) {
        const vRef = db
          .collection("stores")
          .doc(body.storeId)
          .collection("vouchers")
          .doc(validatedVoucher.voucherId);
        const vSnap = await tx.get(vRef);
        const vData = vSnap.data();
        if (!vData || Number(vData.usedCount ?? 0) >= Number(vData.usageLimit ?? 0)) {
          throw new AppError("VOUCHER_EXHAUSTED", "Mã giảm giá vừa hết lượt sử dụng.");
        }
        tx.update(vRef, { usedCount: admin.firestore.FieldValue.increment(1) });
        tx.set(db.collection("voucherRedemptions").doc(`${validatedVoucher.voucherId}_${user.uid}_${orderRef.id}`), {
          voucherId: validatedVoucher.voucherId,
          storeId: body.storeId,
          uid: user.uid,
          orderId: orderRef.id,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }

      tx.set(orderRef, {
        code: orderCode,
        customerId: user.uid,
        storeId: store.id,
        storeName: store.name,
        items: snapshots,
        pricing: finalPricing,
        voucherId: validatedVoucher?.voucherId ?? null,
        voucherCode: validatedVoucher?.code ?? null,
        address: body.address,
        payment: {
          method: body.paymentMethod,
          status: payInit.status,
          amountVnd: finalPricing.total,
          appTransId: payInit.appTransId ?? null,
          gatewayTxnId: null,
          paymentUrl: payInit.paymentUrl ?? null,
        },
        refundStatus: "NONE",
        status: initialStatus,
        statusHistory: [{ status: initialStatus, at: new Date().toISOString(), by: user.uid }],
        delivery: {
          provider: store.deliveryProvider,
          quotationId: body.quotationId,
          externalId: null,
          driver: null,
          shareLink: null,
        },
        rejectReason: null,
        idempotencyKey: body.idempotencyKey,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    return {
      ok: true,
      data: {
        orderId: orderRef.id,
        orderCode,
        status: initialStatus,
        pricing: finalPricing,
        payment: {
          method: body.paymentMethod,
          amountVnd: finalPricing.total,
          appTransId: payInit.appTransId ?? null,
          paymentUrl: payInit.paymentUrl ?? null,
        },
      },
    };
  } catch (err) {
    throw toHttpsError(err);
  }
});

export const confirmOrder = onCall(async (request) => {
  try {
    const merchant = requireMerchant(request.auth);
    const { orderId } = request.data as { orderId: string };
    const ref = db.collection("orders").doc(orderId);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists || snap.data()!.storeId !== merchant.storeId) {
        throw new AppError("PERMISSION_DENIED", "Không có quyền xác nhận đơn này.");
      }
      OrderStateMachine.assertTransition(snap.data()!.status as OrderStatus, "CONFIRMED");
      tx.update(ref, {
        status: "CONFIRMED",
        statusHistory: admin.firestore.FieldValue.arrayUnion({
          status: "CONFIRMED",
          at: new Date().toISOString(),
          by: merchant.uid,
        }),
      });
    });
    return { ok: true, data: { orderId, status: "CONFIRMED" } };
  } catch (err) {
    throw toHttpsError(err);
  }
});

export const rejectOrder = onCall(async (request) => {
  try {
    const merchant = requireMerchant(request.auth);
    const { orderId, reasonCode, reasonText, disableMenuItemIds } = request.data as {
      orderId: string;
      reasonCode: string;
      reasonText: string;
      disableMenuItemIds?: string[];
    };

    const ref = db.collection("orders").doc(orderId);
    let paymentToRefund: { method: PaymentMethod; zpTransId: string; amountVnd: number } | null = null;

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists || snap.data()!.storeId !== merchant.storeId) {
        throw new AppError("PERMISSION_DENIED", "Không có quyền từ chối đơn này.");
      }
      const order = snap.data()!;
      OrderStateMachine.assertTransition(order.status as OrderStatus, "REJECTED");

      if (order.voucherId) {
        const vRef = db.collection("stores").doc(merchant.storeId).collection("vouchers").doc(order.voucherId);
        tx.update(vRef, { usedCount: admin.firestore.FieldValue.increment(-1) });
      }

      if (order.payment?.status === "PAID" && order.payment?.method === "ZALOPAY" && order.payment?.gatewayTxnId) {
        paymentToRefund = {
          method: "ZALOPAY",
          zpTransId: String(order.payment.gatewayTxnId),
          amountVnd: Number(order.payment.amountVnd),
        };
      }

      tx.update(ref, {
        status: "REJECTED",
        rejectReason: reasonText || reasonCode,
        refundStatus: paymentToRefund ? "PENDING" : "NONE",
        statusHistory: admin.firestore.FieldValue.arrayUnion({
          status: "REJECTED",
          at: new Date().toISOString(),
          by: merchant.uid,
        }),
      });

      for (const itemId of disableMenuItemIds ?? []) {
        const itemRef = db.collection("stores").doc(merchant.storeId).collection("menuItems").doc(itemId);
        tx.update(itemRef, { isAvailable: false });
      }
    });

    let refundStatus = paymentToRefund ? "PENDING" : "NONE";
    if (paymentToRefund) {
      const p = paymentToRefund as { method: PaymentMethod; zpTransId: string; amountVnd: number };
      const res = await PaymentGatewayFactory.of(p.method).refund(p.zpTransId, p.amountVnd, reasonText);
      refundStatus = res.success ? "REFUNDED" : "REFUND_FAILED";
      await ref.update({ refundStatus });
    }

    return { ok: true, data: { orderId, status: "REJECTED", refundStatus } };
  } catch (err) {
    throw toHttpsError(err);
  }
});

export const markReady = onCall(async (request) => {
  try {
    const merchant = requireMerchant(request.auth);
    const { orderId } = request.data as { orderId: string };
    const ref = db.collection("orders").doc(orderId);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists || snap.data()!.storeId !== merchant.storeId) {
        throw new AppError("PERMISSION_DENIED", "Không có quyền thao tác đơn này.");
      }
      OrderStateMachine.assertTransition(snap.data()!.status as OrderStatus, "READY");
      tx.update(ref, {
        status: "READY",
        statusHistory: admin.firestore.FieldValue.arrayUnion({
          status: "READY",
          at: new Date().toISOString(),
          by: merchant.uid,
        }),
      });
    });
    return { ok: true, data: { orderId, status: "READY" } };
  } catch (err) {
    throw toHttpsError(err);
  }
});

export const dispatchDriver = onCall(async (request) => {
  try {
    const merchant = requireMerchant(request.auth);
    const { orderId } = request.data as { orderId: string };
    const ref = db.collection("orders").doc(orderId);
    const snap = await ref.get();
    if (!snap.exists || snap.data()!.storeId !== merchant.storeId) {
      throw new AppError("PERMISSION_DENIED", "Không có quyền gọi shipper cho đơn này.");
    }
    const order = snap.data()!;
    OrderStateMachine.assertTransition(order.status as OrderStatus, "FINDING_DRIVER");

    const store = await catalogService.getStoreInfo(merchant.storeId);
    const provider = DeliveryProviderFactory.of(store.deliveryProvider);
    const pickup = {
      address: store.address,
      lat: store.lat,
      lng: store.lng,
      name: store.name,
      phone: store.phone,
    };
    const dropoff = {
      address: String(order.address?.fullText ?? ""),
      lat: Number(order.address?.lat ?? 0),
      lng: Number(order.address?.lng ?? 0),
      name: String(order.address?.recipientName ?? ""),
      phone: String(order.address?.recipientPhone ?? ""),
    };

    // Báo giá lại để đảm bảo quotationId luôn còn hiệu lực (BR-SHIP-04: chênh lệch quán chịu)
    const freshQuote = await provider.quote(pickup, dropoff);
    const codAmount = order.payment?.method === "COD" ? Number(order.pricing?.total ?? 0) : 0;

    const ext = await provider.createOrder({
      orderId,
      orderCode: String(order.code ?? orderId),
      quotationId: freshQuote.quotationId,
      pickup,
      dropoff,
      codAmount,
      deliveryNote: order.address?.deliveryNote,
      pickupStopId: freshQuote.pickupStopId,
      dropoffStopId: freshQuote.dropoffStopId,
    });

    await ref.update({
      status: "FINDING_DRIVER",
      "delivery.provider": ext.provider,
      "delivery.externalId": ext.externalOrderId,
      "delivery.shareLink": ext.shareLink ?? null,
      statusHistory: admin.firestore.FieldValue.arrayUnion({
        status: "FINDING_DRIVER",
        at: new Date().toISOString(),
        by: merchant.uid,
      }),
    });

    return {
      ok: true,
      data: {
        orderId,
        status: "FINDING_DRIVER",
        provider: ext.provider,
        externalOrderId: ext.externalOrderId,
        shareLink: ext.shareLink ?? null,
        codAmountToCollect: codAmount,
      },
    };
  } catch (err) {
    throw toHttpsError(err);
  }
});

export const cancelOrder = onCall(async (request) => {
  try {
    const user = requireAuth(request.auth);
    const { orderId, reason } = request.data as { orderId: string; reason?: string };
    const ref = db.collection("orders").doc(orderId);
    let paymentToRefund: { method: PaymentMethod; zpTransId: string; amountVnd: number } | null = null;
    const cancelReason = reason ?? "Khách hàng hủy đơn trước khi quán xác nhận";

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists || snap.data()!.customerId !== user.uid) {
        throw new AppError("PERMISSION_DENIED", "Bạn không có quyền hủy đơn này.");
      }
      const order = snap.data()!;
      OrderStateMachine.assertTransition(order.status as OrderStatus, "CANCELLED");
      if (order.voucherId) {
        const vRef = db.collection("stores").doc(order.storeId).collection("vouchers").doc(order.voucherId);
        tx.update(vRef, { usedCount: admin.firestore.FieldValue.increment(-1) });
      }
      if (order.payment?.status === "PAID" && order.payment?.method === "ZALOPAY" && order.payment?.gatewayTxnId) {
        paymentToRefund = {
          method: "ZALOPAY",
          zpTransId: String(order.payment.gatewayTxnId),
          amountVnd: Number(order.payment.amountVnd),
        };
      }
      tx.update(ref, {
        status: "CANCELLED",
        rejectReason: cancelReason,
        refundStatus: paymentToRefund ? "PENDING" : "NONE",
        statusHistory: admin.firestore.FieldValue.arrayUnion({
          status: "CANCELLED",
          at: new Date().toISOString(),
          by: user.uid,
        }),
      });
    });

    let refundStatus = paymentToRefund ? "PENDING" : "NONE";
    if (paymentToRefund) {
      const p = paymentToRefund as { method: PaymentMethod; zpTransId: string; amountVnd: number };
      const res = await PaymentGatewayFactory.of(p.method).refund(p.zpTransId, p.amountVnd, cancelReason);
      refundStatus = res.success ? "REFUNDED" : "REFUND_FAILED";
      await ref.update({ refundStatus });
    }

    return { ok: true, data: { orderId, status: "CANCELLED", refundStatus } };
  } catch (err) {
    throw toHttpsError(err);
  }
});

/** Scheduler mỗi 5 phút: tự hủy đơn PENDING_PAYMENT quá 15 phút & hoàn lượt voucher */
export const expirePendingOrders = onSchedule("every 5 minutes", async () => {
  const cutoff = admin.firestore.Timestamp.fromDate(new Date(Date.now() - 15 * 60 * 1000));
  const snap = await db
    .collection("orders")
    .where("status", "==", "PENDING_PAYMENT")
    .where("createdAt", "<", cutoff)
    .limit(50)
    .get();

  for (const doc of snap.docs) {
    await db.runTransaction(async (tx) => {
      const fresh = await tx.get(doc.ref);
      if (!fresh.exists || fresh.data()!.status !== "PENDING_PAYMENT") return;
      const order = fresh.data()!;
      if (order.voucherId) {
        const vRef = db.collection("stores").doc(order.storeId).collection("vouchers").doc(order.voucherId);
        tx.update(vRef, { usedCount: admin.firestore.FieldValue.increment(-1) });
      }
      tx.update(doc.ref, {
        status: "CANCELLED",
        rejectReason: "Hết thời gian thanh toán (15 phút)",
        statusHistory: admin.firestore.FieldValue.arrayUnion({
          status: "CANCELLED",
          at: new Date().toISOString(),
          by: "SCHEDULER",
        }),
      });
    });
  }
});
