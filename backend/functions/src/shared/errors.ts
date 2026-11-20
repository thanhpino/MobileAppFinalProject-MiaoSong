/** 17 mã lỗi chuẩn từ docs/08-api-contract.md §2 — thuần TypeScript, không phụ thuộc Firebase */
export type ErrorCode =
  | "UNAUTHENTICATED"
  | "PERMISSION_DENIED"
  | "STORE_CLOSED"
  | "ITEM_UNAVAILABLE"
  | "INVALID_OPTIONS"
  | "PRICE_CHANGED"
  | "OUT_OF_RANGE"
  | "QUOTE_EXPIRED"
  | "QUOTE_UNAVAILABLE"
  | "VOUCHER_NOT_FOUND"
  | "VOUCHER_EXPIRED"
  | "VOUCHER_MIN_ORDER"
  | "VOUCHER_EXHAUSTED"
  | "VOUCHER_USER_LIMIT"
  | "INVALID_TRANSITION"
  | "DISPATCH_FAILED"
  | "REFUND_FAILED";

export class AppError extends Error {
  constructor(
    public readonly code: ErrorCode,
    message: string,
    public readonly details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = "AppError";
  }
}
