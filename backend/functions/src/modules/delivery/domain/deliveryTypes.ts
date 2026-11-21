export interface GeoPoint {
  address: string;
  lat: number;
  lng: number;
  name?: string;
  phone?: string;
}

export interface DeliveryQuote {
  quotationId: string;
  provider: "LALAMOVE" | "MOCK";
  distanceKm: number;
  shippingFee: number;
  expiresAt: string; // ISO string
  pickupStopId?: string;
  dropoffStopId?: string;
}

export interface ExternalDeliveryOrder {
  provider: "LALAMOVE" | "MOCK";
  externalOrderId: string;
  shareLink?: string;
}

export interface DriverInfo {
  name: string;
  phone: string;
  plate: string;
}

export type MappedDeliveryStatus =
  | "FINDING_DRIVER"
  | "DRIVER_ASSIGNED"
  | "ARRIVED_AT_STORE"
  | "PICKED_UP"
  | "DELIVERING"
  | "COMPLETED"
  | "READY"
  | "DELIVERY_FAILED";

export interface DeliveryEventPayload {
  eventId: string;
  externalOrderId: string;
  orderId?: string;
  status: MappedDeliveryStatus;
  driver?: DriverInfo;
}

/** Tính khoảng cách đường chim bay (km) bằng công thức Haversine */
export function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2;
  return Math.round(R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10) / 10;
}
