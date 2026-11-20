import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { db } from "../../../shared/firebase";
import { FcmNotificationAdapter } from "../adapters/fcmNotificationAdapter";

const notifier = new FcmNotificationAdapter();

const CUSTOMER_STATUS_COPY: Record<string, { title: string; body: string }> = {
  CONFIRMED: {
    title: "Quán đã nhận đơn",
    body: "Quán đang chuẩn bị món cho bạn nè 🍲",
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

export const onOrderUpdated = onDocumentWritten("orders/{orderId}", async (event) => {
  const after = event.data?.after.data();
  const before = event.data?.before.data();
  if (!after) return;

  const newStatus = String(after.status ?? "");
  const oldStatus = before ? String(before.status ?? "") : "";
  if (newStatus === oldStatus) return;

  const orderId = event.params.orderId;
  const orderCode = String(after.code ?? orderId);
  const customerId = String(after.customerId ?? "");
  const storeId = String(after.storeId ?? "");

  // 1. Đơn mới -> báo chuông tới thiết bị Quán
  if (newStatus === "PLACED") {
    const merchantSnap = await db
      .collection("users")
      .where("role", "==", "MERCHANT")
      .where("storeId", "==", storeId)
      .get();
    const merchantUids = merchantSnap.docs.map((d) => d.id);
    const total = Number(after.pricing?.total ?? 0);
    await notifier.sendToUsers(merchantUids, {
      title: `🔔 Đơn mới #${orderCode}`,
      body: `${(after.items?.length ?? 0)} món · ${total.toLocaleString("vi-VN")}đ`,
      channelId: "miaosong_new_order",
      sound: "meow_bell.mp3",
      data: {
        type: "NEW_ORDER",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "MERCHANT",
        deepLink: `miaosong://merchant/orders/${orderId}`,
      },
    });
    return;
  }

  // 2. Đơn bị từ chối -> gửi lý do + trạng thái hoàn tiền cho khách
  if (newStatus === "REJECTED") {
    const reason = String(after.rejectReason ?? "hết nguyên liệu");
    await notifier.sendToUsers([customerId], {
      title: `Đơn #${orderCode} bị từ chối`,
      body: `Quán xin lỗi, đơn bị từ chối vì ${reason}.`,
      channelId: "miaosong_order_status",
      data: {
        type: "ORDER_STATUS",
        orderId,
        orderCode,
        status: newStatus,
        targetRole: "CUSTOMER",
        deepLink: `miaosong://customer/orders/${orderId}`,
      },
    });
    return;
  }

  // 3. Các trạng thái tiến độ giao hàng -> push khách
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
        deepLink: `miaosong://customer/orders/${orderId}`,
      },
    });
  }
});
