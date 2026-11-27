# 06 — State Machine, Data Model & Class Diagrams

> **Dự án:** MiaoSong (喵送) — Nền tảng đặt món & điều phối giao hàng tự động (D2C F&B)  
> **Người phụ trách:** Trương Minh Thành · Review: Huỳnh Trần Thiên Phú  
> **Tài liệu liên quan:** [01-SRS.md](01-SRS.md) · [02-architecture.md](02-architecture.md) · [03-use-case-diagrams.md](03-use-case-diagrams.md) · [04-sequence-diagrams.md](04-sequence-diagrams.md) · [05-activity-diagrams.md](05-activity-diagrams.md)  
> **Mã nguồn PlantUML:** [diagrams/06-state-data-class/class-diagrams.puml](diagrams/06-state-data-class/class-diagrams.puml)

---

## 1. State machine đơn hàng

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT: đặt + ZaloPay
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
| `PENDING_PAYMENT` | `PLACED` / `CANCELLED` | ZaloPay Callback / khách hủy, Cloud Scheduler (15p) |
| `PLACED` | `CONFIRMED` / `REJECTED` / `CANCELLED` | quán / quán / khách |
| `CONFIRMED` | `READY` / `REJECTED` | quán (chế độ Bếp) / quán (hết nguyên liệu) |
| `READY` | `FINDING_DRIVER` | quán (bấm Gọi Shipper) |
| `FINDING_DRIVER` | `DRIVER_ASSIGNED` / `READY` | Lalamove gán xe / tài xế hủy chuyến |
| `DRIVER_ASSIGNED` | `ARRIVED_AT_STORE` / `READY` | Lalamove Webhook |
| `ARRIVED_AT_STORE` | `PICKED_UP` | Lalamove Webhook |
| `PICKED_UP` | `DELIVERING` | Lalamove Webhook |
| `DELIVERING` | `COMPLETED` / `DELIVERY_FAILED` | Lalamove Webhook |

---

## 2. State machine thanh toán & hoàn tiền

```mermaid
stateDiagram-v2
    state "payment.status" as P {
        [*] --> UNPAID
        UNPAID --> PAID: ZaloPay Callback OK
        UNPAID --> FAILED: thanh toán lỗi
        FAILED --> UNPAID: thanh toán lại
        PAID --> [*]
    }
    state "refundStatus" as R {
        [*] --> NONE
        NONE --> PENDING: đơn REJECTED/CANCELLED và đã PAID
        PENDING --> REFUNDED: ZaloPay Refund thành công
        PENDING --> REFUND_FAILED: API Refund lỗi
        REFUND_FAILED --> PENDING: thử lại thủ công
    }
```

---

## 3. State machine trạng thái quán

```mermaid
stateDiagram-v2
    [*] --> OPEN
    OPEN --> BUSY: chọn bận 15'/30'/1h/1 ngày
    BUSY --> OPEN: hết busyUntil / mở lại thủ công
    OPEN --> CLOSED: đóng thủ công / ngoài lịch tuần
    BUSY --> CLOSED: đóng thủ công
    CLOSED --> OPEN: mở thủ công / tới giờ mở theo lịch
```

---

## 4. ERD (mô hình logic thực thể)

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
        string role "CUSTOMER | MERCHANT"
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
        string avatarUrl
        string manualStatus "OPEN | BUSY | CLOSED"
        timestamp busyUntil
        int maxDistanceKm
        double ratingAvg
        int ratingCount
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
        string type "SINGLE | MULTI"
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
        string type "PERCENT | FIXED"
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
        string code "MS-241120-0012"
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
        string method "COD | ZALOPAY"
        string status "UNPAID | PAID | FAILED"
        long amountVnd
        string appTransId
        string gatewayTxnId
        string paymentUrl
        string refundStatus "NONE | PENDING | REFUNDED | REFUND_FAILED"
    }
    DELIVERY {
        string provider "LALAMOVE | MOCK"
        string quotationId
        string externalId
        string driverName
        string driverPhone
        string plate
        string shareLink
    }
    REVIEW {
        string orderId PK
        int stars
        string comment
        string tags
        string imageUrl
    }
