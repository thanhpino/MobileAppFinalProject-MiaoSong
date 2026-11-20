import { HttpsError, FunctionsErrorCode } from "firebase-functions/v2/https";
import { AppError, ErrorCode } from "./errors";

const ERROR_CODE_TO_HTTPS: Record<ErrorCode, FunctionsErrorCode> = {
  UNAUTHENTICATED: "unauthenticated",
  PERMISSION_DENIED: "permission-denied",
  STORE_CLOSED: "failed-precondition",
  ITEM_UNAVAILABLE: "failed-precondition",
  INVALID_OPTIONS: "invalid-argument",
  PRICE_CHANGED: "aborted",
  OUT_OF_RANGE: "failed-precondition",
  QUOTE_EXPIRED: "failed-precondition",
  QUOTE_UNAVAILABLE: "unavailable",
  VOUCHER_NOT_FOUND: "not-found",
  VOUCHER_EXPIRED: "failed-precondition",
  VOUCHER_MIN_ORDER: "failed-precondition",
  VOUCHER_EXHAUSTED: "resource-exhausted",
  VOUCHER_USER_LIMIT: "resource-exhausted",
  INVALID_TRANSITION: "failed-precondition",
  DISPATCH_FAILED: "unavailable",
  REFUND_FAILED: "internal",
};

export function toHttpsError(err: unknown): HttpsError {
  if (err instanceof HttpsError) return err;
  if (err instanceof AppError) {
    return new HttpsError(ERROR_CODE_TO_HTTPS[err.code], err.message, {
      code: err.code,
      ...(err.details ?? {}),
    });
  }
  const msg = err instanceof Error ? err.message : "Lỗi hệ thống không xác định.";
  return new HttpsError("internal", msg);
}

export function requireAuth(auth?: { uid: string; token: Record<string, unknown> }): {
  uid: string;
  role: string;
  storeId?: string;
} {
  if (!auth) {
    throw toHttpsError(new AppError("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
  }
  return {
    uid: auth.uid,
    role: String(auth.token.role ?? "CUSTOMER"),
    storeId: auth.token.storeId ? String(auth.token.storeId) : undefined,
  };
}

export function requireMerchant(
  auth: { uid: string; token: Record<string, unknown> } | undefined,
  expectedStoreId?: string,
): { uid: string; storeId: string } {
  const user = requireAuth(auth);
  if (user.role !== "MERCHANT" || !user.storeId) {
    throw toHttpsError(
      new AppError("PERMISSION_DENIED", "Chỉ tài khoản Quán mới có quyền thực hiện.", {
        requiredRole: "MERCHANT",
      }),
    );
  }
  if (expectedStoreId && user.storeId !== expectedStoreId) {
    throw toHttpsError(
      new AppError("PERMISSION_DENIED", "Bạn không có quyền thao tác trên quán này.", {
        requiredRole: "MERCHANT",
      }),
    );
  }
  return { uid: user.uid, storeId: user.storeId };
}
