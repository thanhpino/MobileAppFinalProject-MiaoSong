import { CreatePaymentInput, PaymentInitResult, RefundResult } from "../domain/paymentTypes";
import { PaymentGateway } from "../ports/paymentGateway";

export class CodGateway implements PaymentGateway {
  readonly method = "COD" as const;

  async createPayment(_input: CreatePaymentInput): Promise<PaymentInitResult> {
    return { method: "COD", status: "UNPAID" };
  }

  async queryStatus(): Promise<{ paid: boolean }> {
    return { paid: false };
  }

  async refund(): Promise<RefundResult> {
    // BR-PAY-03: COD không phát sinh hoàn tiền online
    return { success: true, message: "COD không cần hoàn tiền." };
  }
}