```

---

## 5. Ánh xạ Firestore (mô hình vật lý)

| Path Collection | Ghi bởi | Ghi chú & Ràng buộc bảo mật |
|---|---|---|
| `users/{uid}` | client (hồ sơ), CF (role) | Client không được sửa role/storeId |
| `users/{uid}/addresses/{id}` | client | Quản lý địa chỉ giao hàng |
| `stores/{storeId}` | MERCHANT (Rules) | `weeklySchedule`, `specialDays` nhúng trong doc |
| `stores/{storeId}/categories/{id}` | MERCHANT | Nhóm danh mục món ăn |
| `stores/{storeId}/menuItems/{id}` | MERCHANT | `optionGroups[]` nhúng trực tiếp trong document |
| `stores/{storeId}/vouchers/{id}` | MERCHANT | `usedCount` chỉ do Cloud Functions tăng trong transaction |
| `voucherRedemptions/{voucherId_uid_orderId}` | chỉ CF | Dùng kiểm soát `perUserLimit` |
| `orders/{orderId}` | **chỉ CF** | Cấm client ghi trực tiếp. Toàn bộ items, payment, delivery snapshot |
| `reviews/{orderId}` | chỉ CF | `id = orderId` đảm bảo 1 đơn chỉ đánh giá 1 lần (hỗ trợ `imageUrl` từ Storage) |
| `idempotencyKeys/{uid_key}` | chỉ CF | Chống tạo đơn trùng khi mạng chập chờn hoặc bấm Đặt nhiều lần |
| `processedEvents/{source_eventId}` | chỉ CF | Chống webhook/IPN gửi lặp (Idempotency) |

---

## 6. Class Diagram — Backend Hexagonal Architecture (Module `orders`)

Mô hình cấu trúc Ports & Adapters bên trong module `orders` — trung tâm điều phối toàn bộ vòng đời đơn hàng, cách ly hoàn toàn logic domain với Firebase SDK.

```mermaid
classDiagram
    class PlaceOrderUseCase {
        -orderRepo: OrderRepository
        -catalogPort: CatalogPort
        -pricingPort: PricingPort
        -paymentPort: PaymentPort
        -eventPublisher: EventPublisher
        +execute(cmd: PlaceOrderCommand) OrderResult
    }
    class TransitionOrderUseCase {
        -orderRepo: OrderRepository
        -eventPublisher: EventPublisher
        +execute(orderId: String, newStatus: OrderStatus) Result
    }
    class DispatchDriverUseCase {
        -orderRepo: OrderRepository
        -deliveryPort: DeliveryPort
        +execute(orderId: String) DeliveryResult
    }

    class OrderRepository {
        <<interface>>
        +findById(id: String) Order
        +save(order: Order) void
        +saveInTransaction(order: Order, tx: Transaction) void
    }
    class CatalogPort {
        <<interface>>
        +isStoreOpen(storeId: String, at: Timestamp) Boolean
        +resolveItems(itemIds: List) List~ItemSnapshot~
    }
    class PricingPort {
        <<interface>>
        +calculate(lines: List, voucherCode: String, shipFee: Long) PriceBreakdown
        +reserveVoucher(storeId: String, code: String, userId: String) void
    }
    class DeliveryPort {
        <<interface>>
        +quote(pickup: LatLng, dropoff: LatLng) Quote
        +createOrder(order: Order, quote: Quote) ExternalOrder
    }
    class PaymentPort {
        <<interface>>
        +createPayment(order: Order) PaymentInit
        +refund(order: Order) RefundResult
    }
    class EventPublisher {
        <<interface>>
        +publish(event: DomainEvent) void
    }

    class FirestoreOrderRepository {
        -db: Firestore
        +findById(id: String) Order
        +save(order: Order) void
    }
    class CatalogAdapter {
        +isStoreOpen(storeId: String, at: Timestamp) Boolean
    }
    class PricingAdapter {
        +calculate(...) PriceBreakdown
    }
    class InProcessEventBus {
        +publish(event: DomainEvent) void
    }

    PlaceOrderUseCase --> OrderRepository
    PlaceOrderUseCase --> CatalogPort
    PlaceOrderUseCase --> PricingPort
    PlaceOrderUseCase --> PaymentPort
    PlaceOrderUseCase --> EventPublisher
    TransitionOrderUseCase --> OrderRepository
    TransitionOrderUseCase --> EventPublisher
    DispatchDriverUseCase --> OrderRepository
    DispatchDriverUseCase --> DeliveryPort

    FirestoreOrderRepository ..|> OrderRepository
    CatalogAdapter ..|> CatalogPort
    PricingAdapter ..|> PricingPort
    InProcessEventBus ..|> EventPublisher
