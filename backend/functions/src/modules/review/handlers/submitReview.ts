import { onCall } from "firebase-functions/v2/https";
import { requireAuth, toHttpsError } from "../../../shared/auth";
import { AppError } from "../../../shared/errors";
import { admin, db } from "../../../shared/firebase";

export const submitReview = onCall(async (request) => {
  try {
    const user = requireAuth(request.auth);
    const { orderId, stars, tags, comment } = request.data as {
      orderId: string;
      stars: number;
      tags?: string[];
      comment?: string;
    };

    if (!Number.isInteger(stars) || stars < 1 || stars > 5) {
      throw new AppError("INVALID_OPTIONS", "Số sao đánh giá phải từ 1 đến 5.");
    }

    const orderRef = db.collection("orders").doc(orderId);
    const reviewRef = db.collection("reviews").doc(orderId);

    const result = await db.runTransaction(async (tx) => {
      const [orderSnap, reviewSnap] = await Promise.all([tx.get(orderRef), tx.get(reviewRef)]);
      if (!orderSnap.exists) {
        throw new AppError("PERMISSION_DENIED", "Không tìm thấy đơn hàng.");
      }
      const order = orderSnap.data()!;
      if (order.customerId !== user.uid) {
        throw new AppError("PERMISSION_DENIED", "Bạn không có quyền đánh giá đơn này.");
      }
      if (order.status !== "COMPLETED") {
        throw new AppError("INVALID_TRANSITION", "Chỉ được đánh giá sau khi đơn giao thành công.");
      }
      if (reviewSnap.exists) {
        throw new AppError("INVALID_TRANSITION", "Đơn hàng này đã được đánh giá rồi.");
      }

      const storeRef = db.collection("stores").doc(String(order.storeId));
      const storeSnap = await tx.get(storeRef);
      const prevCount = Number(storeSnap.data()?.ratingCount ?? 0);
      const prevAvg = Number(storeSnap.data()?.ratingAvg ?? 0);
      const newCount = prevCount + 1;
      const newAvg = Math.round(((prevAvg * prevCount + stars) / newCount) * 100) / 100;

      tx.set(reviewRef, {
        orderId,
        storeId: order.storeId,
        customerId: user.uid,
        stars,
        tags: Array.isArray(tags) ? tags.slice(0, 6) : [],
        comment: String(comment ?? "").trim().slice(0, 500),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      tx.update(storeRef, { ratingAvg: newAvg, ratingCount: newCount });

      return { reviewId: orderId, newStoreRatingAvg: newAvg, newStoreRatingCount: newCount };
    });

    return { ok: true, data: result };
  } catch (err) {
    throw toHttpsError(err);
  }
});
