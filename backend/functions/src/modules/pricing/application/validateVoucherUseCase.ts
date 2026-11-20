import { AppError } from "../../../shared/errors";
import { calculateVoucherDiscount, validateVoucherContext, Voucher } from "../domain/voucher";
import { VoucherRepository } from "../ports/voucherRepository";

export interface ValidateVoucherResult {
  voucherId: string;
  code: string;
  type: Voucher["type"];
  value: number;
  maxDiscount: number | null;
  discount: number;
}

export class ValidateVoucherUseCase {
  constructor(private readonly repo: VoucherRepository) {}

  async execute(params: {
    storeId: string;
    code: string;
    subtotal: number;
    uid: string;
    now?: Date;
  }): Promise<ValidateVoucherResult> {
    const voucher = await this.repo.findByCode(params.storeId, params.code);
    if (!voucher) {
      throw new AppError("VOUCHER_NOT_FOUND", "Không tìm thấy mã giảm giá này.", {
        code: params.code,
      });
    }
    const userUsedCount = await this.repo.countUserRedemptions(voucher.id, params.uid);
    validateVoucherContext({
      voucher,
      subtotal: params.subtotal,
      userUsedCount,
      now: params.now ?? new Date(),
    });
    const discount = calculateVoucherDiscount(voucher, params.subtotal);
    return {
      voucherId: voucher.id,
      code: voucher.code,
      type: voucher.type,
      value: voucher.value,
      maxDiscount: voucher.maxDiscount ?? null,
      discount,
    };
  }
}