```

---

## 7. Class Diagram — Strategy Pattern cho Delivery & Payment (Backend)

Cấu trúc mở rộng cho phép cắm/rút nhà cung cấp vận chuyển (Lalamove, Mock) và cổng thanh toán (COD, ZaloPay) mà không thay đổi luồng cốt lõi.

```mermaid
classDiagram
    class DeliveryProvider {
        <<interface>>
        +quote(pickup: LatLng, dropoff: LatLng) Quote
        +createOrder(order: Order, quote: Quote) ExternalOrder
        +getDriver(externalId: String, driverId: String) DriverDetail
        +cancel(externalId: String) void
        +parseWebhook(req: Request) DeliveryEvent
    }
    class LalamoveAdapter {
        -apiKey: String
        -secret: String
        -sign(method: String, path: String, body: String) String
        +quote(...) Quote
        +createOrder(...) ExternalOrder
    }
    class MockDeliveryAdapter {
        +quote(...) Quote
        +createOrder(...) ExternalOrder
        +advance(orderId: String, status: String) void
    }
    class DeliveryProviderFactory {
        +getProvider(type: String) DeliveryProvider
    }

    DeliveryProvider <|.. LalamoveAdapter
    DeliveryProvider <|.. MockDeliveryAdapter
    DeliveryProviderFactory ..> DeliveryProvider

    class PaymentGateway {
        <<interface>>
        +createPayment(order: Order) PaymentInit
        +confirm(payload: Any) PaymentResult
        +refund(order: Order) RefundResult
    }
    class CodGateway {
        +createPayment(order: Order) PaymentInit
        +confirm(payload: Any) PaymentResult
    }
    class ZalopayGateway {
        -appId: String
        -key1: String
        -key2: String
        -hmacSha256(data: String, key: String) String
        +createPayment(order: Order) PaymentInit
        +confirm(payload: Any) PaymentResult
        +refund(order: Order) RefundResult
    }
    class PaymentGatewayFactory {
        +getGateway(method: String) PaymentGateway
    }

    PaymentGateway <|.. CodGateway
    PaymentGateway <|.. ZalopayGateway
    PaymentGatewayFactory ..> PaymentGateway
