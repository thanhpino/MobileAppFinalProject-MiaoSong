import { CreatePaymentInput, PaymentInitResult, PaymentMethod, RefundResult } from "../domain/paymentTypes";

export interface PaymentGateway {
  readonly method: PaymentMethod;
  createPayment(input: CreatePaymentInput): Promise<PaymentInitResult>;
  queryStatus(appTransId: string): Promise<{ paid: boolean; zpTransId?: string }>;
  refund(zpTransId: string, amountVnd: number, reason: string): Promise<RefundResult>;
}
