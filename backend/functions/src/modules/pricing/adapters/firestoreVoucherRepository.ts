import { db } from "../../../shared/firebase";
import { Voucher } from "../domain/voucher";
import { VoucherRepository } from "../ports/voucherRepository";

export class FirestoreVoucherRepository implements VoucherRepository {
  async findByCode(storeId: string, code: string): Promise<Voucher | null> {
    const normalized = code.trim().toUpperCase();
    const snap = await db
      .collection("stores")
      .doc(storeId)
      .collection("vouchers")
      .where("code", "==", normalized)
      .limit(1)
      .get();
    if (snap.empty) return null;
    const doc = snap.docs[0];
    const d = doc.data();
    return {
      id: doc.id,
      code: String(d.code),
      type: d.type === "PERCENT" ? "PERCENT" : "FIXED",
      value: Number(d.value ?? 0),
      maxDiscount: d.maxDiscount != null ? Number(d.maxDiscount) : null,
      minOrder: Number(d.minOrder ?? 0),
      startAt: d.startAt?.toDate ? d.startAt.toDate() : new Date(0),
      endAt: d.endAt?.toDate ? d.endAt.toDate() : new Date(0),
      usageLimit: Number(d.usageLimit ?? 0),
      usedCount: Number(d.usedCount ?? 0),
      perUserLimit: Number(d.perUserLimit ?? 1),
      isActive: Boolean(d.isActive),
    };
  }

  async countUserRedemptions(voucherId: string, uid: string): Promise<number> {
    const snap = await db
      .collection("voucherRedemptions")
      .where("voucherId", "==", voucherId)
      .where("uid", "==", uid)
      .count()
      .get();
    return snap.data().count;
  }
}
