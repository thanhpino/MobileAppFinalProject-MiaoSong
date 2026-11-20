export type PaymentMethod = "COD" | "ZALOPAY";
export type PaymentStatus = "UNPAID" | "PAID" | "FAILED";
export type RefundStatus = "NONE" | "PENDING" | "REFUNDED" | "REFUND_FAILED";

export interface CreatePaymentInput {
  orderId: string;
  orderCode: string;
  amountVnd: number;
  userId: string;
  description: string;
}

export interface PaymentInitResult {
  method: PaymentMethod;
  status: PaymentStatus;
  appTransId?: string;
  paymentUrl?: string;
}

export interface RefundResult {
  success: boolean;
  refundId?: string;
  message?: string;
}