```

---

## 8. Class Diagram — Domain Model Dùng Chung (Kotlin & TypeScript)

Các thực thể nghiệp vụ, Value Objects và thuật toán cốt lõi đảm bảo đồng nhất 100% giữa ứng dụng Android và Backend Functions.

```mermaid
classDiagram
    class Money {
        +amount: Long
        +plus(other: Money) Money
        +minus(other: Money) Money
        +times(factor: Int) Money
        +formatVnd() String
    }

    class CartLineKey {
        +value: String
        +generate(itemId: String, sizeId: String, toppings: List, note: String)$ CartLineKey
    }

    class CartLine {
        +key: CartLineKey
        +menuItemId: String
        +sizeOptionId: String
        +toppingOptionIds: List~String~
        +note: String
        +quantity: Int
        +clientUnitPrice: Money
        +lineTotal() Money
    }

    class PricingCalculator {
        +calculate(lines: List~CartLine~, voucher: Voucher, shipFee: Long) PriceBreakdown
    }

    class PriceBreakdown {
        +itemsTotal: Money
        +toppingsTotal: Money
        +subtotal: Money
        +discount: Money
        +shippingFee: Money
        +total: Money
    }

    class Voucher {
        +code: String
        +type: VoucherType
        +value: Long
        +maxDiscount: Long
        +minOrder: Long
        +calculateDiscount(subtotal: Money) Money
    }

    class VoucherSpecification {
        <<interface>>
        +isSatisfiedBy(orderContext: OrderContext) Boolean
    }
    class MinOrderSpec
    class DateRangeSpec
    class UsageLimitSpec
    class PerUserLimitSpec

    class OrderStateMachine {
        +canTransition(current: OrderStatus, target: OrderStatus) Boolean
        +validateTransition(current: OrderStatus, target: OrderStatus) void
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

---

## 9. Class Diagram — Android Clean Architecture: Feature Cart & Checkout

Kiến trúc UDF (Unidirectional Data Flow) của tính năng Giỏ hàng và Checkout phía Khách hàng.

```mermaid
classDiagram
    class CartScreen {
        <<Composable UI>>
        +CartContent(uiState: CartUiState, onEvent: (CartEvent) -> Unit)
    }

    class CartViewModel {
        -observeCartUseCase: ObserveCartUseCase
        -updateQuantityUseCase: UpdateQuantityUseCase
        -removeLineUseCase: RemoveLineUseCase
        +uiState: StateFlow~CartUiState~
        +onIncrease(key: CartLineKey)
        +onDecrease(key: CartLineKey)
        +onRemove(key: CartLineKey)
        +onProceedToCheckout()
    }

    class CartUiState {
        +lines: List~CartLineUiModel~
        +pricing: PriceBreakdown
        +isLoading: Boolean
        +hasUnavailableItems: Boolean
    }

    class ObserveCartUseCase {
        -cartRepo: CartRepository
        +invoke() Flow~List~CartLine~~
    }
    class UpdateQuantityUseCase {
        -cartRepo: CartRepository
        +invoke(key: CartLineKey, newQty: Int) void
    }

    class CartRepository {
        <<interface>>
        +observeCart() Flow~List~CartLine~~
        +upsert(line: CartLine) void
        +updateQuantity(key: CartLineKey, qty: Int) void
        +clearCart() void
    }

    class CartRepositoryImpl {
        -cartDao: CartDao
        +observeCart() Flow~List~CartLine~~
        +upsert(line: CartLine) void
    }

    class CartDao {
        <<Room DAO>>
        +getCartLines() Flow~List~CartLineEntity~~
        +insertOrUpdate(entity: CartLineEntity) void
        +deleteByKey(key: String) void
        +deleteAll() void
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

---

## 10. Class Diagram — Android Clean Architecture: Feature Theo dõi Đơn & Bản đồ OSMDroid Realtime

Thiết kế phân lớp cho màn hình Theo dõi đơn hàng (`OrderTrackingScreen`), tích hợp bản đồ OpenStreetMap (OSMDroid) + tuyến đường bộ OSRM, đối soát thanh toán ZaloPay tự động và đánh giá kèm ảnh.

```mermaid
classDiagram
    class OrderTrackingScreen {
        <<Composable UI>>
        +OrderTrackingScreen(orderId: String?, onBack: () -> Unit)
        -OsmDeliveryMapView(pickup: GeoPoint, dropoff: GeoPoint, driver: GeoPoint, routePoints: List)
    }

    class OrderTrackingViewModel {
        -authRepository: AuthRepository
        -orderRepository: OrderRepository
        -storeRepository: StoreRepository
        +uiState: StateFlow~OrderTrackingUiState~
        +bindOrderId(orderId: String?)
        +reconcilePendingPayment(silent: Boolean)
        +cancelPendingOrder()
        +submitReview(stars: Int, tags: List, comment: String, imageBytes: ByteArray?)
    }

    class OrderTrackingUiState {
        +order: Order
        +store: Store?
        +customerName: String
        +submittedStars: Int?
        +feedbackMessage: String?
    }

    class OrderRepository {
        <<interface>>
        +observeOrder(orderId: String) Flow~Order?~
        +confirmPayment(orderId: String, appTransId: String) AppResult~Boolean~
        +cancelOrder(orderId: String, reason: String) AppResult~Unit~
        +submitReview(orderId: String, stars: Int, tags: List, comment: String, imageBytes: ByteArray?) AppResult~Unit~
    }

    class OrderRepositoryImpl {
        -firestore: FirebaseFirestore
        -functions: FirebaseFunctions
        -storage: FirebaseStorage
    }

    OrderTrackingScreen --> OrderTrackingViewModel
    OrderTrackingViewModel --> OrderTrackingUiState
    OrderTrackingViewModel --> OrderRepository
    OrderRepository <|.. OrderRepositoryImpl
```

---

## 11. Class Diagram — Android Clean Architecture: Feature Quán & Bếp (KDS)

Mô hình phân lớp cho Màn hình Bếp (Kitchen Display System), hỗ trợ giữ sáng màn hình và lọc nổi bật món ăn có ghi chú dị ứng.

```mermaid
classDiagram
    class KdsScreen {
        <<Composable UI>>
        +KdsContent(uiState: KdsUiState, onMarkReady: (String) -> Unit)
    }

    class KdsViewModel {
        -observeKitchenOrdersUseCase: ObserveKitchenOrdersUseCase
        -markReadyUseCase: MarkOrderReadyUseCase
        +uiState: StateFlow~KdsUiState~
        +onMarkOrderReady(orderId: String)
    }

    class KdsUiState {
        +activeTickets: List~KitchenTicketUiModel~
        +selectedFilter: TicketFilter
        +isLoading: Boolean
    }

    class KitchenTicketUiModel {
        +orderId: String
        +orderCode: String
        +elapsedMinutes: Int
        +items: List~KitchenItemUiModel~
        +hasAllergyAlert: Boolean
        +specialNotes: String?
    }

    class ObserveKitchenOrdersUseCase {
        -orderRepo: MerchantOrderRepository
        +invoke(storeId: String) Flow~List~Order~~
    }

    class MarkOrderReadyUseCase {
        -orderRepo: MerchantOrderRepository
        +invoke(orderId: String) Result~Unit~
    }

    class MerchantOrderRepository {
        <<interface>>
        +observeConfirmedOrders(storeId: String) Flow~List~Order~~
        +markReady(orderId: String) Result~Unit~
        +dispatchShipper(orderId: String) Result~String~
    }

    class MerchantOrderRepositoryImpl {
        -orderDataSource: FirestoreOrderDataSource
        -functionsDataSource: CloudFunctionsOrderDataSource
        +observeConfirmedOrders(storeId: String) Flow~List~Order~~
        +markReady(orderId: String) Result~Unit~
    }

    KdsScreen --> KdsViewModel
    KdsViewModel --> KdsUiState
    KdsUiState --> KitchenTicketUiModel
    KdsViewModel --> ObserveKitchenOrdersUseCase
    KdsViewModel --> MarkOrderReadyUseCase
    ObserveKitchenOrdersUseCase --> MerchantOrderRepository
    MarkOrderReadyUseCase --> MerchantOrderRepository
    MerchantOrderRepository <|.. MerchantOrderRepositoryImpl
```
