import { randomUUID } from "crypto";
import {
  DeliveryQuote,
  DriverInfo,
  ExternalDeliveryOrder,
  GeoPoint,
  haversineKm,
} from "../domain/deliveryTypes";
import { CreateDeliveryRequest, DeliveryProvider } from "../ports/deliveryProvider";

/**
 * MockDeliveryAdapter: dùng cùng interface DeliveryProvider để demo trọn luồng
 * khi Lalamove Sandbox không có tài xế thật nhận chuyến.
 */
export class MockDeliveryAdapter implements DeliveryProvider {
  readonly name = "MOCK" as const;

  async quote(pickup: GeoPoint, dropoff: GeoPoint): Promise<DeliveryQuote> {
    const distanceKm = Math.max(0.5, haversineKm(pickup.lat, pickup.lng, dropoff.lat, dropoff.lng));
    // Công thức mô phỏng: 12.000đ cho 2km đầu + 4.000đ mỗi km tiếp theo
    const extraKm = Math.max(0, Math.ceil(distanceKm - 2));
    const shippingFee = 12000 + extraKm * 4000;
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString();

    return {
      quotationId: `qt_mock_${randomUUID().slice(0, 8)}`,
      provider: "MOCK",
      distanceKm,
      shippingFee,
      expiresAt,
      pickupStopId: "mock_stop_pickup",
      dropoffStopId: "mock_stop_dropoff",
    };
  }

  async createOrder(req: CreateDeliveryRequest): Promise<ExternalDeliveryOrder> {
    return {
      provider: "MOCK",
      externalOrderId: `mock_ord_${req.orderCode}_${Date.now()}`,
      shareLink: `https://miaosong-pro.web.app/mock-track/${req.orderId}`,
    };
  }

  async getDriver(): Promise<DriverInfo> {
    return {
      name: "Nguyễn Văn Minh",
      phone: "0909888777",
      plate: "59X1-123.45",
    };
  }

  async cancel(): Promise<void> {
    return;
  }
}
