import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { db } from "../../../shared/firebase";
import { FcmNotificationAdapter } from "../adapters/fcmNotificationAdapter";

const notifier = new FcmNotificationAdapter();

const CUSTOMER_STATUS_COPY: Record<string, { title: string; body: string }> = {
  CONFIRMED: {
    title: "Quán đã nhận đơn",
    body: "Quán đang chuẩn bị món cho bạn nè 🍲",
  },
  READY: {
    title: "Quán đã làm xong món",
    body: "Món đã đóng gói xong, quán đang chuẩn bị gọi tài xế giao cho bạn.",
  },
  FINDING_DRIVER: {
    title: "Đang tìm tài xế",
    body: "Quán đã đóng gói xong và đang gọi tài xế Lalamove 🛵",
  },
  DRIVER_ASSIGNED: {
    title: "Đã có tài xế nhận đơn",
    body: "Tài xế đang trên đường tới quán lấy món cho bạn.",
  },
  ARRIVED_AT_STORE: {
    title: "Tài xế đã tới quán",
    body: "Tài xế đang nhận món tại quán.",
  },
  PICKED_UP: {
    title: "Tài xế đã lấy món",
    body: "Món nóng hổi đang trên đường tới bạn 🛵",
  },
  DELIVERING: {
    title: "Đang giao tới bạn",
    body: "Chú ý điện thoại nhé, tài xế sắp tới rồi!",
  },
  COMPLETED: {
    title: "Giao hàng thành công!",
    body: "Chúc bạn ngon miệng! Đánh giá cho quán nhé ⭐",
  },
};

async function getMerchantUids(storeId: string): Promise<string[]> {
  if (!storeId) return [];
  const snap = await db
    .collection("users")
    .where("role", "==", "MERCHANT")
    .where("storeId", "==", storeId)
    .get();
  return snap.docs.map((d) => d.id);
}

