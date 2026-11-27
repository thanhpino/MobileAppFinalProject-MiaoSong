# 07 — Kế hoạch Kiểm thử Tự động Toàn hệ thống (Automated Test Plan)

> **Dự án:** MiaoSong (喵送) — Nền tảng đặt món & điều phối giao hàng tự động (D2C F&B)  
> **Người phụ trách:** Huỳnh Trần Thiên Phú (P2) & Trương Minh Thành (P1)  
> **Tiêu chuẩn tham chiếu:** ISO/IEC/IEEE 29119 (Software Testing Standard)  
> **Tài liệu liên quan:** [01-SRS.md](01-SRS.md) · [02-architecture.md](02-architecture.md) · [04-sequence-diagrams.md](04-sequence-diagrams.md) · [06-state-data-class.md](06-state-data-class.md) · [08-api-contract.md](08-api-contract.md)  
> **Dữ liệu test dùng chung:** [shared/test-vectors/pricing.json](../../shared/test-vectors/pricing.json)

---

## Mục lục
1. [Chiến lược & Kim tự tháp Kiểm thử](#1-chiến-lược--kim-tự-tháp-kiểm-thử)
2. [Ma trận Kiểm thử Tiêu chí Nghiệm thu (AC Matrix)](#2-ma-trận-kiểm-thử-tiêu-chí-nghiệm-thu-ac-matrix)
3. [Kế hoạch Unit Test (Kiểm thử Đơn vị - Domain Logic)](#3-kế-hoạch-unit-test-kiểm-thử-đơn-vị---domain-logic)
4. [Kế hoạch Integration Test (Firebase Emulator Suite)](#4-kế-hoạch-integration-test-firebase-emulator-suite)
5. [Kế hoạch UI & Instrumental Test (Jetpack Compose UI)](#5-kế-hoạch-ui--instrumental-test-jetpack-compose-ui)
6. [Kế hoạch Kiểm thử Bảo mật & Ràng buộc Dữ liệu (Security Rules)](#6-kế-hoạch-kiểm-thử-bảo-mật--ràng-buộc-dữ-liệu-security-rules)
7. [Kịch bản Kiểm thử Đầu-cuối (End-to-End Demo Script trên 2 máy thật)](#7-kịch-bản-kiểm-thử-đầu-cuối-end-to-end-demo-script-trên-2-máy-thật)
8. [Tiêu chuẩn Hoàn thành Kiểm thử (Definition of Done - DoD)](#8-tiêu-chuẩn-hoàn-thành-kiểm-thử-definition-of-done---dod)

---

## 1. Chiến lược & Kim tự tháp Kiểm thử

Hệ thống MiaoSong áp dụng mô hình **Kim tự tháp kiểm thử (Testing Pyramid)** nhằm tối đa hóa tốc độ phản hồi và độ tin cậy trong thời gian phát triển 12 ngày:

```
          / \
         /   \        E2E / UI Tests (10%)
        / E2E \       Compose UI Test, Demo 2 máy thật qua scrcpy
       /-------\
      /         \     Integration Tests (20%)
     /   INTEG   \    Firebase Emulator: Concurrency, Idempotency, ZaloPay Callback, Webhooks
    /-------------\
   /               \  Unit Tests (70%)
  /      UNIT       \ Domain logic thuần túy: PricingCalculator, CartLineKey,
 /                   \ OrderStateMachine, VoucherSpecs (JUnit 5, MockK, Vitest)
/---------------------\
```

### Phân công công cụ kiểm thử:
| Tầng kiểm thử | Phía Android (Kotlin) | Phía Backend (TypeScript) |
|---|---|---|
| **Unit Test** | JUnit 5, MockK, Turbine (kiểm thử Flow), Kotlin Coroutines Test | Vitest / Jest, TypeScript compiler |
| **Integration Test** | AndroidX Test Runner, Hilt Android Testing | Firebase Emulator Suite (Firestore, Functions, Auth), Supertest |
| **UI Test** | Jetpack Compose Testing (`createComposeRule`), Semantics Matcher | — |
| **Bảo mật** | — | `@firebase/rules-unit-testing` (thẩm định Firestore Security Rules) |

---

## 2. Ma trận Kiểm thử Tiêu chí Nghiệm thu (AC Matrix)

Ánh xạ trực tiếp 10 tiêu chí nghiệm thu từ `01-SRS.md` sang các bộ kiểm thử tự động tương ứng:

| Mã AC | Mô tả Tiêu chí Nghiệm thu | Tầng kiểm thử | Test Case ID | Trạng thái |
|---|---|---|---|---|
| **AC-01** | Thêm 2 tô bún cùng loại (1 không hành, 1 nhiều hành) $\rightarrow$ tách thành 2 dòng riêng | Unit / UI | `TC-UNIT-CLK-01`, `TC-UI-CART-01` | Sẵn sàng |
| **AC-02** | Voucher 20k, minOrder 100k, subtotal 95k $\rightarrow$ từ chối áp mã & báo thiếu 5k | Unit / Backend | `TC-UNIT-PRICE-04`, `TC-INT-VOUCHER-02` | Sẵn sàng |
| **AC-03** | Subtotal 15k, voucher 20k, ship 18k $\rightarrow$ tổng = 18k (voucher không trừ vào ship) | Unit / Backend | `TC-UNIT-PRICE-03`, `TC-INT-PRICING-01` | Sẵn sàng |
| **AC-04** | Voucher còn đúng 1 lượt, 2 khách đặt đồng thời $\rightarrow$ chỉ 1 đơn thành công | Integration | `TC-INT-CONCURRENCY-01` | Sẵn sàng |
| **AC-05** | Đơn mới tạo $\rightarrow$ thiết bị Quán phát chuông lặp trong $\le 5$ giây (kể cả khi chạy nền) | UI / E2E | `TC-E2E-NOTI-01` | Sẵn sàng |
| **AC-06** | Đơn đã thanh toán ZaloPay bị quán từ chối "Hết chả cá" $\rightarrow$ tự động hoàn tiền | Integration | `TC-INT-REFUND-01` | Sẵn sàng |
| **AC-07** | Bấm Gọi Shipper $\rightarrow$ khi gán xe, khách thấy Tên, SĐT, Biển số xe trong $\le 5$ giây | Integration | `TC-INT-LALA-WH-01`, `TC-E2E-TRACK-01` | Sẵn sàng |
| **AC-08** | Webhook cùng 1 `eventId` gửi lặp 3 lần $\rightarrow$ chỉ cập nhật đơn và bắn push 1 lần | Integration | `TC-INT-IDEMPOTENCY-01` | Sẵn sàng |
| **AC-09** | Quán chọn "Bận 30 phút" $\rightarrow$ khách không đặt được, thấy giờ mở lại; sau 30p tự mở | Unit / Integ | `TC-UNIT-STATUS-01`, `TC-INT-STORE-01` | Sẵn sàng |
| **AC-10** | "Đặt lại đơn này" khi có món đã ngừng bán $\rightarrow$ nạp món còn bán, bỏ món hết, báo banner | Unit / UI | `TC-UNIT-REORDER-01`, `TC-UI-REORDER-01` | Sẵn sàng |

---

## 3. Kế hoạch Unit Test (Kiểm thử Đơn vị - Domain Logic)

Các ca kiểm thử độc lập, không kết nối mạng, chạy trong $< 2$ giây, áp dụng chung bộ test vectors `shared/test-vectors/pricing.json`.

### 3.1 Nhóm `CartLineKeyTest` (Chuẩn hóa khóa dòng giỏ hàng)
* **`TC-UNIT-CLK-01` (Tách dòng khi khác ghi chú):**
  * *Input:* Món `item_bun_rieu`, size `opt_size_large`, topping `[opt_top_cha, opt_top_gio]`. Dòng A ghi chú `"Không hành"`, Dòng B ghi chú `"nhiều hành"`.
  * *Expected:* `CartLineKey(A) != CartLineKey(B)`. Giỏ hàng chứa 2 dòng độc lập.
* **`TC-UNIT-CLK-02` (Gộp dòng khi trùng nội dung chuẩn hóa):**
  * *Input:* Dòng A ghi chú `"  KHÔNG   hành  "`, Dòng B ghi chú `"không hành"`. Thứ tự mảng topping Dòng A là `[gio, cha]`, Dòng B là `[cha, gio]`.
  * *Expected:* `CartLineKey(A) == CartLineKey(B)`. Tự động cộng dồn số lượng `quantity`.

### 3.2 Nhóm `PricingCalculatorTest` (Công thức tính tiền & Voucher)
* **`TC-UNIT-PRICE-01` (Hóa đơn tiêu chuẩn PV-01):**
  * *Input:* 2 tô bún lớn (55k $\times 2$), topping chả (8k $\times 2$), 1 trà đá (5k). Voucher 10% trần 20k. Phí ship 18k.
  * *Expected:* `itemsTotal = 115.000đ`, `toppingsTotal = 16.000đ`, `subtotal = 131.000đ`, `discount = 13.100đ`, `shippingFee = 18.000đ`, `total = 135.900đ`.
* **`TC-UNIT-PRICE-02` (Chạm trần giảm giá maxDiscount PV-02):**
  * *Input:* `subtotal = 300.000đ`. Voucher 10% (lý thuyết giảm 30k) nhưng `maxDiscount = 20.000đ`. Ship 22k.
  * *Expected:* `discount = 20.000đ`, `total = 302.000đ`.
* **`TC-UNIT-PRICE-03` (Voucher không trừ vào ship PV-03):**
  * *Input:* `subtotal = 15.000đ`, voucher cố định giảm 20.000đ, ship 18.000đ.
  * *Expected:* `discount = 15.000đ` (bằng subtotal), `total = 18.000đ` (chính bằng tiền ship, hóa đơn không âm).
* **`TC-UNIT-PRICE-04` (Không đạt đơn tối thiểu PV-04):**
  * *Input:* `subtotal = 95.000đ`, voucher yêu cầu `minOrder = 100.000đ`.
  * *Expected:* Ném ngoại lệ / trả lỗi `VOUCHER_MIN_ORDER` kèm shortfall `5.000đ`.

### 3.3 Nhóm `OrderStateMachineTest` (Máy trạng thái đơn hàng)
* **`TC-UNIT-STATE-01` (Luồng chuyển dịch hợp lệ):**
  * Verify chuỗi chuyển dịch: `PENDING_PAYMENT` $\rightarrow$ `PLACED` $\rightarrow$ `CONFIRMED` $\rightarrow$ `READY` $\rightarrow$ `FINDING_DRIVER` $\rightarrow$ `DRIVER_ASSIGNED` $\rightarrow$ `ARRIVED_AT_STORE` $\rightarrow$ `PICKED_UP` $\rightarrow$ `DELIVERING` $\rightarrow$ `COMPLETED`.
* **`TC-UNIT-STATE-02` (Chặn đứng chuyển dịch trái phép):**
  * Thử chuyển từ `PLACED` trực tiếp sang `DELIVERING` hoặc từ `COMPLETED` sang `REJECTED` $\rightarrow$ ném lỗi `INVALID_TRANSITION`.

### 3.4 Nhóm `StoreStatusResolverTest` (Độ ưu tiên trạng thái quán)
* **`TC-UNIT-STATUS-01` (Thứ tự ưu tiên BR-STORE-01):**
  * Thiết lập: Đang trong khung giờ mở cửa theo lịch tuần, nhưng có `manualStatus = BUSY` và `busyUntil > now`.
  * *Expected:* `effectiveStatus == BUSY`. Nút đặt hàng bị vô hiệu hóa.

---

## 4. Kế hoạch Integration Test (Firebase Emulator Suite)

Chạy trên môi trường giả lập Firebase cục bộ (`firebase emulators:start`), kiểm tra tính toàn vẹn của dữ liệu và giao tiếp giữa các module.

### 4.1 Tranh chấp Voucher đồng thời (Concurrency Control)
* **`TC-INT-CONCURRENCY-01`:**
  * *Kịch bản:* Voucher `MIAO50` chỉ còn đúng 1 lượt dùng (`usedCount = 99`, `usageLimit = 100`).
  * *Hành động:* Khởi tạo đồng thời 2 tiến trình `placeOrder` từ 2 User khác nhau (`uid_1`, `uid_2`) trong cùng 1 mili-giây.
  * *Thẩm định:* Đúng 1 lệnh đặt đơn thành công, lệnh còn lại nhận lỗi `VOUCHER_EXHAUSTED`. `usedCount` trên Firestore phải bằng chính xác `100`.

### 4.2 Xử lý Idempotency (Chống lặp đơn & Webhook)
* **`TC-INT-IDEMPOTENCY-ORD` (Client bấm Đặt 2 lần):**
  * *Hành động:* Gửi 2 request `placeOrder` với cùng 1 `idempotencyKey = "uuid-1234"`.
  * *Thẩm định:* Hệ thống chỉ tạo đúng 1 document trong `orders`, cả 2 request đều trả về cùng 1 `orderId`.
* **`TC-INT-IDEMPOTENCY-WH` (Lalamove Webhook gửi lặp):**
  * *Hành động:* Bắn 3 request liên tiếp tới `/webhooks/lalamove` với cùng `eventId = "evt_driver_999"`.
  * *Thẩm định:* Chỉ 1 lần cập nhật trạng thái đơn sang `DRIVER_ASSIGNED`, collection `processedEvents` chỉ ghi 1 bản ghi, chỉ 1 thông báo push được phát đi.

### 4.3 Cổng thanh toán ZaloPay Sandbox & Hoàn tiền tự động
* **`TC-INT-ZALOPAY-CB`:**
  * Giả lập ZaloPay gọi POST `/payments/zalopay/callback` với payload hợp lệ và MAC HMAC-SHA256 chuẩn (key2) $\rightarrow$ Verify đơn chuyển sang `PLACED`, `payment.status = "PAID"`.
  * Giả lập Callback với MAC giả mạo / sai lệch số tiền $\rightarrow$ Verify trả mã lỗi `return_code: -1` (Invalid MAC).
* **`TC-INT-REFUND-01`:**
  * Tạo đơn đã trả qua ZaloPay. Gọi callable `rejectOrder(orderId, "OUT_OF_STOCK", "Hết chả cá")`.
  * *Thẩm định:* `orders/{id}.status` chuyển `REJECTED`, `refundStatus` chuyển `PENDING` $\rightarrow$ `REFUNDED` (gọi ZaloPay Refund API `/v2/refund`), lượt voucher được hoàn lại.

---

## 5. Kế hoạch UI & Instrumental Test (Jetpack Compose UI)

Kiểm thử giao diện người dùng trên máy ảo / máy thật Android bằng `ComposeTestRule`.

```kotlin
// Ví dụ cấu trúc test case Compose cho Giỏ hàng
@Test
fun cart_displays_separated_lines_for_different_notes() {
    composeTestRule.setContent {
        MiaoTheme { CartScreen(uiState = sampleStateWithTwoDifferentNoteLines) }
    }
    // Verify 2 dòng riêng biệt xuất hiện
    composeTestRule.onNodeWithText("không hành").assertIsDisplayed()
    composeTestRule.onNodeWithText("nhiều hành").assertIsDisplayed()
    composeTestRule.onAllNodesWithText("Bún riêu").assertCountEquals(2)
}
```

### Danh mục UI Test Cases:
* **`TC-UI-CART-01`:** Thêm 2 món cùng tên khác ghi chú $\rightarrow$ Màn hình giỏ hàng hiển thị 2 thẻ riêng biệt, mỗi thẻ có số lượng là 1.
* **`TC-UI-KDS-01`:** Màn hình Bếp hiển thị vé đơn $\rightarrow$ Kiểm tra các từ khóa "dị ứng", "không ăn được" có hiển thị trên huy hiệu màu đỏ (`#C8372D`) và chữ to $\ge 20\text{sp}$ hay không.
* **`TC-UI-CHAT-01`:** Màn hình Nhắn tin In-App $\rightarrow$ Nhập tin nhắn "Cho xin thêm ớt" và bấm nút gửi $\rightarrow$ Tin nhắn xuất hiện ngay trên bong bóng chat bên phải và danh sách tự cuộn xuống đáy.

---

## 6. Kế hoạch Kiểm thử Bảo mật & Ràng buộc Dữ liệu (Security Rules)

Sử dụng thư viện `@firebase/rules-unit-testing` để chứng minh hệ thống an toàn trước mọi hành vi can thiệp trái phép:

* **`TC-SEC-RULES-01` (Khách không được xem đơn người khác):**
  * Client `uid_A` cố gắng đọc `orders/ord_cua_uid_B` $\rightarrow$ Bị từ chối `PERMISSION_DENIED`.
* **`TC-SEC-RULES-02` (Client tuyệt đối không được ghi vào orders):**
  * Bất kỳ client nào (`CUSTOMER` hay `MERCHANT`) cố gắng gọi `set()`, `update()`, `delete()` trực tiếp vào `orders` $\rightarrow$ Bị Security Rules chặn 100%. Mọi thao tác bắt buộc thông qua Cloud Functions.
* **`TC-SEC-RULES-03` (Bảo vệ thông tin tài khoản):**
  * Client cố gắng cập nhật trường `role = "MERCHANT"` hoặc `storeId` trên document `users/{uid}` $\rightarrow$ Bị từ chối.
* **`TC-SEC-SECRET-01` (Kiểm tra rò rỉ khóa bí mật):**
  * Quét decompile file APK release bằng `apktool` / `ripgrep`: Đảm bảo không chứa bất kỳ secret key nào của Lalamove (`secret`) hoặc ZaloPay (`key1`, `key2`).

---

## 7. Kịch bản Kiểm thử Đầu-cuối (End-to-End Demo Script trên 2 máy thật)

Kịch bản tổng duyệt nghiệm thu trước hội đồng bằng 2 điện thoại thật kết nối `scrcpy` lên màn hình chiếu:

### 📱 Thiết bị tham gia:
* **Máy 1 (Khách hàng):** Cài đặt MiaoSong, đăng nhập tài khoản Khách (`CUSTOMER`).
* **Máy 2 (Chủ quán):** Cài đặt MiaoSong, đăng nhập tài khoản Quán (`MERCHANT`).
* **Laptop điều khiển:** Chạy `scrcpy` chiếu song song 2 màn hình, mở tab Mock Provider Simulator (đẩy trạng thái tài xế nếu cần).

### Kịch bản 1: Luồng Đặt món Chuẩn & Giao hàng Thành công (Happy Path)
1. **[Máy 1]** Khách vào quán Bún Riêu Cô Ba, chọn 1 tô lớn "Không hành" + 1 tô lớn "Nhiều hành" + 1 Trà đá.
2. **[Máy 1]** Vào giỏ: Chứng minh giỏ hàng tách thành 2 dòng riêng biệt. Chọn địa chỉ trường ĐH Tôn Đức Thắng (OpenStreetMap) $\rightarrow$ Báo phí ship tự động 18.000đ (3,2 km).
3. **[Máy 1]** Nhập mã `MIAO10` $\rightarrow$ Giảm giá 13.100đ (chứng minh không trừ vào phí ship). Chọn thanh toán **ZaloPay Sandbox** $\rightarrow$ Bấm "Đặt đơn".
4. **[Máy 1]** Mở Chrome Custom Tab / ZaloPay App, chọn tài khoản sandbox $\rightarrow$ Thanh toán thành công, tự động quay về app qua App Link.
5. **[Máy 2]** Ngay lập tức chuông báo đơn mới reo lặp trên máy quán. Mở đơn, xem ghi chú đặc biệt $\rightarrow$ Bấm **"Xác nhận nhận đơn"**.
6. **[Máy 1]** Nhận push notification: *"Quán đang chuẩn bị món 🍲"*.
7. **[Máy 2]** Chuyển sang tab Màn hình Bếp (KDS) $\rightarrow$ Xem vé chế biến $\rightarrow$ Bấm **"Đã đóng gói"** $\rightarrow$ Chuyển tab sang Gọi Shipper $\rightarrow$ Bấm **"Gọi Shipper"**.
8. **[Máy 1]** Khách theo dõi tiến độ đơn và lộ trình tài xế trực tiếp trên bản đồ **OSMDroid + OSRM** (kèm nút gọi tài xế trực tiếp khi cần).
9. **[Laptop/Webhook]** Lalamove gán tài xế $\rightarrow$ Máy 1 rung chuông, hiện ngay Thẻ tài xế (Tên, SĐT, Biển số xe).
10. **[Hệ thống]** Trạng thái chuyến chuyển sang `COMPLETED` $\rightarrow$ Máy 1 hiện hộp thoại đánh giá $\rightarrow$ Khách chấm 5 sao kèm nhận xét $\rightarrow$ Vào Lịch sử bấm **"Đặt lại đơn này"**.

### Kịch bản 2: Luồng Quán từ chối đơn & Hoàn tiền ZaloPay tự động (Exception Path)
1. **[Máy 1]** Đặt đơn mới và thanh toán trực tuyến thành công qua ZaloPay Sandbox.
2. **[Máy 2]** Quán nhận đơn, bấm **"Từ chối đơn"** $\rightarrow$ Chọn lý do *"Hết nguyên liệu: Chả cá"*.
3. **[Hệ thống]** Server ghi nhận `REJECTED`, tự động kích hoạt gọi API hoàn tiền sang ZaloPay.
4. **[Máy 1]** Nhận thông báo: *"Quán xin lỗi, đơn bị từ chối vì hết chả cá. Tiền thanh toán đã được hoàn về tài khoản của bạn."*

---

## 8. Tiêu chuẩn Hoàn thành Kiểm thử (Definition of Done - DoD)

Một tính năng được coi là hoàn tất chỉ khi đáp ứng đầy đủ các tiêu chuẩn kiểm thử sau:
1. ✅ **100% Tiêu chí nghiệm thu (AC-01 $\rightarrow$ AC-10)** đều có test case tự động bao phủ và vượt qua (PASS).
2. ✅ **Độ bao phủ kiểm thử (Code Coverage):** Logic nghiệp vụ tầng Domain (`core:domain` trên Android và `modules/*/domain` trên Cloud Functions) đạt độ bao phủ $\ge 75\%$.
3. ✅ **Firebase Emulator Suite:** Chạy thành công toàn bộ kịch bản tranh chấp Concurrency và Idempotency mà không phát sinh lỗi race condition.
4. ✅ **Bảo mật:** Firestore Security Rules vượt qua 100% test case trong bộ test `@firebase/rules-unit-testing`.
5. ✅ **Trải nghiệm thực tế:** Kịch bản demo trên 2 điện thoại thật chạy trơn tru, không có hiện tượng văng ứng dụng (ANR/Crash), âm thanh chuông báo và rung thông báo hoạt động chính xác.

---

## 9. Kết quả Kiểm thử Thực tế đã Thực thi (`v1.0.0 — Feature Freeze`)

Để phân biệt rõ giữa **kế hoạch kiểm thử mục tiêu** (Mục 1–8) và **phạm vi đã chạy xác minh thực tế** trên phiên bản `v1.0.0`:

| Hạng mục kiểm thử | Lệnh / Môi trường thực thi | Kết quả thực tế |
|---|---|---|
| **Backend Unit Tests & Shared Pricing Vectors** | `npm test` (`backend/functions`, dùng chung `shared/test-vectors/pricing.json`) | ✅ **PASS 12/12** |
| **Kiểm tra Kiến trúc Modular Monolith & TypeScript** | `npm run lint` (`dependency-cruiser` 4 luật R1–R4 + `tsc --noEmit`) | ✅ **PASS (0 vi phạm)** |
| **Android Kotlin Domain Unit Test** | `./gradlew :core:domain:test` (`SharedPricingVectorsTest`) | ✅ **PASS** |
| **Kiểm thử E2E thực tế (2 máy / 2 chế độ)** | Kịch bản 1 (Happy Path + OSMDroid/OSRM) & Kịch bản 2 (ZaloPay Sandbox + Quán từ chối & Hoàn tiền tự động) | ✅ **PASS** |
| **Đóng gói phát hành Google Play** | `./gradlew :app:bundleRelease` (R8/ProGuard & ShrinkResources) + Deploy 15 Cloud Functions live (`asia-southeast1`) | ✅ **PASS (`app-release.aab`)** |
| **Emulator Integration & Compose UI Automation** | Firebase Emulator Suite / `createComposeRule` | ⏳ *Đã thiết kế test case chi tiết (Mục 4–6), ưu tiên kiểm chứng trực tiếp bằng E2E trên máy thật và Backend Live trong khung thời gian 12 ngày* |
