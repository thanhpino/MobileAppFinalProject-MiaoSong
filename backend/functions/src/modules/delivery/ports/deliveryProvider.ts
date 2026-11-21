import {
  DeliveryQuote,
  DriverInfo,
  ExternalDeliveryOrder,
  GeoPoint,
} from "../domain/deliveryTypes";

export interface CreateDeliveryRequest {
  orderId: string;
  orderCode: string;
  quotationId: string;
  pickup: GeoPoint;
  dropoff: GeoPoint;
  codAmount: number;
  deliveryNote?: string;
  pickupStopId?: string;
  dropoffStopId?: string;
}

export interface DeliveryProvider {
  readonly name: "LALAMOVE" | "MOCK";
  quote(pickup: GeoPoint, dropoff: GeoPoint): Promise<DeliveryQuote>;
  createOrder(req: CreateDeliveryRequest): Promise<ExternalDeliveryOrder>;
  getDriver(externalOrderId: string, driverId: string): Promise<DriverInfo>;
  cancel(externalOrderId: string): Promise<void>;
}
