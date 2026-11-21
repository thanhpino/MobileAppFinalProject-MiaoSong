import { LalamoveAdapter } from "../adapters/lalamoveAdapter";
import { MockDeliveryAdapter } from "../adapters/mockDeliveryAdapter";
import { DeliveryProvider } from "../ports/deliveryProvider";

export class DeliveryProviderFactory {
  private static readonly lalamove = new LalamoveAdapter();
  private static readonly mock = new MockDeliveryAdapter();

  static of(type: "LALAMOVE" | "MOCK"): DeliveryProvider {
    return type === "MOCK" ? this.mock : this.lalamove;
  }
}
