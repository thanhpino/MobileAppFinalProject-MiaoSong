# 06 — State Machine, Data Model & Class Diagrams

> Phụ trách: **Thành** · Review: **Phú**

## 1. State machine đơn hàng

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT: đặt + VNPay
    [*] --> PLACED: đặt + COD
    PENDING_PAYMENT --> PLACED: thanh toán thành công
    PENDING_PAYMENT --> CANCELLED: quá 15 phút / khách hủy
    PLACED --> CONFIRMED: quán xác nhận
    PLACED --> REJECTED: quán từ chối
    PLACED --> CANCELLED: khách hủy
    CONFIRMED --> READY: quán đóng gói xong
    CONFIRMED --> REJECTED: hết nguyên liệu
    READY --> FINDING_DRIVER: gọi shipper
    FINDING_DRIVER --> DRIVER_ASSIGNED: Lalamove gán tài xế
    FINDING_DRIVER --> READY: không có tài xế / hủy chuyến
    DRIVER_ASSIGNED --> ARRIVED_AT_STORE
    DRIVER_ASSIGNED --> READY: tài xế hủy
    ARRIVED_AT_STORE --> PICKED_UP
    PICKED_UP --> DELIVERING
    DELIVERING --> COMPLETED
    DELIVERING --> DELIVERY_FAILED
    REJECTED --> [*]
    CANCELLED --> [*]
    COMPLETED --> [*]
    DELIVERY_FAILED --> [*]
```

**Bảng chuyển trạng thái (nguồn cho `OrderStateMachine`):**

| Từ | Sang | Ai kích hoạt |
|---|---|---|
| PENDING_PAYMENT | PLACED / CANCELLED | payment / khách, scheduler |
| PLACED | CONFIRMED / REJECTED / CANCELLED | quán / quán / khách |
| CONFIRMED | READY / REJECTED | quán (chế độ Bếp) / quán |
| READY | FINDING_DRIVER | quán |
| FINDING_DRIVER | DRIVER_ASSIGNED / READY | Lalamove |
| DRIVER_ASSIGNED | ARRIVED_AT_STORE / READY | Lalamove |
| ARRIVED_AT_STORE | PICKED_UP | Lalamove |
| PICKED_UP | DELIVERING | Lalamove |
| DELIVERING | COMPLETED / DELIVERY_FAILED | Lalamove |

## 2. State machine thanh toán & hoàn tiền

```mermaid
stateDiagram-v2
    state "payment.status" as P {
        [*] --> UNPAID
        UNPAID --> PAID: VNPay IPN OK
        UNPAID --> FAILED: thanh toán lỗi
        FAILED --> UNPAID: thanh toán lại
        PAID --> [*]
    }
    state "refundStatus" as R {
        [*] --> NONE
        NONE --> PENDING: đơn REJECTED/CANCELLED và đã PAID
        PENDING --> REFUNDED
        PENDING --> REFUND_FAILED
        REFUND_FAILED --> PENDING: thử lại thủ công
    }
```

## 3. State machine trạng thái quán

```mermaid
stateDiagram-v2
    [*] --> OPEN
    OPEN --> BUSY: chọn bận 15'/30'/1h/1 ngày
    BUSY --> OPEN: hết busyUntil / mở lại thủ công
    OPEN --> CLOSED: đóng thủ công / ngoài lịch
    BUSY --> CLOSED: đóng thủ công
    CLOSED --> OPEN: mở thủ công / tới giờ lịch
