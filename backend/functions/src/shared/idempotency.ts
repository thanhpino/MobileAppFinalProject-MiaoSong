import { db, admin } from "./firebase";

export type EventSource = "LALAMOVE" | "ZALOPAY" | "MOCK";

/**
 * Đảm bảo webhook / callback chỉ được xử lý đúng 1 lần (Idempotency).
 * Trả về true nếu đây là event MỚI và đã đánh dấu xử lý; trả về false nếu đã xử lý trước đó.
 */
export async function claimEventOnce(source: EventSource, eventId: string): Promise<boolean> {
  const docId = `${source}_${eventId}`;
  const ref = db.collection("processedEvents").doc(docId);
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (snap.exists) {
      return false;
    }
    tx.set(ref, {
      source,
      eventId,
      receivedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    return true;
  });
}