export const onOrderUpdated = onDocumentWritten("orders/{orderId}", async (event) => {
  const after = event.data?.after.data();
  const before = event.data?.before.data();
  if (!after) return;

  const newStatus = String(after.status ?? "");
  const oldStatus = before ? String(before.status ?? "") : "";
  const newRefund = String(after.refundStatus ?? "NONE");
  const oldRefund = before ? String(before.refundStatus ?? "NONE") : "NONE";

  const statusChanged = newStatus !== oldStatus;
  const refundChanged = newRefund !== oldRefund;
  if (!statusChanged && !refundChanged) return;

  const orderId = event.params.orderId;
  const orderCode = String(after.code ?? orderId);
  const customerId = String(after.customerId ?? "");
  const storeId = String(after.storeId ?? "");
  const totalVnd = Number(after.pricing?.total ?? after.payment?.amountVnd ?? 0);
  const totalFormatted = `${totalVnd.toLocaleString("vi-VN")}đ`;

  // A. Thông báo cập nhật trạng thái HOÀN TIỀN (REFUNDED / REFUND_FAILED) kể cả khi status không đổi
  if (refundChanged && (newRefund === "REFUNDED" || newRefund === "REFUND_FAILED")) {
    if (newRefund === "REFUNDED") {
      await notifier.sendToUsers([customerId], {
        title: `💸 Đã hoàn tiền đơn #${orderCode}`,
        body: `Số tiền ${totalFormatted} đã được hoàn về ZaloPay của bạn.`,
        channelId: "miaosong_order_status",
        data: {
          type: "ORDER_STATUS",
          orderId,
          orderCode,
          status: newStatus,
          refundStatus: newRefund,
          targetRole: "CUSTOMER",
        },
      });
    } else if (newRefund === "REFUND_FAILED") {
      const merchantUids = await getMerchantUids(storeId);
      await notifier.sendToUsers([customerId], {
        title: `⚠️ Hoàn tiền ZaloPay gặp sự cố · #${orderCode}`,
        body: `Chưa thể hoàn tự động ${totalFormatted}. Vui lòng liên hệ hỗ trợ để xử lý ngay.`,
        channelId: "miaosong_late_alert",
        data: {
          type: "LATE_ALERT",
          orderId,
          orderCode,
          status: newStatus,
          refundStatus: newRefund,
          targetRole: "CUSTOMER",
        },
      });
      await notifier.sendToUsers(merchantUids, {
        title: `⚠️ Lỗi hoàn tiền đơn #${orderCode}`,
        body: `Hoàn tiền ZaloPay ${totalFormatted} cho khách bị lỗi — cần kiểm tra thủ công.`,
        channelId: "miaosong_late_alert",
        data: {
          type: "LATE_ALERT",
          orderId,
          orderCode,
          status: newStatus,
          refundStatus: newRefund,
          targetRole: "MERCHANT",
        },
      });
    }
  }

  if (!statusChanged) return;

  // B1. Đơn mới tạo chờ thanh toán ZaloPay
  if (newStatus === "PENDING_PAYMENT") {
    await notifier.sendToUsers([customerId], {
      title: `⏳ Chờ thanh toán ZaloPay · #${orderCode}`,
      body: `Vui lòng hoàn tất thanh toán ${totalFormatted} trong 15 phút để gửi đơn đến quán.`,
      channelId: "miaosong_order_status",
      data: {
        type: "ORDER_STATUS",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "CUSTOMER",
      },
    });
    return;
  }

  // B2. Đơn mới đã đặt thành công (COD hoặc ZaloPay PAID) -> báo chuông Quán + xác nhận cho Khách
  if (newStatus === "PLACED") {
    const merchantUids = await getMerchantUids(storeId);
    await notifier.sendToUsers(merchantUids, {
      title: `🔔 Đơn mới #${orderCode}`,
      body: `${after.items?.length ?? 0} món · ${totalFormatted}`,
      channelId: "miaosong_new_order",
      sound: "meow_bell.mp3",
      data: {
        type: "NEW_ORDER",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "MERCHANT",
      },
    });
    await notifier.sendToUsers([customerId], {
      title: `✅ Đặt đơn thành công · #${orderCode}`,
      body: `Đơn ${totalFormatted} đã gửi tới quán. Chờ quán xác nhận nhận đơn nhé!`,
      channelId: "miaosong_order_status",
      data: {
        type: "ORDER_STATUS",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "CUSTOMER",
      },
    });
    return;
  }

  // B3. Quán từ chối đơn -> báo Khách kèm lý do + thông tin hoàn tiền
  if (newStatus === "REJECTED") {
    const reason = String(after.rejectReason ?? "hết nguyên liệu");
    const refundHint =
      newRefund === "PENDING" || newRefund === "REFUNDED"
        ? " Hệ thống đang tự động hoàn tiền ZaloPay & hoàn lượt voucher cho bạn."
        : "";
    await notifier.sendToUsers([customerId], {
      title: `❌ Đơn #${orderCode} bị quán từ chối`,
      body: `Quán xin lỗi, đơn bị từ chối vì ${reason}.${refundHint}`,
      channelId: "miaosong_order_status",
      data: {
        type: "ORDER_STATUS",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "CUSTOMER",
      },
    });
    return;
  }

  // B4. Đơn bị hủy (Khách tự hủy hoặc hệ thống hủy quá hạn 15 phút)
  if (newStatus === "CANCELLED") {
    const reason = String(after.rejectReason ?? "Đơn hàng đã được hủy");
    const refundHint =
      newRefund === "PENDING" || newRefund === "REFUNDED"
        ? " Đang tự động hoàn tiền ZaloPay & hoàn lượt voucher."
        : "";
    await notifier.sendToUsers([customerId], {
      title: `Đơn #${orderCode} đã bị hủy`,
      body: `${reason}.${refundHint}`,
      channelId: "miaosong_order_status",
      data: {
        type: "ORDER_STATUS",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "CUSTOMER",
      },
    });
    // Nếu đơn đã từng gửi tới quán (PLACED) rồi khách hủy -> báo ngay cho Bếp Quán
    if (oldStatus === "PLACED") {
      const merchantUids = await getMerchantUids(storeId);
      await notifier.sendToUsers(merchantUids, {
        title: `⚠️ Khách đã hủy đơn #${orderCode}`,
        body: `Khách vừa hủy đơn trước khi xác nhận — bếp không cần chuẩn bị đơn này.`,
        channelId: "miaosong_new_order",
        data: {
          type: "NEW_ORDER",
          orderId,
          orderCode,
          status: newStatus,
          targetRole: "MERCHANT",
        },
      });
    }
    return;
  }

  // B5. Ngoại lệ giao hàng thất bại -> báo cả Khách lẫn Quán
  if (newStatus === "DELIVERY_FAILED") {
    const merchantUids = await getMerchantUids(storeId);
    await notifier.sendToUsers([customerId], {
      title: `⚠️ Giao hàng không thành công · #${orderCode}`,
      body: "Tài xế không thể hoàn tất giao đơn. Vui lòng kiểm tra chi tiết hoặc liên hệ quán.",
      channelId: "miaosong_late_alert",
      data: {
        type: "LATE_ALERT",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "CUSTOMER",
      },
    });
    await notifier.sendToUsers(merchantUids, {
      title: `⚠️ Giao hàng thất bại · #${orderCode}`,
      body: "Chuyến giao Lalamove báo thất bại — vui lòng liên hệ tài xế & khách hàng kiểm tra.",
      channelId: "miaosong_late_alert",
      data: {
        type: "LATE_ALERT",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "MERCHANT",
      },
    });
    return;
  }

  // B6. Các mốc tiến độ bình thường (CONFIRMED -> READY -> FINDING_DRIVER -> DRIVER_ASSIGNED -> ... -> COMPLETED)
  const copy = CUSTOMER_STATUS_COPY[newStatus];
  if (copy) {
    const driver = after.delivery?.driver as { name?: string; plate?: string } | undefined;
    const body =
      newStatus === "DRIVER_ASSIGNED" && driver?.name
        ? `Tài xế ${driver.name} (${driver.plate ?? ""}) đang tới quán lấy món.`
        : copy.body;

    await notifier.sendToUsers([customerId], {
      title: `${copy.title} · #${orderCode}`,
      body,
      channelId: "miaosong_order_status",
      data: {
        type: "ORDER_STATUS",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "CUSTOMER",
      },
    });
  }
});