```

## 4. ERD (mô hình logic)

```mermaid
erDiagram
    USER ||--o{ ADDRESS : "có"
    USER ||--o{ ORDER : "đặt"
    USER |o--o| STORE : "vận hành (MERCHANT)"
    STORE ||--o{ CATEGORY : "có"
    CATEGORY ||--o{ MENU_ITEM : "chứa"
    MENU_ITEM ||--o{ OPTION_GROUP : "có"
    OPTION_GROUP ||--|{ OPTION : "gồm"
    STORE ||--o{ VOUCHER : "phát hành"
    VOUCHER ||--o{ VOUCHER_REDEMPTION : "được dùng"
    STORE ||--o{ ORDER : "nhận"
    ORDER ||--|{ ORDER_ITEM : "gồm"
    ORDER ||--|| PAYMENT : "có"
    ORDER ||--o| DELIVERY : "có"
    ORDER ||--o| REVIEW : "được đánh giá"

    USER {
        string uid PK
        string name
        string phone
        string role
        string storeId FK
    }
    ADDRESS {
        string id PK
        string fullText
        double lat
        double lng
        bool isDefault
    }
    STORE {
        string id PK
        string name
        string slug
        string manualStatus
        timestamp busyUntil
        int maxDistanceKm
        double ratingAvg
    }
    CATEGORY {
        string id PK
        string name
        int sortOrder
    }
    MENU_ITEM {
        string id PK
        string name
        long basePrice
        bool isAvailable
        string imageUrl
    }
    OPTION_GROUP {
        string id PK
        string name
        string type
        bool required
        int min
        int max
    }
    OPTION {
        string id PK
        string name
        long priceDelta
        bool isAvailable
    }
    VOUCHER {
        string id PK
        string code
        string type
        long value
        long maxDiscount
        long minOrder
        timestamp startAt
        timestamp endAt
        int usageLimit
        int usedCount
        int perUserLimit
    }
    VOUCHER_REDEMPTION {
        string id PK
        string uid
        string orderId
    }
    ORDER {
        string id PK
        string code
        string status
        long subtotal
        long discount
        long shippingFee
        long total
        string idempotencyKey
        timestamp createdAt
    }
    ORDER_ITEM {
        string lineKey
        string itemName
        string sizeName
        string toppings
        string note
        long unitPrice
        int quantity
    }
    PAYMENT {
        string method
        string status
        long amountVnd
        string gatewayTxnId
        string refundStatus
    }
    DELIVERY {
        string provider
        string quotationId
        string externalId
        string driverName
        string driverPhone
        string plate
        timestamp etaAt
    }
    REVIEW {
        string orderId PK
        int stars
        string comment
    }
```

## 5. Ánh xạ Firestore (mô hình vật lý)

| Path | Ghi bởi | Ghi chú |
|---|---|---|
| `users/{uid}` | client (hồ sơ), CF (role) | |
| `users/{uid}/addresses/{id}` | client | |
| `stores/{storeId}` | MERCHANT (Rules) | `weeklySchedule`, `specialDays` nhúng trong doc |
| `stores/{storeId}/categories/{id}` | MERCHANT | |
| `stores/{storeId}/menuItems/{id}` | MERCHANT | `optionGroups[]` **nhúng** (đọc 1 lần là đủ) |
| `stores/{storeId}/vouchers/{id}` | MERCHANT, trừ `usedCount` (chỉ CF) | |
| `voucherRedemptions/{voucherId_uid_orderId}` | CF | đếm perUserLimit |
| `orders/{orderId}` | **chỉ CF** | `items[]`, `payment`, `delivery`, `statusHistory[]` nhúng |
| `reviews/{orderId}` | chỉ CF | id = orderId → đảm bảo 1 đơn 1 đánh giá |
| `processedEvents/{source_eventId}` | chỉ CF | idempotency |
| `config/payment` | admin | `pendingPaymentTtlMin` |

**Index cần tạo:** `orders(storeId, status, createdAt desc)` · `orders(customerId, createdAt desc)` · `orders(status, createdAt)` cho scheduler.

## 6. Class diagram — Strategy cho Delivery & Payment (backend)

```mermaid
classDiagram
    class DeliveryProvider {
        <<interface>>
        +quote(pickup, dropoff) Quote
        +createOrder(order, quote) ExternalOrder
        +getDriver(externalId, driverId) Driver
        +cancel(externalId) void
        +parseWebhook(req) DeliveryEvent
    }
    class LalamoveAdapter {
        -apiKey
        -secret
        -sign(method, path, body) string
    }
    class MockAdapter {
        +advance(orderId) void
    }
    class DeliveryProviderFactory {
        +of(type) DeliveryProvider
    }
    DeliveryProvider <|.. LalamoveAdapter
    DeliveryProvider <|.. MockAdapter
    DeliveryProviderFactory ..> DeliveryProvider

    class PaymentGateway {
        <<interface>>
        +createPayment(order) PaymentInit
        +confirm(payload) PaymentResult
        +refund(payment) RefundResult
    }
    class CodGateway
    class VnpayGateway {
        -tmnCode
        -hashSecret
        -hmacSha512(data) string
    }
    class PaymentGatewayFactory {
        +of(method) PaymentGateway
    }
    PaymentGateway <|.. CodGateway
    PaymentGateway <|.. VnpayGateway
    PaymentGatewayFactory ..> PaymentGateway
```

## 7. Class diagram — Domain (dùng chung ý tưởng cho Kotlin & TypeScript)

```mermaid
classDiagram
    class Money {
        +Long vnd
        +plus(Money) Money
        +minus(Money) Money
        +times(Int) Money
    }
    class CartLine {
        +CartLineKey key
        +String menuItemId
        +SelectedOption size
        +List~SelectedOption~ toppings
        +String note
        +Int quantity
        +unitPrice() Money
        +lineTotal() Money
    }
    class CartLineKey {
        +String value
        +of(itemId, sizeId, toppingIds, note)$ CartLineKey
    }
    class PricingCalculator {
        +calculate(lines, voucher, shippingFee) PriceBreakdown
    }
    class PriceBreakdown {
        +Money itemsTotal
        +Money toppingsTotal
        +Money subtotal
        +Money discount
        +Money shippingFee
        +Money total
    }
    class Voucher {
        +VoucherType type
        +Long value
        +Long maxDiscount
        +Long minOrder
        +discountFor(subtotal) Money
    }
    class VoucherSpecification {
        <<interface>>
        +isSatisfiedBy(ctx) Boolean
    }
    class MinOrderSpec
    class DateRangeSpec
    class UsageLimitSpec
    class PerUserLimitSpec
    class OrderStateMachine {
        +canTransition(from, to) Boolean
        +transition(order, to) Order
    }
    CartLine --> CartLineKey
    CartLine --> Money
    PricingCalculator ..> CartLine
    PricingCalculator ..> Voucher
    PricingCalculator --> PriceBreakdown
    VoucherSpecification <|.. MinOrderSpec
    VoucherSpecification <|.. DateRangeSpec
    VoucherSpecification <|.. UsageLimitSpec
    VoucherSpecification <|.. PerUserLimitSpec
```

## 8. Class diagram — Android (một feature tiêu biểu: Cart)

```mermaid
classDiagram
    class CartScreen {
        <<Composable>>
    }
    class CartViewModel {
        +StateFlow~CartUiState~ uiState
        +onIncrease(key)
        +onDecrease(key)
        +onRemove(key)
        +onCheckout()
    }
    class CartUiState {
        +List~CartLineUi~ lines
        +PriceBreakdown price
        +Boolean hasUnavailable
    }
    class ObserveCartUseCase
    class UpdateQuantityUseCase
    class CartRepository {
        <<interface>>
        +observe() Flow~List~CartLine~~
        +upsert(line)
        +updateQuantity(key, qty)
        +clear()
    }
    class CartRepositoryImpl
    class CartDao {
        <<Room DAO>>
    }
    CartScreen --> CartViewModel
    CartViewModel --> CartUiState
    CartViewModel --> ObserveCartUseCase
    CartViewModel --> UpdateQuantityUseCase
    ObserveCartUseCase --> CartRepository
    UpdateQuantityUseCase --> CartRepository
    CartRepository <|.. CartRepositoryImpl
    CartRepositoryImpl --> CartDao
```

