import { CodGateway } from "../adapters/codGateway";
import { ZaloPayGateway } from "../adapters/zalopayGateway";
import { PaymentMethod } from "../domain/paymentTypes";
import { PaymentGateway } from "../ports/paymentGateway";

export class PaymentGatewayFactory {
  private static readonly cod = new CodGateway();
  private static readonly zalopay = new ZaloPayGateway();

  static of(method: PaymentMethod): PaymentGateway {
    return method === "ZALOPAY" ? this.zalopay : this.cod;
  }

  static zaloPay(): ZaloPayGateway {
    return this.zalopay;
  }
}
