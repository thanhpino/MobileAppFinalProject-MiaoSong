import { setGlobalOptions } from "firebase-functions/v2";

// Toàn bộ Cloud Functions chạy ở Singapore (asia-southeast1)
setGlobalOptions({ region: "asia-southeast1", maxInstances: 10 });

// Export toàn bộ Public Endpoints từ 7 module Modular Monolith
export { validateVoucher } from "./modules/pricing";
export {
  getDeliveryQuote,
  lalamoveWebhook,
  mockDeliveryAdvance,
} from "./modules/delivery";
export { zalopayCallback, confirmPayment } from "./modules/payment";
export {
  placeOrder,
  confirmOrder,
  rejectOrder,
  markReady,
  dispatchDriver,
  cancelOrder,
  expirePendingOrders,
} from "./modules/orders";
export { onOrderUpdated } from "./modules/notification";
export { submitReview } from "./modules/review";
