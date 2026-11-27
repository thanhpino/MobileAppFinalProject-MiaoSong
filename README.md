# 🐱 MiaoSong (喵送)

> **Meo một tiếng, món tới liền.**
> Nền tảng đặt món trực tiếp từ quán (**D2C F&B**) & điều phối giao hàng tự động qua **Lalamove** — loại bỏ trung gian, đồng bộ bếp (KDS) & tài xế realtime.
> Đồ án cuối kỳ môn **Phát triển ứng dụng di động** — HK1/2026-2027.

---

## 1. Thành viên thực hiện

| Họ tên | MSSV | Vai trò | Nhánh Git |
|---|---|---|---|
| **Trương Minh Thành** | `524H0032` | Tech Lead · Kiến trúc · Backend Modular Monolith · Chế độ Quán (KDS, Menu, Voucher, Quán, Foreground Service) · Tracking OSMDroid · Release | `thanhpino` |
| **Huỳnh Trần Thiên Phú** | `524H0023` | Business Analyst · Android Chế độ Khách (Auth, Home, Store Detail, Item Customizer, Cart, Address, Profile) · Design System · UI/UX | `thienphu` |

---

## 2. Kiến trúc & Tech Stack

### Android App (`vn.miaosong.app` — 1 app, 2 chế độ Khách & Quán)
- **Ngôn ngữ & UI:** Kotlin 2.x · Jetpack Compose · Material 3 (Edge-to-Edge, `minSdk 26`, `targetSdk 36`)
- **Kiến trúc:** Multi-module Clean Architecture + MVVM (UDF: `UiState` + `StateFlow`)
  - `:app` (entry point, role router, Deep Link, FCM, Foreground Service)
  - `:feature-customer` & `:feature-merchant` (độc lập nhau, chỉ phụ thuộc `:core:*`)
  - `:core:model` · `:core:common` · `:core:domain` · `:core:data` · `:core:designsystem`
- **Thư viện chính:** Hilt DI · Coroutines/Flow · Room (giỏ hàng tách dòng `CartLineKey`) · Coil 3 · Chrome Custom Tabs (ZaloPay) · **OSMDroid (OpenStreetMap) + OSRM Routing**

### Backend (`miaosong-pro` — Modular Monolith trên Firebase Serverless)
- **Nền tảng:** Firebase Auth (Custom Claims `role`, `storeId`) · Cloud Firestore · Cloud Functions v2 (Node.js 22, TypeScript, `asia-southeast1`) · Cloud Storage · FCM · Firebase Hosting
- **6 Module tách biệt (Hexagonal — Ports & Adapters):** `catalog`, `pricing`, `orders`, `delivery`, `payment`, `notification` (+ `review`)
  - Kiểm soát ranh giới tự động bằng `dependency-cruiser` (4 luật: Public API qua `index.ts`, sở hữu dữ liệu theo module, `domain` không phụ thuộc Firebase SDK, không phụ thuộc vòng).
- **Tích hợp bên thứ ba:**
  - **Thanh toán:** COD & **ZaloPay Sandbox v2** (`app_id = 2554`, HMAC-SHA256 create/query/refund + IPN callback + auto-reconcile khi quay về app).
  - **Vận chuyển:** **Lalamove API v3** + **MockDeliveryProvider** (Strategy + Adapter pattern, dễ mở rộng đối tác mới).
  - **Bản đồ:** **OpenStreetMap (OSMDroid)** + **OSRM** tìm tuyến đường bộ ngắn nhất + Nominatim Geocoding.

