import { createHmac, randomUUID } from "crypto";
import { AppError } from "../../../shared/errors";
import {
  DeliveryQuote,
  DriverInfo,
  ExternalDeliveryOrder,
  GeoPoint,
  haversineKm,
} from "../domain/deliveryTypes";
import { CreateDeliveryRequest, DeliveryProvider } from "../ports/deliveryProvider";

/**
 * Adapter cho Lalamove API v3 (Sandbox, Market: VN, serviceType: MOTORCYCLE).
 */
export class LalamoveAdapter implements DeliveryProvider {
  readonly name = "LALAMOVE" as const;

  private get apiKey(): string {
    return process.env.LALAMOVE_API_KEY ?? "";
  }
  private get apiSecret(): string {
    return process.env.LALAMOVE_API_SECRET ?? "";
  }
  private get baseUrl(): string {
    return process.env.LALAMOVE_BASE_URL ?? "https://rest.sandbox.lalamove.com";
  }
  private get market(): string {
    return process.env.LALAMOVE_MARKET ?? "VN";
  }

  private sign(method: string, path: string, bodyStr: string): { token: string; requestId: string } {
    const time = Date.now().toString();
    const raw = `${time}\r\n${method}\r\n${path}\r\n\r\n${bodyStr}`;
    const signature = createHmac("sha256", this.apiSecret).update(raw).digest("hex");
    return {
      token: `hmac ${this.apiKey}:${time}:${signature}`,
      requestId: randomUUID(),
    };
  }

  private async request<T>(method: "GET" | "POST" | "DELETE", path: string, body?: unknown): Promise<T> {
    if (!this.apiKey || !this.apiSecret || this.apiSecret.includes("PASTE_")) {
      throw new AppError("QUOTE_UNAVAILABLE", "Chưa cấu hình khóa API Lalamove.");
    }
    const bodyStr = body ? JSON.stringify(body) : "";
    const { token, requestId } = this.sign(method, path, bodyStr);

    const res = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers: {
        Authorization: token,
        Market: this.market,
        "Request-ID": requestId,
        "Content-Type": "application/json",
      },
      body: body ? bodyStr : undefined,
      signal: AbortSignal.timeout(8000),
    });

    if (!res.ok) {
      const text = await res.text();
      throw new AppError("DISPATCH_FAILED", `Lalamove API lỗi (${res.status})`, {
        providerMessage: text.slice(0, 300),
      });
    }
    if (res.status === 204) return {} as T;
    return (await res.json()) as T;
  }

  async quote(pickup: GeoPoint, dropoff: GeoPoint): Promise<DeliveryQuote> {
    const payload = {
      data: {
        serviceType: "MOTORCYCLE",
        language: "vi_VN",
        stops: [
          { coordinates: { lat: String(pickup.lat), lng: String(pickup.lng) }, address: pickup.address },
          { coordinates: { lat: String(dropoff.lat), lng: String(dropoff.lng) }, address: dropoff.address },
        ],
      },
    };

    const json = await this.request<{
      data: {
        quotationId: string;
        expiresAt: string;
        priceBreakdown: { total: string };
        distance?: { value: string };
        stops: Array<{ stopId: string }>;
      };
    }>("POST", "/v3/quotations", payload);

    const d = json.data;
    const distanceKm = d.distance?.value
      ? Math.round((Number(d.distance.value) / 1000) * 10) / 10
      : haversineKm(pickup.lat, pickup.lng, dropoff.lat, dropoff.lng);

    return {
      quotationId: d.quotationId,
      provider: "LALAMOVE",
      distanceKm,
      shippingFee: Math.round(Number(d.priceBreakdown.total)),
      expiresAt: d.expiresAt,
      pickupStopId: d.stops[0]?.stopId,
      dropoffStopId: d.stops[1]?.stopId,
    };
  }

  async createOrder(req: CreateDeliveryRequest): Promise<ExternalDeliveryOrder> {
    const remarks = [
      `Đơn MiaoSong #${req.orderCode}`,
      req.codAmount > 0 ? `THU HỘ COD: ${req.codAmount.toLocaleString("vi-VN")}đ` : "ĐÃ THANH TOÁN ONLINE (Thu 0đ)",
      req.deliveryNote ?? "",
    ]
      .filter(Boolean)
      .join(" | ");

    const payload = {
      data: {
        quotationId: req.quotationId,
        sender: {
          stopId: req.pickupStopId,
          name: req.pickup.name ?? "Quán MiaoSong",
          phone: req.pickup.phone ?? "+84900000000",
        },
        recipients: [
          {
            stopId: req.dropoffStopId,
            name: req.dropoff.name ?? "Khách MiaoSong",
            phone: req.dropoff.phone ?? "+84900000001",
            remarks,
          },
        ],
        metadata: { orderId: req.orderId, orderCode: req.orderCode },
      },
    };

    const json = await this.request<{ data: { orderId: string; shareLink?: string } }>(
      "POST",
      "/v3/orders",
      payload,
    );
    return {
      provider: "LALAMOVE",
      externalOrderId: json.data.orderId,
      shareLink: json.data.shareLink,
    };
  }

  async getDriver(externalOrderId: string, driverId: string): Promise<DriverInfo> {
    const json = await this.request<{
      data: { name: string; phone: string; plateNumber: string };
    }>("GET", `/v3/orders/${externalOrderId}/drivers/${driverId}`);
    return {
      name: json.data.name,
      phone: json.data.phone,
      plate: json.data.plateNumber,
    };
  }

  async cancel(externalOrderId: string): Promise<void> {
    await this.request("DELETE", `/v3/orders/${externalOrderId}`);
  }
}
