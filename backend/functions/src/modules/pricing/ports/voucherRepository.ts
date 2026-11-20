import { Voucher } from "../domain/voucher";

export interface VoucherRepository {
  findByCode(storeId: string, code: string): Promise<Voucher | null>;
  countUserRedemptions(voucherId: string, uid: string): Promise<number>;
}
