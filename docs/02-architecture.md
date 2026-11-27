# 02 — Kiến trúc hệ thống MiaoSong

> Người phụ trách: **Trương Minh Thành** · Review: **Huỳnh Trần Thiên Phú**
> Liên quan: [01-SRS.md](01-SRS.md) · [03-use-case-diagrams.md](03-use-case-diagrams.md) · [04-sequence-diagrams.md](04-sequence-diagrams.md) · [05-activity-diagrams.md](05-activity-diagrams.md) · [06-state-data-class.md](06-state-data-class.md) · [07-test-plan.md](07-test-plan.md) · [08-api-contract.md](08-api-contract.md)

## Mục lục
1. [Quyết định kiến trúc (ADR)](#1-quyết-định-kiến-trúc-adr)
2. [C4 Level 1 — System Context](#2-c4-level-1--system-context)
3. [C4 Level 2 — Container](#3-c4-level-2--container)
4. [Backend — Modular Monolith (Component)](#4-backend--modular-monolith-component)
5. [Bên trong một module — Hexagonal](#5-bên-trong-một-module--hexagonal)
6. [Luồng sự kiện giữa module](#6-luồng-sự-kiện-giữa-module)
7. [Kiến trúc Android](#7-kiến-trúc-android)
8. [Deployment](#8-deployment)
9. [Kiến trúc bảo mật](#9-kiến-trúc-bảo-mật)
10. [Cấu trúc thư mục](#10-cấu-trúc-thư-mục)

---

## 1. Quyết định kiến trúc (ADR)

| ID | Quyết định | Lý do | Phương án đã loại |
|---|---|---|---|
| ADR-01 | **Modular Monolith** trên Cloud Functions | 2 người / 12 ngày; giao dịch đặt đơn + voucher cần 1 transaction; không tốn chi phí vận hành | Microservices (quá tải hạ tầng, giao dịch phân tán) |
| ADR-02 | **1 app Android, 2 chế độ** theo role | 1 store listing, 1 CI; giống "Kênh Người Bán" của Shopee | 2 app riêng (gấp đôi công phát hành) |
| ADR-03 | **Firestore** làm DB + kênh realtime | Snapshot listener thay cho WebSocket tự viết | PostgreSQL + Socket.IO (phải tự host) |
| ADR-04 | **Mọi ghi vào `orders` qua Cloud Functions** | Server là nguồn sự thật về giá & trạng thái | Client ghi trực tiếp (dễ gian lận giá) |
| ADR-05 | **Strategy + Adapter** cho Delivery và Payment | Thêm Lalamove/Mock, COD/ZaloPay không sửa luồng đặt đơn | `if/else` theo provider |
| ADR-06 | **MockDeliveryProvider** song song Lalamove | Sandbox không có tài xế thật → demo vẫn trọn luồng | Chỉ dùng sandbox |
| ADR-07 | **Tiền = số nguyên VND** (`Long` / `number` integer) | Tránh sai số dấu phẩy động | `Double` |
| ADR-08 | **Clean Architecture + MVVM (UDF)** trên Android | Tách UI khỏi logic, test được ViewModel/UseCase | MVC / logic trong Composable |
| ADR-09 | **Idempotency** cho `placeOrder`, webhook, IPN | Mạng chập chờn, đối tác gửi lặp | Không kiểm tra trùng |

---

## 2. C4 Level 1 — System Context

```mermaid
flowchart TB
    KH(["👤 Khách hàng"])
    Q(["🏪 Quán"])
    MS["MiaoSong Platform<br/>Đặt món D2C & điều phối giao hàng"]
    LL["🛵 Lalamove<br/>Giao hàng"]
    ZLP["💳 ZaloPay<br/>Thanh toán nội địa"]
    OSM["🗺️ OpenStreetMap<br/>Địa chỉ & bản đồ"]
    FCM["Firebase Cloud Messaging"]
    KH -- "duyệt menu, đặt món, thanh toán, theo dõi, đánh giá" --> MS
    Q -- "quản lý menu, voucher, nhận đơn, bếp, gọi shipper" --> MS
    MS -- "báo giá, tạo chuyến" --> LL
    LL -- "webhook trạng thái, tài xế" --> MS
    MS <-- "tạo giao dịch, callback, hoàn tiền" --> ZLP
    MS -- "Nominatim search, Geocoding" --> OSM
    MS -- "gửi push" --> FCM
    FCM -. "thông báo" .-> KH
    FCM -. "chuông đơn mới" .-> Q
```

## 3. C4 Level 2 — Container

```mermaid
flowchart TB
    subgraph Device["📱 Thiết bị Android"]
        APP["MiaoSong App<br/>Kotlin · Jetpack Compose<br/>Chế độ Khách / Chế độ Quán"]
        ROOM[("Room DB<br/>giỏ hàng offline")]
        DS[("DataStore<br/>cài đặt, role")]
        APP --> ROOM
        APP --> DS
    end
    subgraph Firebase["☁️ Firebase · asia-southeast1"]
        AUTH["Firebase Auth<br/>Google, Email + custom claims"]
        FS[("Cloud Firestore<br/>dữ liệu + realtime")]
        CF["Cloud Functions 2nd gen<br/>TypeScript · Modular Monolith"]
        ST[("Cloud Storage<br/>ảnh món")]
        HOST["Firebase Hosting<br/>privacy policy, trang return thanh toán, assetlinks.json"]
        SCH["Cloud Scheduler"]
        FCMS["FCM"]
    end
    APP -- "đăng nhập" --> AUTH
    APP -- "đọc realtime, ghi menu/địa chỉ (Rules)" --> FS
    APP -- "callable HTTPS" --> CF
    APP -- "upload ảnh" --> ST
    APP -- "App Link sau thanh toán" --> HOST
    CF -- "Admin SDK" --> FS
    CF --> FCMS
    SCH -- "mỗi 5 phút" --> CF
    FCMS -. push .-> APP
    CF <--> LL["Lalamove API v3"]
    CF <--> ZLP["ZaloPay"]
    APP --> OSM["OpenStreetMap (Nominatim)"]
```

## 4. Backend — Modular Monolith (Component)

```mermaid
flowchart LR
    subgraph Handlers["handlers — lớp mỏng (15 Cloud Functions)"]
        HC["onCall: placeOrder, confirmOrder, rejectOrder, cancelOrder,<br/>markReady, dispatchDriver, mockDeliveryAdvance, confirmPayment,<br/>getDeliveryQuote, validateVoucher, submitReview"]
        HW["onRequest: lalamoveWebhook, zalopayCallback"]
        HT["triggers: onOrderUpdated, expirePendingOrders (scheduler)"]
    end
    subgraph Modules["modules"]
        CAT["catalog<br/>store, menu, trạng thái quán"]
        PRI["pricing<br/>tính tiền, voucher"]
        ORD["orders<br/>đặt đơn, state machine"]
        DEL["delivery<br/>Lalamove, Mock"]
        PAY["payment<br/>COD, ZaloPay"]
        NOT["notification<br/>FCM"]
        REV["review<br/>đánh giá"]
    end
    SH["shared kernel<br/>Money, Result, AppError, EventBus, Idempotency"]
    HC --> ORD
    HC --> PRI
    HC --> DEL
    HC --> REV
    HW --> DEL
    HW --> PAY
    HT --> NOT
    HT --> ORD
    ORD --> CAT
    ORD --> PRI
    ORD --> DEL
    ORD --> PAY
    Modules -.-> SH
```

**Bảng sở hữu dữ liệu (luật R2):**

| Module | Collection sở hữu | API công khai (`index.ts`) |
|---|---|---|
| catalog | `stores`, `stores/*/categories`, `stores/*/menuItems` | `getStoreForOrder()`, `resolveItems()`, `isStoreOpen()` |
| pricing | `stores/*/vouchers`, `voucherRedemptions` | `calculate()`, `validateVoucher()`, `reserveVoucher(tx)`, `releaseVoucher(tx)` |
| orders | `orders` | `placeOrder()`, `transition()`, `getOrder()` |
| delivery | `processedEvents` (LALAMOVE) | `quote()`, `dispatch()`, `cancel()`, `handleWebhook()` |
| payment | `processedEvents` (ZALOPAY), `config/payment` | `createPayment()`, `confirm()`, `refund()` |
| notification | `users/*.fcmTokens` (đọc) | `notifyOrderStatus()` |
| review | `reviews` | `submitReview()` |

**4 luật module** (kiểm tra bằng `dependency-cruiser` trong CI):
1. **R1** — chỉ import `modules/<x>/index.ts` của module khác.
2. **R2** — mỗi collection chỉ một module được ghi.
3. **R3** — `handlers → application → domain`; `domain` không import Firebase.
4. **R4** — không phụ thuộc vòng; `delivery`/`payment` báo ngược cho `orders` bằng **event**.

## 5. Bên trong một module — Hexagonal

Ví dụ module `orders`:

```mermaid
flowchart LR
    subgraph In["Driving adapters"]
        H1["handlers/placeOrder.ts"]
        H2["handlers/onDeliveryEvent.ts"]
    end
    subgraph App["application"]
        UC1["PlaceOrderUseCase"]
        UC2["TransitionOrderUseCase"]
    end
    subgraph Dom["domain — thuần TypeScript"]
        E1["Order (entity)"]
        E2["OrderStateMachine"]
        E3["OrderItemSnapshot (VO)"]
    end
    subgraph Ports["ports"]
        P1["OrderRepository"]
        P2["CatalogPort"]
        P3["PricingPort"]
        P4["PaymentPort"]
        P5["EventPublisher"]
    end
    subgraph Out["Driven adapters"]
        A1["FirestoreOrderRepository"]
        A2["catalog/index.ts"]
        A3["pricing/index.ts"]
        A4["payment/index.ts"]
        A5["InProcessEventBus"]
    end
    H1 --> UC1
    H2 --> UC2
    UC1 --> E1
    UC2 --> E2
    UC1 --> P1 & P2 & P3 & P4
    UC2 --> P1 & P5
    A1 -. implements .-> P1
    A2 -. implements .-> P2
    A3 -. implements .-> P3
    A4 -. implements .-> P4
    A5 -. implements .-> P5
```

## 6. Luồng sự kiện giữa module

```mermaid
flowchart LR
    LLW["Webhook Lalamove"] --> DEL["delivery.handleWebhook"]
    DEL -- "DeliveryStatusChanged" --> BUS(("EventBus"))
    PAYW["Callback ZaloPay"] --> PAY["payment.confirm"]
    PAY -- "PaymentSucceeded / PaymentRefunded" --> BUS
    BUS --> ORD["orders.transition"]
    ORD -- "ghi orders/{id}" --> FS[("Firestore")]
    FS -- "onDocumentUpdated" --> TRG["trigger onOrderUpdated"]
    TRG --> NOT["notification.notifyOrderStatus"]
    TRG -- "REJECTED / CANCELLED + đã trả online" --> REF["payment.refund"]
    NOT --> FCM["FCM"]
```

| Event | Phát bởi | Xử lý bởi |
|---|---|---|
| `DeliveryStatusChanged` | delivery | orders |
| `PaymentSucceeded` | payment | orders (`PENDING_PAYMENT → PLACED`) |
| `PaymentRefunded` / `RefundFailed` | payment | orders (`refundStatus`) |
| `OrderStatusChanged` (Firestore trigger) | orders | notification, payment (hoàn tiền) |

> Khi cần tách microservice: `InProcessEventBus` → **Cloud Pub/Sub**, lời gọi `index.ts` → HTTP. Logic nghiệp vụ giữ nguyên.

## 7. Kiến trúc Android

### 7.1 Module graph

```mermaid
flowchart TB
    APP[":app<br/>MainActivity, RoleRouter, Hilt graph, FCM service"]
    FC[":feature-customer"]
    FM[":feature-merchant"]
    DOM[":core:domain<br/>UseCase, PricingCalculator, CartLineKey, StoreStatusResolver"]
    DATA[":core:data<br/>Repository impl, Firestore, Room, Retrofit"]
    MOD[":core:model<br/>data class thuần Kotlin"]
    DSY[":core:designsystem<br/>MiaoTheme, token, component"]
    COM[":core:common<br/>Result, MoneyFormatter, Dispatchers"]
    APP --> FC & FM
    FC --> DOM & DSY
    FM --> DOM & DSY
    DOM --> MOD & COM
    DATA --> DOM
    APP --> DATA
```
`feature-customer` và `feature-merchant` **không phụ thuộc nhau**; `:core:domain` định nghĩa interface Repository, `:core:data` implement (Dependency Inversion).

### 7.2 Phân lớp trong một màn hình (UDF)

```mermaid
flowchart LR
    UI["Composable Screen"] -- "UiEvent (onAddToCart)" --> VM["ViewModel"]
    VM -- "StateFlow of UiState" --> UI
    VM -- "one-off effect (navigate, snackbar)" --> UI
    VM --> UC["UseCase"]
    UC --> REPO["Repository (interface)"]
    REPO --> RI["RepositoryImpl"]
    RI --> FSD["Firestore DataSource"]
    RI --> RD["Room DAO"]
    RI --> FN["Functions DataSource"]
```

### 7.3 Điều hướng theo role

```mermaid
flowchart TD
    S["Splash"] --> A{"Đã đăng nhập?"}
    A -- "Chưa" --> L["Onboarding → Login"]
    L --> R
    A -- "Rồi" --> R{"custom claim role"}
    R -- "CUSTOMER" --> C["CustomerNavGraph<br/>Home · Đơn hàng · Tài khoản"]
    R -- "MERCHANT" --> M["MerchantNavGraph<br/>Đơn · Bếp (KDS) · Menu · Quán"]
    DL["Deep link /s/slug"] --> C
```

## 8. Deployment

```mermaid
flowchart LR
    DEV["💻 Máy dev<br/>Android Studio, Firebase Emulator, ngrok"] -- "git push" --> GH["GitHub<br/>thanhpino/MobileAppFinalProject-MiaoSong"]
    GH -- "GitHub Actions: lint, test, assembleRelease" --> AAB["app-release.aab"]
    GH -- "firebase deploy (main)" --> FB["Firebase project miaosong-prod"]
    AAB -- "upload" --> PLAY["Google Play Console<br/>Internal testing"]
    PLAY --> P1["📱 Máy khách"]
    PLAY --> P2["📱 Máy quán"]
    FB <--> EXT["Lalamove sandbox · ZaloPay sandbox · OpenStreetMap"]
```

| Môi trường | Firebase project | Đối tác |
|---|---|---|
| local | Emulator Suite | MockProvider, ZaloPay sandbox qua ngrok |
| prod (demo) | `miaosong-prod` | Lalamove sandbox + Mock, ZaloPay sandbox, OpenStreetMap |

## 9. Kiến trúc bảo mật

| Lớp | Biện pháp |
|---|---|
| Danh tính | Firebase Auth; role (`CUSTOMER` / `MERCHANT`) & `storeId` lưu trong **custom claims** (chỉ Functions set) |
| Dữ liệu | Firestore Rules: khách đọc `orders` có `customerId == uid`; quán đọc `orders` có `storeId == token.storeId`; **client không ghi `orders`, `vouchers.usedCount`, `reviews` trực tiếp** |
| Secret | API key Lalamove/ZaloPay (`key1`, `key2`) trong **Secret Manager** (`defineSecret`), không có trong APK |
| Webhook | Lalamove: xác minh chữ ký HMAC; ZaloPay: MAC HMAC-SHA256 (`key2`) |
| Chống lặp | `processedEvents/{source_eventId}`, `idempotencyKey` của đơn |
| App | R8 obfuscation, HTTPS, App Check (Could) |

## 10. Cấu trúc thư mục

```
MiaoSong/
├── android/
│   ├── app/
│   ├── feature-customer/  (home, store, item, cart, checkout, payment, tracking, history, review, profile)
│   ├── feature-merchant/  (inbox, orderdetail, kitchen, dispatch, menu, voucher, storestatus, schedule, reviews)
│   └── core/ (model, domain, data, designsystem, common)
├── backend/functions/src/
│   ├── modules/ (catalog, pricing, orders, delivery, payment, notification, review)
│   │   └── <module>/ index.ts · domain/ · application/ · ports/ · adapters/ · handlers/
│   ├── shared/
│   └── index.ts
├── shared/test-vectors/pricing.json
└── docs/
```

