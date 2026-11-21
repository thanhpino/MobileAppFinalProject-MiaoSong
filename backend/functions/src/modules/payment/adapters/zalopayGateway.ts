import { createHmac } from "crypto";
import { AppError } from "../../../shared/errors";
import { CreatePaymentInput, PaymentInitResult, RefundResult } from "../domain/paymentTypes";
import { PaymentGateway } from "../ports/paymentGateway";

/**
 * ZaloPay API v2 Sandbox Gateway (create / query / refund + HMAC-SHA256 verification).
 * Tài liệu chính thức: https://docs.zalopay.vn/v2/general/overview.html
 */
export class ZaloPayGateway implements PaymentGateway {
  readonly method = "ZALOPAY" as const;

  private get appId(): number {
    return Number(process.env.ZALOPAY_APP_ID ?? 2553);
  }
  private get key1(): string {
    return process.env.ZALOPAY_KEY1 ?? "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
  }
  private get key2(): string {
    return process.env.ZALOPAY_KEY2 ?? "kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz";
  }
  private get createUrl(): string {
    return process.env.ZALOPAY_CREATE_URL ?? "https://sb-openapi.zalopay.vn/v2/create";
  }
  private get queryUrl(): string {
    return process.env.ZALOPAY_QUERY_URL ?? "https://sb-openapi.zalopay.vn/v2/query";
  }
  private get refundUrl(): string {
    return process.env.ZALOPAY_REFUND_URL ?? "https://sb-openapi.zalopay.vn/v2/refund";
  }
  private get callbackUrl(): string {
    return process.env.ZALOPAY_CALLBACK_URL ?? "";
  }
  private get redirectUrl(): string {
    return process.env.ZALOPAY_REDIRECT_URL ?? "https://miaosong-pro.web.app/pay/return";
  }

  private hmac(key: string, data: string): string {
    return createHmac("sha256", key).update(data).digest("hex");
  }

  private buildAppTransId(orderCode: string): string {
    const vn = new Date(Date.now() + 7 * 60 * 60 * 1000);
    const yy = String(vn.getUTCFullYear()).slice(-2);
    const mm = String(vn.getUTCMonth() + 1).padStart(2, "0");
    const dd = String(vn.getUTCDate()).padStart(2, "0");
    const suffix = orderCode.replace(/[^A-Za-z0-9]/g, "").slice(-10) + String(Date.now()).slice(-4);
    return `${yy}${mm}${dd}_${suffix}`;
  }

  verifyCallback(dataStr: string, reqMac: string): boolean {
    const expected = this.hmac(this.key2, dataStr);
    return expected === reqMac;
  }

  async createPayment(input: CreatePaymentInput): Promise<PaymentInitResult> {
    const appTransId = this.buildAppTransId(input.orderCode);
    const appTime = Date.now();
    const embedData = JSON.stringify({
      orderId: input.orderId,
      redirecturl: `${this.redirectUrl}?orderId=${encodeURIComponent(input.orderId)}`,
    });
    const item = JSON.stringify([]);
    const rawMac = `${this.appId}|${appTransId}|${input.userId}|${input.amountVnd}|${appTime}|${embedData}|${item}`;
    const mac = this.hmac(this.key1, rawMac);

    const body = new URLSearchParams({
      app_id: String(this.appId),
      app_user: input.userId,
      app_time: String(appTime),
      amount: String(input.amountVnd),
      app_trans_id: appTransId,
      embed_data: embedData,
      item,
      description: input.description,
      bank_code: "",
      callback_url: this.callbackUrl,
      mac,
    });

    const res = await fetch(this.createUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
      signal: AbortSignal.timeout(8000),
    });
    const json = (await res.json()) as {
      return_code: number;
      return_message: string;
      order_url?: string;
    };

    if (json.return_code !== 1 || !json.order_url) {
      throw new AppError("REFUND_FAILED", `ZaloPay tạo giao dịch thất bại: ${json.return_message}`, {
        gateway: "ZALOPAY",
        gatewayCode: json.return_code,
      });
    }

    return {
      method: "ZALOPAY",
      status: "UNPAID",
      appTransId,
      paymentUrl: json.order_url,
    };
  }

  async queryStatus(appTransId: string): Promise<{ paid: boolean; zpTransId?: string }> {
    const mac = this.hmac(this.key1, `${this.appId}|${appTransId}|${this.key1}`);
    const body = new URLSearchParams({
      app_id: String(this.appId),
      app_trans_id: appTransId,
      mac,
    });
    const res = await fetch(this.queryUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
      signal: AbortSignal.timeout(8000),
    });
    const json = (await res.json()) as { return_code: number; zp_trans_id?: number | string };
    return {
      paid: json.return_code === 1,
      zpTransId: json.zp_trans_id ? String(json.zp_trans_id) : undefined,
    };
  }

  async refund(zpTransId: string, amountVnd: number, reason: string): Promise<RefundResult> {
    const timestamp = Date.now();
    const vn = new Date(timestamp + 7 * 60 * 60 * 1000);
    const yymmdd = `${String(vn.getUTCFullYear()).slice(-2)}${String(vn.getUTCMonth() + 1).padStart(2, "0")}${String(vn.getUTCDate()).padStart(2, "0")}`;
    const mRefundId = `${yymmdd}_${this.appId}_${String(timestamp).slice(-8)}`;
    const mac = this.hmac(
      this.key1,
      `${this.appId}|${zpTransId}|${amountVnd}|${reason}|${timestamp}`,
    );

    const body = new URLSearchParams({
      m_refund_id: mRefundId,
      app_id: String(this.appId),
      zp_trans_id: zpTransId,
      amount: String(amountVnd),
      timestamp: String(timestamp),
      description: reason,
      mac,
    });

    const res = await fetch(this.refundUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
      signal: AbortSignal.timeout(8000),
    });
    const json = (await res.json()) as { return_code: number; return_message: string; refund_id?: number };
    return {
      success: json.return_code === 1 || json.return_code === 2,
      refundId: json.refund_id ? String(json.refund_id) : mRefundId,
      message: json.return_message,
    };
  }
}
