import { onCall, onRequest } from "firebase-functions/v2/https";
import { requireAuth, requireMerchant, toHttpsError } from "../../../shared/auth";
import { AppError } from "../../../shared/errors";
import { eventBus } from "../../../shared/eventBus";
import { claimEventOnce } from "../../../shared/idempotency";
import { catalogService } from "../../catalog";
import { DeliveryProviderFactory } from "../application/deliveryProviderFactory";
import { DriverInfo, GeoPoint, haversineKm, MappedDeliveryStatus } from "../domain/deliveryTypes";

export const getDeliveryQuote = onCall(async (request) => {
  try {
    requireAuth(request.auth);
    const { storeId, dropoff } = request.data as { storeId: string; dropoff: GeoPoint };
    const store = await catalogService.getStoreInfo(storeId);

    const distance = haversineKm(store.lat, store.lng, dropoff.lat, dropoff.lng);
    if (distance > store.maxDistanceKm) {
      throw new AppError("OUT_OF_RANGE", "Quán chưa hỗ trợ giao tới địa chỉ này.", {
        distanceKm: distance,
        maxDistanceKm: store.maxDistanceKm,
      });
    }

    const provider = DeliveryProviderFactory.of(store.deliveryProvider);
    const quote = await provider.quote(
      { address: store.address, lat: store.lat, lng: store.lng, name: store.name, phone: store.phone },
      dropoff,
    );
    return { ok: true, data: quote };
  } catch (err) {
    throw toHttpsError(err);
  }
});

const LALAMOVE_STATUS_MAP: Record<string, MappedDeliveryStatus> = {
  ASSIGNING_DRIVER: "FINDING_DRIVER",
  ON_GOING: "DRIVER_ASSIGNED",
  PICKED_UP: "PICKED_UP",
  COMPLETED: "COMPLETED",
  CANCELED: "READY",
  REJECTED: "READY",
  EXPIRED: "READY",
};

export const lalamoveWebhook = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }
  const body = req.body as {
    eventId?: string;
    eventType?: string;
    data?: {
      order?: { orderId: string; status: string; driverId?: string; metadata?: { orderId?: string } };
    };
  };
  const eventId = body.eventId ?? `ll_${Date.now()}`;
  const isNew = await claimEventOnce("LALAMOVE", eventId);
  if (!isNew) {
    res.status(200).json({ ok: true, duplicate: true });
    return;
  }

  const llOrder = body.data?.order;
  if (!llOrder) {
    res.status(200).json({ ok: true, ignored: true });
    return;
  }

  const mappedStatus = LALAMOVE_STATUS_MAP[llOrder.status];
  if (mappedStatus) {
    let driver: DriverInfo | undefined;
    if (mappedStatus === "DRIVER_ASSIGNED" && llOrder.driverId) {
      try {
        driver = await DeliveryProviderFactory.of("LALAMOVE").getDriver(
          llOrder.orderId,
          llOrder.driverId,
        );
      } catch {
        // Bỏ qua nếu sandbox chưa trả thông tin tài xế
      }
    }
    await eventBus.publish({
      type: "DeliveryStatusChanged",
      occurredAt: new Date(),
      payload: {
        eventId,
        externalOrderId: llOrder.orderId,
        orderId: llOrder.metadata?.orderId,
        status: mappedStatus,
        driver,
      },
    });
  }
  res.status(200).json({ ok: true });
});

/** Endpoint giả lập tài xế chuyển trạng thái khi demo (chỉ chạy ở dev/MOCK) */
export const mockDeliveryAdvance = onCall(async (request) => {
  try {
    requireMerchant(request.auth);
    const { orderId, targetStatus, mockDriver } = request.data as {
      orderId: string;
      targetStatus: MappedDeliveryStatus;
      mockDriver?: DriverInfo;
    };
    await eventBus.publish({
      type: "DeliveryStatusChanged",
      occurredAt: new Date(),
      payload: {
        eventId: `mock_${orderId}_${targetStatus}_${Date.now()}`,
        externalOrderId: `mock_${orderId}`,
        orderId,
        status: targetStatus,
        driver: mockDriver ?? { name: "Nguyễn Văn Minh", phone: "0909888777", plate: "59X1-123.45" },
      },
    });
    return { ok: true, data: { orderId, status: targetStatus } };
  } catch (err) {
    throw toHttpsError(err);
  }
});
