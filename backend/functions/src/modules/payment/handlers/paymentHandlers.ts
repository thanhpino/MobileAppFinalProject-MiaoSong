import { onCall, onRequest } from "firebase-functions/v2/https";
import { requireAuth, toHttpsError } from "../../../shared/auth";
import { eventBus } from "../../../shared/eventBus";
import { claimEventOnce } from "../../../shared/idempotency";
import { PaymentGatewayFactory } from "../application/paymentGatewayFactory";

/** ZaloPay Server-to-Server Callback (IPN) */
export const zalopayCallback = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ return_code: -1, return_message: "Method Not Allowed" });
    return;
  }
  const { data: dataStr, mac } = req.body as { data?: string; mac?: string };
  if (!dataStr || !mac || !PaymentGatewayFactory.zaloPay().verifyCallback(dataStr, mac)) {
    res.status(200).json({ return_code: -1, return_message: "mac not equal" });
    return;
  }

  const parsed = JSON.parse(dataStr) as {
    app_trans_id: string;
    zp_trans_id: number | string;
    embed_data?: string;
    amount: number;
  };
  const isNew = await claimEventOnce("ZALOPAY", String(parsed.zp_trans_id));
  if (!isNew) {
    res.status(200).json({ return_code: 1, return_message: "duplicate ignored" });
    return;
  }

  const embed = parsed.embed_data ? (JSON.parse(parsed.embed_data) as { orderId?: string }) : {};
  if (embed.orderId) {
    await eventBus.publish({
      type: "PaymentSucceeded",
      occurredAt: new Date(),
      payload: {
        orderId: embed.orderId,
        zpTransId: String(parsed.zp_trans_id),
        appTransId: parsed.app_trans_id,
        amountVnd: parsed.amount,
      },
    });
  }
  res.status(200).json({ return_code: 1, return_message: "success" });
});

/** App gọi đối soát trạng thái thanh toán sau khi quay về từ Chrome Custom Tab */
export const confirmPayment = onCall(async (request) => {
  try {
    requireAuth(request.auth);
    const { orderId, appTransId } = request.data as { orderId: string; appTransId: string };
    const status = await PaymentGatewayFactory.zaloPay().queryStatus(appTransId);
    if (status.paid && status.zpTransId) {
      const isNew = await claimEventOnce("ZALOPAY", status.zpTransId);
      if (isNew) {
        await eventBus.publish({
          type: "PaymentSucceeded",
          occurredAt: new Date(),
          payload: { orderId, zpTransId: status.zpTransId, appTransId },
        });
      }
    }
    return {
      ok: true,
      data: {
        orderId,
        paymentStatus: status.paid ? "PAID" : "UNPAID",
      },
    };
  } catch (err) {
    throw toHttpsError(err);
  }
});