### Hạ tầng đã triển khai Live
- **Web / Hosting & Deep Link:** [https://miaosong-pro.web.app](https://miaosong-pro.web.app)
- **Chính sách quyền riêng tư (Google Play):** [https://miaosong-pro.web.app/privacy-policy.html](https://miaosong-pro.web.app/privacy-policy.html)
- **Deep Link mở quán:** `https://miaosong-pro.web.app/s/{slug}` hoặc `miaosong://s/{slug}`
- **ZaloPay Return Link:** `https://miaosong-pro.web.app/pay/return?orderId=...` → `miaosong://pay/return`

---

## 3. Tính năng chính đã hoàn thiện

### Chế độ Khách (`CUSTOMER`)
- Đăng nhập / Đăng ký (Firebase Auth), quản lý hồ sơ, danh sách quán yêu thích, chia sẻ link quán (Deep Link).
- Trang chủ lọc theo danh mục, sắp xếp gần tôi / giao nhanh / đánh giá cao, tìm kiếm không dấu.
- Trang quán: danh mục món, khóa món tạm hết hàng, tùy chỉnh Size (`SINGLE` bắt buộc) & Topping (`MULTI`), ghi chú món & dị ứng.
- Giỏ hàng Room tự động tách dòng theo `CartLineKey` (cùng món khác ghi chú = 2 dòng riêng), áp Voucher thật từ Firestore (chỉ giảm trên tiền món, không trừ phí ship), báo giá giao hàng realtime.
- Thanh toán **COD** hoặc **ZaloPay Sandbox** qua Chrome Custom Tabs (hỗ trợ thanh toán lại / hủy đơn `PENDING_PAYMENT` hoàn lượt voucher / tự động đối soát `confirmPayment`).
- Theo dõi đơn hàng realtime trên bản đồ **OSMDroid + OSRM**, gọi tài xế trực tiếp, hiển thị lý do từ chối & trạng thái hoàn tiền ZaloPay, đánh giá 1–5 sao kèm **upload ảnh**, đặt lại đơn (Reorder).

### Chế độ Quán (`MERCHANT`)
- **Thực đơn:** CRUD nhóm món, món ăn, upload ảnh món lên Firebase Storage, tùy chọn `SINGLE`/`MULTI`, bật/tắt **Hết hàng** 1 chạm.
- **Voucher:** Tạo, bật/tắt, xóa mã giảm giá (`PERCENT` / `FIXED`).
- **Bếp (KDS) & Điều phối:** **Foreground Service** lắng nghe đơn mới ngay cả khi tắt màn hình, đổ chuông & bấm thông báo nhảy thẳng tab Bếp; nổi bật thẻ **Ghi chú (vàng)** & **Dị ứng (đỏ)**; Xác nhận / Từ chối đơn (tự động hoàn tiền ZaloPay & hoàn voucher) / Báo xong món / Gọi tài xế Lalamove; cảnh báo đỏ đơn giao trễ ≥ 30 phút.
- **Quán:** Upload avatar quán, sửa thông tin, đổi trạng thái 1 chạm (`Mở cửa` / `Tạm bận 15p–1d` / `Đóng cửa`), cài đặt lịch hoạt động thường ngày & ngày đặc biệt, xem đánh giá của khách.

---

## 4. Tài khoản Demo & Thẻ Test (cho Giảng viên)

| Vai trò | Email | Ghi chú |
|---|---|---|
| **Khách hàng (`CUSTOMER`)** | `iamphupq@gmail.com` | Hoặc đăng ký mới trực tiếp trên app |
| **Chủ quán (`MERCHANT`)** | `tt3145539@gmail.com` | Quản lý quán *Bún Riêu Cô Ba*, có nút chuyển nhanh sang chế độ Khách & quay lại Quán |

**Thẻ Visa Test trên cổng ZaloPay Sandbox (`qcgateway.zalopay.vn`):**
- Số thẻ: `4111 1111 1111 1111` · Hết hạn: `12/28` · Tên: `NGUYEN VAN A` · CVV: `123` · OTP (nếu hỏi): `111111` hoặc `123456`.

---

## 5. Hướng dẫn Build & Chạy Kiểm thử

### Android (Yêu cầu JDK 17+, Android Studio Iguana/Ladybug+)
```bash
cd android

# Chạy Kotlin Unit Test (PricingCalculator, CartLineKey, Domain)
./gradlew :core:domain:test

# Build Debug APK
./gradlew :app:assembleDebug

# Build Release AAB (đã bật R8/ProGuard & ShrinkResources cho Google Play)
./gradlew :app:bundleRelease
# File đầu ra: android/app/build/outputs/bundle/release/app-release.aab
```

### Backend Cloud Functions (Node.js 22+)
```bash
cd backend/functions
npm install

# Chạy Unit Test (12/12 tests gồm shared pricing test vectors)
npm test

# Kiểm tra TypeScript & 4 luật kiến trúc Modular Monolith (dependency-cruiser)
npm run lint

# Build TypeScript
npm run build
```

---

## 6. Danh mục tài liệu thiết kế (`docs/`)

| # | Tài liệu | Nội dung | Trạng thái |
|---|---|---|---|
| 01 | [SRS](docs/01-SRS.md) | Đặc tả yêu cầu phần mềm đầy đủ (Use Cases, FRs, BRs, NFRs) | ✅ Hoàn thành |
| 02 | [Kiến trúc](docs/02-architecture.md) | ADR, C4 (L1–L3), Modular Monolith, Hexagonal, Android, Bảo mật | ✅ Hoàn thành |
| 03 | [Use Case Diagrams](docs/03-use-case-diagrams.md) | Bộ sơ đồ Use Case (Actor, Tổng quan, Khách, Quán/Bếp, Hệ thống) | ✅ Hoàn thành |
| 04 | [Sequence Diagrams](docs/04-sequence-diagrams.md) | 16 Sequence Diagrams các luồng nghiệp vụ lõi (`SD-01` → `SD-16`) | ✅ Hoàn thành |
| 05 | [Activity Diagrams](docs/05-activity-diagrams.md) | 5 Activity Diagrams luồng đặt đơn, KDS, hoàn tiền ZaloPay, Scheduler & Reorder | ✅ Hoàn thành |
| 06 | [State · Data · Class](docs/06-state-data-class.md) | State Machine đơn hàng, ERD, Firestore Schema, Class Diagrams | ✅ Hoàn thành |
| 07 | [Test Plan](docs/07-test-plan.md) | Kế hoạch kiểm thử (Test Cases) & Kịch bản Demo 2 máy thật | ✅ Hoàn thành |
| 08 | [API Contract & Rules](docs/08-api-contract.md) | 15 Cloud Functions, Webhooks, Error Codes, FCM, Firestore Rules, [Test Vectors](shared/test-vectors/pricing.json) | ✅ Hoàn thành |
| 09 | [Interactive Visual Flow (HTML)](docs/miaosong-visual-flow.html) | Trang HTML tương tác trực quan (Archify): Kiến trúc, Sequence E2E, Hoàn tiền ZaloPay & ERD | ✅ Hoàn thành |

---

## 7. Trạng thái dự án

🟢 **Feature Freeze (`v1.0.0`)** — Toàn bộ tính năng Android (Khách & Quán), Backend Cloud Functions (15 functions live trên `miaosong-pro`), Unit Test (Backend 12/12 + Kotlin Domain) và Release `.aab` build đều đã hoàn thiện & vượt qua kiểm thử.
