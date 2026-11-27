# 08 — API Contract, Webhooks, Security Rules & Test Vectors

> Người phụ trách: **Trương Minh Thành (524H0032)** · Nhánh: `thanhpino`
> Liên quan: [01-SRS.md](01-SRS.md) · [02-architecture.md](02-architecture.md) · [04-sequence-diagrams.md](04-sequence-diagrams.md) · [06-state-data-class.md](06-state-data-class.md)

## Mục lục
1. [Quy ước chung cho Cloud Functions Callables](#1-quy-ước-chung-cho-cloud-functions-callables)
2. [Bảng mã lỗi chuẩn (Standard Error Codes)](#2-bảng-mã-lỗi-chuẩn-standard-error-codes)
3. [Đặc tả 10 Cloud Functions Callables nghiệp vụ chính](#3-đặc-tả-10-cloud-functions-callables)
4. [Đặc tả Webhooks, Mock Simulator & Background Triggers (Tổng 15 Cloud Functions)](#4-đặc-tả-webhooks-mock-simulator--background-triggers-tổng-15-cloud-functions)
5. [Hợp đồng gói tin FCM Push Notification](#5-hợp-đồng-gói-tin-fcm-push-notification)
6. [Thiết kế Firestore Security Rules](#6-thiết-kế-firestore-security-rules)
7. [Thuật toán chuẩn hóa `CartLineKey` (Kotlin & TypeScript)](#7-thuật-toán-chuẩn-hóa-cartlinekey-kotlin--typescript)

---

## 1. Quy ước chung cho Cloud Functions Callables

- **Region:** `asia-southeast1` (Singapore — độ trễ thấp nhất về Việt Nam).
- **Xác thực:** Mọi callable yêu cầu Firebase Auth ID Token hợp lệ (`context.auth != null`). Các hàm của quán kiểm tra thêm Custom Claims: `context.auth.token.role == 'MERCHANT'` và `context.auth.token.storeId == storeId`.
- **Định dạng tiền tệ:** Toàn bộ số tiền VND là **số nguyên (`Long` trên Kotlin, `number` integer trên TypeScript)**, đơn vị `đồng` (ví dụ: `45000`).
- **Định dạng thời gian:** ISO 8601 UTC string (`"2026-11-20T09:15:00.000Z"`) trong payload JSON; `Timestamp` khi lưu Firestore.
- **Cấu trúc phản hồi chuẩn (Envelope):**
  ```json
  // Thành công
  {
    "ok": true,
    "data": { ... }
  }

  // Thất bại (hoặc throw HttpsError với details)
  {
    "ok": false,
    "error": {
      "code": "VOUCHER_MIN_ORDER",
      "message": "Đơn tối thiểu 100.000đ, thêm 12.000đ nữa để dùng mã nhé.",
      "details": { "minOrder": 100000, "currentSubtotal": 88000, "shortfall": 12000 }
    }
  }
  ```

---

## 2. Bảng mã lỗi chuẩn (Standard Error Codes)

| Mã lỗi (`error.code`) | HTTP / Firebase Code | Khi nào xảy ra | Dữ liệu `details` trả kèm cho App hiển thị |
|---|---|---|---|
| `UNAUTHENTICATED` | `unauthenticated` | Chưa đăng nhập hoặc token hết hạn | — |
| `PERMISSION_DENIED` | `permission-denied` | Không đúng role (`CUSTOMER` / `MERCHANT`) hoặc khác `storeId` | `{ requiredRole }` |
| `STORE_CLOSED` | `failed-precondition` | Quán đang `CLOSED` hoặc `BUSY` tại thời điểm đặt đơn | `{ effectiveStatus, reopenAt }` |
| `ITEM_UNAVAILABLE` | `failed-precondition` | Món ăn hoặc tùy chọn đã bị quán tắt (`isAvailable = false`) | `{ unavailableItemIds: [], unavailableOptionIds: [] }` |
| `INVALID_OPTIONS` | `invalid-argument` | Thiếu nhóm tùy chọn `SINGLE` bắt buộc hoặc số lượng chọn vượt `[min, max]` | `{ groupId, groupName, reason }` |
| `PRICE_CHANGED` | `aborted` | Giá món/topping trên menu vừa thay đổi so với giá snapshot client gửi lên | `{ oldSubtotal, newSubtotal, updatedItems: [] }` |
| `OUT_OF_RANGE` | `failed-precondition` | Khoảng cách giao hàng vượt quá `store.maxDistanceKm` | `{ distanceKm, maxDistanceKm }` |
| `QUOTE_EXPIRED` | `failed-precondition` | `quotationId` đã hết hạn (> 5 phút) hoặc phí ship thay đổi khi đặt | `{ newFee, newQuotationId, expiresAt }` |
| `QUOTE_UNAVAILABLE` | `unavailable` | Lalamove API lỗi hoặc timeout > 8s | `{ provider: "LALAMOVE" }` |
| `VOUCHER_NOT_FOUND` | `not-found` | Mã giảm giá không tồn tại hoặc `isActive = false` | `{ code }` |
| `VOUCHER_EXPIRED` | `failed-precondition` | Chưa tới `startAt` hoặc đã qua `endAt` | `{ startAt, endAt }` |
| `VOUCHER_MIN_ORDER` | `failed-precondition` | `subtotal < voucher.minOrder` | `{ minOrder, currentSubtotal, shortfall }` |
| `VOUCHER_EXHAUSTED` | `resource-exhausted` | `usedCount >= usageLimit` (hết tổng số lượt dùng) | `{ usageLimit }` |
| `VOUCHER_USER_LIMIT` | `resource-exhausted` | Khách đã dùng đủ `perUserLimit` lần cho mã này | `{ perUserLimit }` |
| `INVALID_TRANSITION` | `failed-precondition` | Chuyển trạng thái đơn vi phạm `OrderStateMachine` | `{ currentStatus, targetStatus }` |
| `DISPATCH_FAILED` | `unavailable` | Lalamove từ chối tạo chuyến khi quán bấm "Gọi Shipper" | `{ providerMessage }` |
| `REFUND_FAILED` | `internal` | Gọi API hoàn tiền ZaloPay thất bại | `{ gateway, gatewayCode }` |

---

## 3. Đặc tả 10 Cloud Functions Callables

### 3.1 `getDeliveryQuote`
- **Module:** `delivery` · **Người gọi:** `CUSTOMER` · **Use case:** `UC08`
- **Request:**
  ```json
  {
    "storeId": "store_bun_rieu_co_ba",
    "dropoff": {
      "address": "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
      "lat": 10.7326,
      "lng": 106.6998
    }
  }
  ```
- **Response (`data`):**
  ```json
  {
    "quotationId": "qt_lalamove_998172",
    "provider": "LALAMOVE",
    "distanceKm": 3.2,
    "shippingFee": 18000,
    "expiresAt": "2026-11-20T09:20:00.000Z"
  }
  ```

### 3.2 `validateVoucher`
- **Module:** `pricing` · **Người gọi:** `CUSTOMER` · **Use case:** `UC07`
- **Request:**
  ```json
  {
    "storeId": "store_bun_rieu_co_ba",
    "code": "MIAO10",
    "subtotal": 131000
  }
  ```
- **Response (`data`):**
  ```json
  {
    "voucherId": "v_miao10",
    "code": "MIAO10",
    "type": "PERCENT",
    "value": 10,
    "maxDiscount": 20000,
    "discount": 13100
  }
  ```

### 3.3 `placeOrder`
- **Module:** `orders` · **Người gọi:** `CUSTOMER` · **Use case:** `UC09`
- **Request:**
  ```json
  {
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
    "storeId": "store_bun_rieu_co_ba",
    "address": {
      "label": "Trường ĐH Tôn Đức Thắng",
      "fullText": "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
      "lat": 10.7326,
      "lng": 106.6998,
      "recipientName": "Huỳnh Trần Thiên Phú",
      "recipientPhone": "0901234567",
      "deliveryNote": "Gọi khi tới cổng chính"
    },
    "quotationId": "qt_lalamove_998172",
    "voucherCode": "MIAO10",
    "paymentMethod": "ZALOPAY",
    "expectedPricing": {
      "subtotal": 131000,
      "discount": 13100,
      "shippingFee": 18000,
      "total": 135900
    },
    "items": [
      {
        "lineKey": "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3",
        "menuItemId": "item_bun_rieu_dac_biet",
        "sizeOptionId": "opt_size_large",
        "toppingOptionIds": ["opt_top_cha_ca"],
        "note": "không hành",
        "quantity": 1,
        "clientUnitPrice": 63000
      },
      {
        "lineKey": "c3499c2729730a7f807efb8676a92dcb6f8a3f8f",
        "menuItemId": "item_bun_rieu_dac_biet",
        "sizeOptionId": "opt_size_large",
        "toppingOptionIds": ["opt_top_cha_ca"],
        "note": "nhiều hành",
        "quantity": 1,
        "clientUnitPrice": 63000
      },
      {
        "lineKey": "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15",
        "menuItemId": "item_tra_da",
        "sizeOptionId": null,
        "toppingOptionIds": [],
        "note": "",
        "quantity": 1,
        "clientUnitPrice": 5000
      }
    ]
  }
  ```
- **Response (`data`):**
  ```json
  {
    "orderId": "ord_20261120_0012",
    "orderCode": "MS-261120-0012",
    "status": "PENDING_PAYMENT",
    "pricing": {
      "itemsTotal": 115000,
      "toppingsTotal": 16000,
      "subtotal": 131000,
      "discount": 13100,
      "shippingFee": 18000,
      "total": 135900
    },
    "payment": {
      "method": "ZALOPAY",
      "amountVnd": 135900,
      "paymentUrl": "https://sb-openapi.zalopay.vn/v2/gateway/pay?order=..."
    }
  }
  ```
  *(Nếu `paymentMethod == "COD"`, `status` trả về `"PLACED"` ngay lập tức và `paymentUrl = null`)*.

### 3.4 `confirmPayment`
- **Module:** `payment` · **Người gọi:** `CUSTOMER` (tự động gọi khi `ON_RESUME` quay về app từ ZaloPay Custom Tab) · **Use case:** `UC09`, `UC41`
- **Request:** `{ "orderId": "ord_20261120_0012", "appTransId": "261120_MS2611200012" }`
- **Response (`data`):** `{ "orderId": "ord_20261120_0012", "paymentStatus": "PAID" }`

### 3.5 `cancelOrder`
- **Module:** `orders` + `payment` · **Người gọi:** `CUSTOMER` · **Use case:** `UC15`
- **Điều kiện:** Chỉ hủy được khi `status in ['PENDING_PAYMENT', 'PLACED']` (quán chưa xác nhận). Tự động hoàn trả lượt Voucher và gọi `ZaloPayGateway.refund()` nếu đơn `PLACED` đã thanh toán ZaloPay (`payment.status == 'PAID'`).
- **Request:** `{ "orderId": "ord_20261120_0012", "reason": "Khách hàng hủy đơn trước khi quán xác nhận" }`
- **Response (`data`):** `{ "orderId": "ord_20261120_0012", "status": "CANCELLED", "refundStatus": "REFUNDED" }`

### 3.6 `confirmOrder`
- **Module:** `orders` · **Người gọi:** `MERCHANT` · **Use case:** `UC26`
- **Request:** `{ "orderId": "ord_20261120_0012" }`
- **Response (`data`):** `{ "orderId": "ord_20261120_0012", "status": "CONFIRMED", "confirmedAt": "2026-11-20T09:16:10.000Z" }`

### 3.7 `rejectOrder`
- **Module:** `orders` + `payment` · **Người gọi:** `MERCHANT` · **Use case:** `UC27`
- **Điều kiện:** `status in ['PLACED', 'CONFIRMED']` (chưa gọi shipper).
- **Request:**
  ```json
  {
    "orderId": "ord_20261120_0012",
    "reasonCode": "OUT_OF_STOCK",
    "reasonText": "Hết chả cá",
    "disableMenuItemIds": ["item_bun_cha_ca"]
  }
  ```
- **Response (`data`):**
  ```json
  {
    "orderId": "ord_20261120_0012",
    "status": "REJECTED",
    "refundStatus": "REFUNDED",
    "voucherReleased": true
  }
  ```

### 3.8 `markReady`
- **Module:** `orders` · **Người gọi:** `MERCHANT` · **Use case:** `UC29`
- **Request:** `{ "orderId": "ord_20261120_0012" }`
- **Response (`data`):** `{ "orderId": "ord_20261120_0012", "status": "READY", "readyAt": "2026-11-20T09:24:00.000Z" }`

### 3.9 `dispatchDriver`
- **Module:** `orders` + `delivery` · **Người gọi:** `MERCHANT` · **Use case:** `UC30`
- **Request:** `{ "orderId": "ord_20261120_0012" }`
- **Response (`data`):**
  ```json
  {
    "orderId": "ord_20261120_0012",
    "status": "FINDING_DRIVER",
    "provider": "LALAMOVE",
    "externalOrderId": "1092837465",
    "shareLink": "https://share.sandbox.lalamove.com/...",
    "codAmountToCollect": 0
  }
  ```

### 3.10 `submitReview`
- **Module:** `review` · **Người gọi:** `CUSTOMER` · **Use case:** `UC12`
- **Request:**
  ```json
  {
    "orderId": "ord_20261120_0012",
    "stars": 5,
    "tags": ["Nóng hổi", "Đóng gói kỹ"],
    "comment": "Nước lèo đậm đà, giao nhanh!",
    "imageUrl": "https://firebasestorage.googleapis.com/v0/b/miaosong-pro.firebasestorage.app/..."
  }
  ```
- **Response (`data`):** `{ "reviewId": "ord_20261120_0012", "newStoreRatingAvg": 4.85, "newStoreRatingCount": 42 }`

---

## 4. Đặc tả Webhooks, Mock Simulator & Background Triggers (Tổng 15 Cloud Functions)

| Function Name | Loại | Cơ chế xác thực & Xử lý Idempotency |
|---|---|---|
| `lalamoveWebhook` | `onRequest` (`POST`) | Kiểm tra HMAC-SHA256 trong header `Authorization` của Lalamove; lưu `processedEvents/LALAMOVE_{eventId}` |
| `zalopayCallback` | `onRequest` (`POST`) | Xác minh MAC HMAC-SHA256 với `key2` của ZaloPay (`app_id = 2554`); lưu `processedEvents/ZALOPAY_{zp_trans_id}` |
| `mockDeliveryAdvance` | `onCall` | Yêu cầu quyền `MERCHANT`; đẩy từng chặng `DRIVER_ASSIGNED → ARRIVED_AT_STORE → PICKED_UP → DELIVERING → COMPLETED` khi demo |
| `expirePendingOrders` | `onSchedule` (5 phút) | Tự hủy đơn `PENDING_PAYMENT` quá 15 phút & hoàn trả lượt Voucher trong Transaction |
| `onOrderUpdated` | `onDocumentWritten` | Trigger `orders/{orderId}` gửi Push FCM tới Quán (chuông đơn mới, hủy, lỗi hoàn tiền/giao hàng) & Khách (tiến độ, từ chối, hoàn tiền) |

**Payload mẫu của callable `mockDeliveryAdvance` (dùng khi demo nếu sandbox Lalamove không có tài xế nhận):**
```json
{
  "orderId": "ord_20261120_0012",
  "targetStatus": "DRIVER_ASSIGNED",
  "mockDriver": {
    "name": "Nguyễn Văn Minh",
    "phone": "0909888777",
    "plate": "59X1-123.45"
  }
}
```

---

## 5. Hợp đồng gói tin FCM Push Notification

Mọi push gửi từ module `notification` đều chứa cả `notification` (để hiển thị khi app tắt) và `data` (để Android routing đúng màn hình và phát chuông riêng).

```json
{
  "token": "<fcm_device_token>",
  "android": {
    "priority": "HIGH",
    "notification": {
      "channelId": "miaosong_new_order",
      "sound": "meow_bell.mp3"
    }
  },
  "notification": {
    "title": "🔔 Đơn mới #MS-261120-0012",
    "body": "2 món · 135.900đ (Đã thanh toán ZaloPay)"
  },
  "data": {
    "type": "NEW_ORDER",
    "orderId": "ord_20261120_0012",
    "orderCode": "MS-261120-0012",
    "status": "PLACED",
    "targetRole": "MERCHANT",
    "deepLink": "miaosong://merchant/orders/ord_20261120_0012"
  }
}
```

**Danh sách `channelId` trên Android:**
1. `miaosong_new_order` (Importance: `HIGH`, âm thanh chuông lặp `meow_bell.mp3`, rung mạnh — dùng cho Quán khi có đơn `PLACED`).
2. `miaosong_order_status` (Importance: `HIGH`, có rung — dùng cho Khách hàng mỗi khi trạng thái đơn hoặc tài xế thay đổi).
3. `miaosong_late_alert` (Importance: `HIGH` — cảnh báo Quán khi đơn giao trễ quá `etaAt + 15 phút`).

---

## 6. Thiết kế Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }
    function isMerchantOfStore(storeId) {
      return isSignedIn()
        && request.auth.token.role == 'MERCHANT'
        && request.auth.token.storeId == storeId;
    }

    // 1. Hồ sơ người dùng & địa chỉ
    match /users/{uid} {
      allow read: if isSignedIn() && request.auth.uid == uid;
      // Client không được tự sửa trường role và storeId
      allow create, update: if isSignedIn() && request.auth.uid == uid
        && (!request.resource.data.diff(resource.data).affectedKeys().hasAny(['role', 'storeId']));

      match /addresses/{addrId} {
        allow read, write: if isSignedIn() && request.auth.uid == uid;
      }
    }

    // 2. Quán ăn, nhóm món, thực đơn, voucher
    match /stores/{storeId} {
      allow read: if true;
      allow update: if isMerchantOfStore(storeId)
        && (!request.resource.data.diff(resource.data).affectedKeys().hasAny(['ratingAvg', 'ratingCount']));

      match /categories/{catId} {
        allow read: if true;
        allow write: if isMerchantOfStore(storeId);
      }

      match /menuItems/{itemId} {
        allow read: if true;
        allow write: if isMerchantOfStore(storeId);
      }

      match /vouchers/{voucherId} {
        allow read: if isSignedIn();
        // Quán không được sửa trực tiếp usedCount (do Cloud Functions quản lý trong transaction)
        allow create: if isMerchantOfStore(storeId) && request.resource.data.usedCount == 0;
        allow update: if isMerchantOfStore(storeId)
          && request.resource.data.usedCount == resource.data.usedCount;
        allow delete: if isMerchantOfStore(storeId);
      }
    }

    // 3. Đơn hàng — CHỈ Cloud Functions (Admin SDK) được ghi
    match /orders/{orderId} {
      allow read: if isSignedIn() && (
        resource.data.customerId == request.auth.uid ||
        isMerchantOfStore(resource.data.storeId)
      );
      allow write: if false;
    }

    // 4. Đánh giá — đọc công khai, chỉ Cloud Functions được ghi
    match /reviews/{orderId} {
      allow read: if true;
      allow write: if false;
    }

    // 5. Các bảng nội bộ của Backend
    match /voucherRedemptions/{id} { allow read, write: if false; }
    match /processedEvents/{id} { allow read, write: if false; }
    match /config/{id} { allow read: if true; allow write: if false; }
  }
}
```

---

## 7. Thuật toán chuẩn hóa `CartLineKey` (Kotlin & TypeScript)

Để đảm bảo yêu cầu đề bài: *"Đặt 2 tô bún, 1 tô 'không hành' và 1 tô 'nhiều hành' phải tách thành 2 dòng riêng biệt"*, cả Android và Backend dùng chung công thức tạo khóa dòng giỏ hàng:

1. `normalizedNote = note.trim().replace(Regex("\\s+"), " ").lowercase()`
2. `sortedToppings = toppingOptionIds.sorted().joinToString(",")`
3. `raw = "$menuItemId|${sizeOptionId ?: ""}|$sortedToppings|$normalizedNote"`
4. `cartLineKey = sha1Hex(raw)`

File dữ liệu test chuẩn dùng chung cho cả JUnit (Android) và Vitest/Jest (Cloud Functions) được đặt tại: [`shared/test-vectors/pricing.json`](../../shared/test-vectors/pricing.json).
