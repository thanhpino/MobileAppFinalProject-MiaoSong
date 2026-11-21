import { AppError } from "../../../shared/errors";

export type VoucherType = "PERCENT" | "FIXED";

export interface Voucher {
  id: string;
  code: string;
  type: VoucherType;
  value: number;
  maxDiscount?: number | null;
  minOrder: number;
  startAt: Date;
  endAt: Date;
  usageLimit: number;
  usedCount: number;
  perUserLimit: number;
  isActive: boolean;
}

export interface VoucherCheckContext {
  voucher: Voucher;
  subtotal: number;
  userUsedCount: number;
  now: Date;
}

export interface VoucherSpecification {
  check(ctx: VoucherCheckContext): void;
}

export class ActiveSpec implements VoucherSpecification {
  check({ voucher }: VoucherCheckContext): void {
    if (!voucher.isActive) {
      throw new AppError("VOUCHER_NOT_FOUND", "Mã giảm giá hiện không khả dụng.", {
        code: voucher.code,
      });
    }
  }
}

export class DateRangeSpec implements VoucherSpecification {
  check({ voucher, now }: VoucherCheckContext): void {
    if (now < voucher.startAt || now > voucher.endAt) {
      throw new AppError("VOUCHER_EXPIRED", "Mã giảm giá đã hết hạn hoặc chưa đến thời gian áp dụng.", {
        startAt: voucher.startAt.toISOString(),
        endAt: voucher.endAt.toISOString(),
      });
    }
  }
}

export class MinOrderSpec implements VoucherSpecification {
  check({ voucher, subtotal }: VoucherCheckContext): void {
    if (subtotal < voucher.minOrder) {
      const shortfall = voucher.minOrder - subtotal;
      throw new AppError(
        "VOUCHER_MIN_ORDER",
        `Đơn tối thiểu ${voucher.minOrder.toLocaleString("vi-VN")}đ, thêm ${shortfall.toLocaleString("vi-VN")}đ nữa để dùng mã nhé.`,
        { minOrder: voucher.minOrder, currentSubtotal: subtotal, shortfall },
      );
    }
  }
}

export class UsageLimitSpec implements VoucherSpecification {
  check({ voucher }: VoucherCheckContext): void {
    if (voucher.usedCount >= voucher.usageLimit) {
      throw new AppError("VOUCHER_EXHAUSTED", "Mã giảm giá đã hết lượt sử dụng.", {
        usageLimit: voucher.usageLimit,
      });
    }
  }
}

export class PerUserLimitSpec implements VoucherSpecification {
  check({ voucher, userUsedCount }: VoucherCheckContext): void {
    if (userUsedCount >= voucher.perUserLimit) {
      throw new AppError("VOUCHER_USER_LIMIT", "Bạn đã sử dụng hết số lần cho phép của mã này.", {
        perUserLimit: voucher.perUserLimit,
      });
    }
  }
}

const DEFAULT_SPECS: VoucherSpecification[] = [
  new ActiveSpec(),
  new DateRangeSpec(),
  new MinOrderSpec(),
  new UsageLimitSpec(),
  new PerUserLimitSpec(),
];

export function validateVoucherContext(ctx: VoucherCheckContext): void {
  for (const spec of DEFAULT_SPECS) {
    spec.check(ctx);
  }
}

/**
 * BR-VOU-01, BR-VOU-02, BR-PRICE-02:
 * Giảm giá chỉ tính trên subtotal (tiền món + tiền món thêm), KHÔNG trừ vào phí giao hàng.
 */
export function calculateVoucherDiscount(
  voucher: Pick<Voucher, "type" | "value" | "maxDiscount" | "minOrder">,
  subtotal: number,
): number {
  if (subtotal < voucher.minOrder) {
    throw new AppError(
      "VOUCHER_MIN_ORDER",
      `Đơn tối thiểu ${voucher.minOrder.toLocaleString("vi-VN")}đ.`,
      { minOrder: voucher.minOrder, currentSubtotal: subtotal, shortfall: voucher.minOrder - subtotal },
    );
  }

  let rawDiscount = 0;
  if (voucher.type === "PERCENT") {
    rawDiscount = Math.floor((subtotal * voucher.value) / 100);
    if (voucher.maxDiscount != null && voucher.maxDiscount > 0) {
      rawDiscount = Math.min(rawDiscount, voucher.maxDiscount);
    }
  } else {
    rawDiscount = voucher.value;
  }

  // Không bao giờ vượt quá subtotal (tuyệt đối không trừ vào phí giao hàng)
  return Math.max(0, Math.min(rawDiscount, subtotal));
}
