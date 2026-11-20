# SRS — MiaoSong: Nền tảng đặt món & điều phối giao hàng tự động

| | |
|---|---|
| Phiên bản | 1.0 (Final) — 20/11/2026 |
| Chuẩn tham chiếu | ISO/IEC/IEEE 29148:2018 |
| Sản phẩm | 1 app **MiaoSong** gồm Chế độ Khách và Chế độ Quán (phân quyền theo role) · Backend Firebase |

---

## 1. Giới thiệu

### 1.1 Mục đích
Đặc tả đầy đủ yêu cầu chức năng và phi chức năng cho hệ thống cho phép quán ăn nhận đơn trực tiếp từ khách (D2C) và tự động điều phối giao hàng qua Lalamove, loại bỏ việc nhập đơn thủ công.

### 1.2 Phạm vi
- **Trong phạm vi:** quản lý menu & tùy chọn, voucher, giỏ hàng, báo giá ship tự động, đặt & thanh toán (COD, VNPay sandbox), xử lý đơn tại quán, màn hình bếp, trạng thái/lịch quán, gọi shipper tự động, đồng bộ tài xế, push notification, đánh giá, đặt lại.
- **Ngoài phạm vi:** chat in-app, web admin, iOS, quản lý tài xế riêng, kế toán/hóa đơn VAT.

### 1.3 Thuật ngữ
| Thuật ngữ | Định nghĩa |
|---|---|
| D2C | Direct-to-Consumer: quán bán trực tiếp cho khách, không qua sàn trung gian |
| Option Group | Nhóm tùy chọn của món. `SINGLE` = chọn một, `MULTI` = chọn nhiều |
| Cart Line | Một dòng giỏ hàng = 1 món + 1 cấu hình option + 1 ghi chú duy nhất |
| Quote | Báo giá cước từ đối tác vận chuyển, có thời hạn hiệu lực |
| COD | Thu hộ tiền mặt khi giao |
| KDS | Kitchen Display Screen — màn hình bếp |
| Webhook | Lời gọi HTTP đối tác gửi về khi trạng thái chuyến đổi |
| IPN | Instant Payment Notification từ cổng thanh toán |

---

## 2. Mô tả tổng quan

### 2.1 Bối cảnh sản phẩm
Hệ thống gồm 1 ứng dụng Android native (Kotlin/Jetpack Compose) với 2 chế độ theo vai trò (Khách Mua / Quán Bán), dùng chung backend Firebase (Auth, Firestore, Cloud Functions, FCM, Storage). Backend là modular monolith trên Cloud Functions, chia module catalog, pricing, orders, delivery, payment, notification; là lớp duy nhất giao tiếp với Lalamove, VNPay.

### 2.2 Nhóm chức năng chính
1. Quản lý thực đơn & khuyến mãi (Chủ quán)
2. Trải nghiệm khách hàng & đặt hàng
3. Xử lý đơn hàng tại quán
4. Điều phối vận chuyển tự động
5. Theo dõi lộ trình & chăm sóc sau mua

### 2.3 Lớp người dùng
| Actor | Mô tả | Mức thành thạo kỹ thuật |
|---|---|---|
| Khách hàng | Người đặt món, 16–45 tuổi, dùng smartphone hằng ngày | Trung bình |
| Quán (MERCHANT) | Đại diện phía quán ăn (chủ quán, thu ngân, bếp cùng vận hành trên thiết bị của quán); thao tác nhanh giờ cao điểm | Thấp–Trung bình |
| Đối tác vận chuyển | Hệ thống Lalamove (external) | — |
| Cổng thanh toán | VNPay (external) | — |
| Dịch vụ bản đồ | Google Maps API (Places, Geocoding, Maps SDK) | — |
| FCM | Dịch vụ push (external) | — |
| Bộ lập lịch | Tác nhân thời gian (Cloud Scheduler) | — |

### 2.4 Môi trường vận hành
Android 8.0 (API 26) trở lên, targetSdk 36; cần Internet; phân phối qua Google Play (Internal testing). Backend region `asia-southeast1`.

### 2.5 Ràng buộc
- C-01: Thời gian phát triển 12 ngày, 2 thành viên.
- C-02: Bắt buộc Android native (Kotlin).
- C-03: Chỉ dùng môi trường sandbox của đối tác vận chuyển và thanh toán.
- C-04: Không lưu khóa bí mật của bên thứ ba trong ứng dụng di động.
- C-05: Tiền tệ hệ thống duy nhất VND, số nguyên (`Long` / integer).

### 2.6 Giả định & phụ thuộc
- A-01: Đối tác cấp được tài khoản sandbox; nếu không, dùng MockProvider cùng interface.
- A-02: Mỗi tài khoản chủ quán quản lý đúng 1 quán.
- A-03: Khu vực phục vụ: TP.HCM / Hà Nội (khu vực đối tác hỗ trợ).

---

## 3. Quy mô dự án

### 3.1 Danh sách Use Case

| ID | Tên | Actor | Ưu tiên |
|---|---|---|---|
| UC01 | Đăng nhập / Đăng ký | Khách | Must |
| UC02 | Quản lý địa chỉ giao hàng | Khách, Bản đồ | Must |
| UC03 | Xem quán (danh sách / deep link) | Khách | Must |
| UC04 | Xem thực đơn theo nhóm | Khách | Must |
| UC05 | Tùy chỉnh món | Khách | Must |
| UC06 | Quản lý giỏ hàng | Khách | Must |
| UC07 | Áp dụng voucher | Khách | Must |
| UC08 | Xem báo giá vận chuyển | Khách, Đối tác VC | Must |
| UC09 | Đặt hàng & thanh toán | Khách, VNPay | Must |
| UC10 | Theo dõi đơn & thông tin tài xế | Khách | Must |
| UC11 | Nhận push notification | Khách, FCM | Must |
| UC12 | Đánh giá đơn | Khách | Must |
| UC13 | Xem lịch sử đơn | Khách | Must |
| UC14 | Đặt lại đơn | Khách | Must |
| UC15 | Hủy đơn (trước khi quán xác nhận) | Khách | Should |
| UC20 | Đăng nhập quán | Quán | Must |
| UC21 | Quản lý nhóm món | Quán | Must |
| UC22 | Quản lý món & nhóm tùy chọn | Quán | Must |
| UC23 | Bật/tắt món hoặc option hết hàng | Quán | Must |
| UC24 | Quản lý voucher | Quán | Must |
| UC25 | Nhận đơn mới (chuông) | Quán, FCM | Must |
| UC26 | Xác nhận đơn | Quán | Must |
| UC27 | Từ chối đơn & hoàn tiền | Quán, VNPay | Must |
| UC28 | Xem màn bếp (KDS) | Quán | Must |
| UC29 | Đánh dấu đã đóng gói | Quán | Must |
| UC30 | Gọi shipper | Quán, Lalamove | Must |
| UC31 | Theo dõi giao hàng & cảnh báo trễ | Quán | Must |
| UC32 | Đổi trạng thái hoạt động | Quán | Must |
| UC33 | Cài lịch hoạt động thường ngày & ngày đặc biệt | Quán | Must |
| UC34 | Xem đánh giá | Quán | Should |
| UC35 | Thống kê doanh thu ngày | Quán | Could |
| UC40 | Đồng bộ trạng thái tài xế (webhook) | Đối tác VC | Must |
| UC41 | Xác nhận thanh toán (VNPay IPN) | VNPay | Must |
| UC42 | Phát hiện đơn trễ | Bộ lập lịch | Should |
| UC43 | Hủy đơn chưa thanh toán quá hạn | Bộ lập lịch | Should |
| UC44 | Gửi push theo trạng thái | Hệ thống, FCM | Must |

---

## 4. Đặc tả Use Case chi tiết

### UC05 — Tùy chỉnh món
| Mục | Nội dung |
|---|---|
| Actor | Khách hàng |
| Tiền điều kiện | Khách đang ở trang quán; món `isAvailable = true` |
| Hậu điều kiện | Một Cart Line được thêm hoặc cộng dồn vào giỏ |
| Luồng chính | 1. Khách chạm vào món. 2. Hệ thống mở bottom sheet: ảnh, mô tả, các Option Group. 3. Khách chọn 1 option trong nhóm `SINGLE` bắt buộc (VD Tô lớn +10.000đ). 4. Khách chọn 0..n option trong nhóm `MULTI` (Thêm chả, Thêm giò). 5. Khách nhập ghi chú (≤ 200 ký tự), chọn số lượng. 6. Hệ thống cập nhật giá realtime = (giá size + Σ topping) × SL. 7. Khách bấm "Thêm vào giỏ – 85.000đ". 8. Hệ thống tạo CartLineKey; nếu trùng dòng có sẵn → cộng SL, ngược lại → thêm dòng mới. |
| Luồng thay thế | 3a. Chưa chọn nhóm bắt buộc → nút Thêm bị vô hiệu, nhóm hiện viền đỏ "Bắt buộc chọn 1". 4a. Vượt `max` của nhóm → không cho tích thêm, hiện "Tối đa 3 món thêm". |
| Ngoại lệ | E1. Giỏ đang chứa món của quán khác → hộp thoại "Giỏ hàng chỉ chứa món của 1 quán. Xóa giỏ cũ?" |
| Business rule | BR-MENU-02, BR-CART-01, BR-CART-02 |

### UC06 — Quản lý giỏ hàng
| Mục | Nội dung |
|---|---|
| Actor | Khách hàng |
| Luồng chính | 1. Khách mở giỏ. 2. Hệ thống hiện từng Cart Line: tên món, size, topping, ghi chú, đơn giá, SL, thành tiền. 3. Khách tăng/giảm SL, sửa cấu hình (mở lại bottom sheet), hoặc xóa dòng. 4. Hệ thống tính lại subtotal. |
| Luồng thay thế | 3a. Sửa cấu hình làm trùng key với dòng khác → gộp 2 dòng. 3b. Giảm SL về 0 → hỏi xác nhận xóa. |
| Ngoại lệ | E1. Món trong giỏ bị quán tắt (hết hàng) → dòng mờ đi, nhãn "Hết món", không cho checkout tới khi xóa. |
| Ví dụ kiểm thử | 2 tô bún riêu thường: 1 ghi chú "không hành", 1 ghi chú "nhiều hành" → **2 dòng, mỗi dòng SL 1**. |

### UC07 — Áp dụng voucher
| Mục | Nội dung |
|---|---|
| Tiền điều kiện | Giỏ có hàng, đang ở Checkout |
| Luồng chính | 1. Khách nhập mã hoặc chọn từ danh sách voucher khả dụng của quán. 2. App gọi `validateVoucher(storeId, code, subtotal)`. 3. Server kiểm tra lần lượt: tồn tại & active → trong thời hạn → subtotal ≥ minOrder → usedCount < usageLimit → số lần khách đã dùng < perUserLimit. 4. Trả về discount. 5. App hiển thị dòng "Giảm giá −15.000đ". |
| Luồng thay thế | 3a. Không đạt điều kiện → trả mã lỗi cụ thể; app hiện "Đơn tối thiểu 100.000đ, thêm 12.000đ nữa nhé". 3b. Subtotal thay đổi sau khi áp → validate lại tự động; không còn hợp lệ → gỡ voucher và báo. |
| Hậu điều kiện | Voucher chỉ được **giữ chỗ khi placeOrder thành công** (tăng usedCount trong transaction) |
| Business rule | BR-VOU-01 → BR-VOU-07 |

### UC08 — Xem báo giá vận chuyển
| Mục | Nội dung |
|---|---|
| Luồng chính | 1. Khách chọn/nhập địa chỉ (Google Maps Places Autocomplete). 2. Hệ thống geocode ra lat/lng. 3. App gọi `getDeliveryQuote(storeId, dropoff)`. 4. Server gọi provider của quán, trả `fee, distanceKm, quotationId, expiresAt`. 5. App hiển thị "Phí giao hàng 18.000đ · 3,2 km" trước nút Đặt. |
| Luồng thay thế | 4a. Ngoài bán kính phục vụ (> maxDistanceKm của quán) → "Quán chưa giao tới địa chỉ này". 4b. Lalamove lỗi/timeout 8s → thử lại 1 lần; vẫn lỗi → "Chưa lấy được phí ship, thử lại" + nút thử lại; **không cho đặt khi chưa có phí**. 4c. Quote hết hạn → tự báo giá lại khi khách bấm Đặt. |

### UC09 — Đặt hàng & thanh toán
| Mục | Nội dung |
|---|---|
| Tiền điều kiện | Giỏ hợp lệ, có địa chỉ, có quote còn hạn, quán đang `OPEN` |
| Luồng chính | 1. Khách xem bảng tiền: Tiền món · Tiền món thêm · Giảm giá · Phí ship · **Tổng**. 2. Chọn phương thức (COD / VNPay). 3. Bấm "Đặt đơn". 4. App gửi `placeOrder` kèm `idempotencyKey`. 5. Server: kiểm tra quán mở → validate từng món/option còn bán & giá → tính lại tiền → transaction giữ voucher → tạo đơn. 6a. COD → trạng thái `PLACED`, gửi chuông cho quán. 6b. VNPay → `PENDING_PAYMENT`, trả URL thanh toán đã ký; app mở Custom Tab; IPN hợp lệ → `PLACED`. 7. App chuyển sang màn Theo dõi đơn. |
| Luồng thay thế | 5a. Giá server khác giá app (menu vừa đổi) → trả `PRICE_CHANGED` kèm giá mới, app hiện để khách xác nhận lại. 5b. Voucher hết lượt trong lúc đặt → `VOUCHER_EXHAUSTED`, đơn không được tạo. 6b-1. Thanh toán thất bại/hủy → đơn ở `PENDING_PAYMENT`, cho thanh toán lại trong 15 phút. |
| Ngoại lệ | Bấm đặt 2 lần / mạng chập chờn → cùng idempotencyKey trả về cùng 1 đơn. |

### UC25 + UC26 — Nhận & xác nhận đơn
| Mục | Nội dung |
|---|---|
| Actor | Quán |
| Trigger | Đơn chuyển sang `PLACED` |
| Luồng chính | 1. Hệ thống gửi FCM high-priority tới mọi thiết bị của quán; app phát chuông lặp, hiện thẻ đơn ở tab "Mới". 2. Chủ quán mở đơn: danh sách món, option, **ghi chú đặc biệt nổi bật**, tổng tiền, phương thức thanh toán. 3. Bấm "Xác nhận nhận đơn". 4. Server chuyển `CONFIRMED`, ghi `statusHistory`. 5. Khách nhận push "Quán đang chuẩn bị món 🍲". 6. Đơn xuất hiện trên màn Bếp. |
| Luồng thay thế | 3a. Không phản hồi sau 5 phút → nhắc lại chuông; sau 10 phút → tự động từ chối với lý do "Quán không phản hồi" (Should). |

### UC27 — Từ chối đơn & hoàn tiền
| Mục | Nội dung |
|---|---|
| Tiền điều kiện | Đơn ở `PLACED` hoặc `CONFIRMED` (chưa gọi shipper) |
| Luồng chính | 1. Chủ quán bấm "Từ chối đơn". 2. Chọn lý do nhanh: Hết nguyên liệu (chọn món/nguyên liệu, VD "Hết chả cá") · Quán quá tải · Sắp đóng cửa · Khác (nhập). 3. Xác nhận. 4. Server chuyển `REJECTED`, hoàn lượt voucher. 5. Nếu đã thanh toán online → gọi API hoàn tiền VNPay (`merchant_webapi` refund), `refundStatus = PENDING` → `REFUNDED`. 6. Push cho khách: "Quán xin lỗi, đơn bị từ chối vì hết chả cá. Tiền sẽ hoàn về trong 1–3 ngày." |
| Luồng thay thế | 2a. Chọn "Hết nguyên liệu" → gợi ý tắt luôn món liên quan (UC23). 5a. Refund lỗi → `refundStatus = FAILED`, hiển thị cảnh báo cho chủ quán để xử lý thủ công. |

### UC28 — Màn bếp (KDS)
| Mục | Nội dung |
|---|---|
| Luồng chính | 1. Quán mở tab Chế độ Bếp (KDS). 2. Hệ thống hiển thị ticket các đơn `CONFIRMED` theo thứ tự cũ → mới, mỗi ticket có đồng hồ đếm phút. 3. Mỗi món hiện SL lớn, size, topping. 4. **Ghi chú** hiện trên nền vàng + icon ⚠️; từ khóa dị ứng (dị ứng, không đậu phộng, không tôm...) hiện nền đỏ + icon 🚫 và nằm đầu ticket. 5. Quán bấm "Đã đóng gói" → `READY` (UC29). |
| NFR riêng | Chữ món ≥ 20sp, nút ≥ 64dp, giữ màn hình sáng (`keepScreenOn`). |

### UC30 — Gọi shipper
| Mục | Nội dung |
|---|---|
| Tiền điều kiện | Đơn `READY` |
| Luồng chính | 1. Chủ quán bấm "Gọi Shipper". 2. Server lấy/làm mới quote. 3. Gửi sang provider: địa chỉ & SĐT quán, địa chỉ & SĐT khách, **số tiền thu hộ** (= total nếu COD, 0 nếu đã trả online), ghi chú giao hàng, danh sách món tóm tắt. 4. Lưu `externalOrderId`, chuyển `FINDING_DRIVER`. 5. Khi có webhook tài xế → UC40. |
| Luồng thay thế | 3a. Lalamove từ chối/lỗi → giữ `READY`, báo lỗi và nút thử lại (dev: chuyển MockProvider). 4a. Quá 10 phút chưa có tài xế → cảnh báo, cho hủy chuyến và gọi lại. |

### UC40 — Đồng bộ trạng thái tài xế (webhook)
| Mục | Nội dung |
|---|---|
| Actor | Đối tác vận chuyển |
| Luồng chính | 1. Provider POST webhook. 2. Server xác thực chữ ký/secret. 3. Kiểm tra `eventId` đã xử lý chưa (idempotency). 4. Adapter chuẩn hóa về `DeliveryEvent` nội bộ. 5. State machine kiểm tra chuyển trạng thái hợp lệ. 6. Cập nhật đơn: `driver{name, phone, plate}`, status. 7. Kích hoạt UC44 gửi push cho khách và cập nhật app quán. 8. Trả 200. |
| Ánh xạ trạng thái | ASSIGNING_DRIVER → FINDING_DRIVER · ON_GOING/ACCEPTED → DRIVER_ASSIGNED · arrived pickup → ARRIVED_AT_STORE · PICKED_UP → PICKED_UP/DELIVERING · COMPLETED → COMPLETED · CANCELED/EXPIRED/REJECTED → READY (cho gọi lại) hoặc DELIVERY_FAILED |
| Ngoại lệ | Chữ ký sai → 401. Sự kiện cũ đến sau (out-of-order) → bỏ qua nếu không hợp lệ theo state machine, vẫn trả 200. |

### UC14 — Đặt lại đơn
| Mục | Nội dung |
|---|---|
| Tiền điều kiện | Đơn ở `COMPLETED` |
| Luồng chính | 1. Khách mở Lịch sử → chọn đơn → "Đặt lại đơn này". 2. Hệ thống đối chiếu từng dòng với menu hiện tại. 3. Dòng hợp lệ được đưa vào giỏ với đúng size/topping/ghi chú/SL. 4. Mở giỏ hàng. |
| Luồng thay thế | 2a. Món/option đã ngừng bán → bỏ qua và liệt kê "1 món không còn bán". 2b. Giá thay đổi → dùng giá mới, hiện nhãn "Giá đã cập nhật". 2c. Quán đang đóng/bận → vẫn vào giỏ nhưng báo giờ mở cửa. 2d. Giỏ đang có món quán khác → hỏi thay thế. |

### UC12 — Đánh giá đơn
| Mục | Nội dung |
|---|---|
| Tiền điều kiện | Đơn `COMPLETED`, chưa đánh giá, trong vòng 7 ngày |
| Luồng chính | 1. Khi đơn hoàn thành, app hiện bottom sheet đánh giá (hoặc từ Lịch sử). 2. Khách chọn 1–5 sao (bắt buộc), chip nhanh ("Nóng hổi", "Đóng gói kỹ", "Đậm đà"), nhận xét ≤ 500 ký tự (tùy chọn). 3. Gửi. 4. Server lưu review, cập nhật `ratingAvg`, `ratingCount` của quán (transaction). |

### UC32 + UC33 — Trạng thái & lịch hoạt động
| Mục | Nội dung |
|---|---|
| Luồng chính | 1. Chủ quán chọn: Đang mở cửa · Quán bận (15 phút / 30 phút / 1 tiếng / 1 ngày / tùy chọn) · Đóng cửa. 2. Bận → lưu `busyUntil = now + duration`, tự hết khi qua thời điểm. 3. Lịch tuần: mỗi thứ có nhiều khung giờ (VD 6:00–10:00, 16:00–21:00). 4. Ngày đặc biệt: đóng cả ngày (Tết) hoặc giờ riêng. |
| Quy tắc ưu tiên | BR-STORE-01 |

---

## 5. Yêu cầu chức năng (Functional Requirements)

### 5.1 Quản lý thực đơn & khuyến mãi
| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| FR-MENU-01 | Chủ quán tạo/sửa/xóa/sắp xếp nhóm món (Món chính, Món thêm, Thức uống...) | Must |
| FR-MENU-02 | Chủ quán tạo/sửa/xóa món: tên, mô tả, ảnh, giá cơ bản, nhóm | Must |
| FR-MENU-03 | Mỗi món có 0..n Option Group, kiểu `SINGLE` (bắt buộc/không) hoặc `MULTI` với `min`/`max` | Must |
| FR-MENU-04 | Mỗi option có tên và giá chênh lệch (≥ 0) | Must |
| FR-MENU-05 | Bật/tắt trạng thái còn hàng cho món và cho từng option, cập nhật realtime tới khách | Must |
| FR-VOU-01 | Tạo voucher loại phần trăm (có trần giảm tối đa) hoặc số tiền cố định | Must |
| FR-VOU-02 | Thiết lập điều kiện: đơn tối thiểu, ngày bắt đầu/kết thúc, tổng số lượt, số lượt mỗi khách | Must |
| FR-VOU-03 | Bật/tắt voucher, xem số lượt đã dùng | Must |

### 5.2 Khách hàng & đặt hàng
| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| FR-AUTH-01 | Đăng nhập Google hoặc Email/mật khẩu; bắt buộc cập nhật SĐT trước khi đặt đơn đầu tiên | Must |
| FR-ADDR-01 | Thêm/sửa/xóa địa chỉ với autocomplete, ghim vị trí trên bản đồ, đặt mặc định | Must |
| FR-STORE-01 | Hiển thị danh sách quán đối tác, trạng thái mở/bận/đóng, điểm đánh giá | Must |
| FR-STORE-02 | Mở thẳng trang quán qua deep link `https://miaosong.web.app/s/{slug}` | Should |
| FR-CART-01 | Thêm món với cấu hình; tách dòng theo CartLineKey | Must |
| FR-CART-02 | Sửa SL, sửa cấu hình, xóa dòng; giỏ lưu cục bộ, còn sau khi tắt app | Must |
| FR-CART-03 | Giỏ chỉ chứa món của một quán | Must |
| FR-SHIP-01 | Tự động lấy báo giá ship theo khoảng cách thực tế khi chọn địa chỉ | Must |
| FR-SHIP-02 | Hiển thị phí ship và khoảng cách trước khi xác nhận đặt | Must |
| FR-PAY-01 | Hỗ trợ COD | Must |
| FR-PAY-02 | Hỗ trợ VNPay sandbox: tạo URL thanh toán, nhận IPN, hoàn tiền | Must |
| FR-ORD-01 | Hiển thị bảng tiền: tiền món, tiền món thêm, giảm giá, phí ship, tổng | Must |
| FR-ORD-02 | Server tính lại toàn bộ tiền khi đặt; từ chối nếu lệch | Must |
| FR-ORD-03 | Chống tạo đơn trùng bằng idempotency key | Must |
| FR-ORD-04 | Khách hủy được đơn khi còn `PLACED`/`PENDING_PAYMENT` | Should |

### 5.3 Xử lý đơn tại quán
| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| FR-MER-01 | Phát chuông lặp trên thiết bị quán khi có đơn mới, kể cả khi app ở nền | Must |
| FR-MER-02 | Xác nhận đơn → khách nhận thông báo "Quán đang chuẩn bị món" | Must |
| FR-MER-03 | Từ chối đơn với lý do; tự động hoàn tiền nếu đã thanh toán online; thông báo lý do cho khách | Must |
| FR-KDS-01 | Màn bếp hiển thị ticket, làm nổi bật ghi chú đặc biệt & dị ứng | Must |
| FR-KDS-02 | Đánh dấu "Đã đóng gói" | Must |
| FR-OPS-01 | Đổi trạng thái: mở / bận (15', 30', 1h, 1 ngày, tùy chọn) / đóng | Must |
| FR-OPS-02 | Cài lịch tuần nhiều khung giờ và ngày đặc biệt | Must |
| FR-OPS-03 | Quán không mở → khách không đặt được, thấy giờ mở cửa kế tiếp | Must |

### 5.4 Điều phối vận chuyển
| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| FR-DSP-01 | Nút "Gọi Shipper" gửi đủ: địa chỉ quán, địa chỉ khách, SĐT, tiền thu hộ, ghi chú | Must |
| FR-DSP-02 | Nhận & lưu tên, SĐT, biển số tài xế qua webhook; hiển thị ngay cho khách | Must |
| FR-DSP-03 | Tích hợp Lalamove sandbox + MockProvider (giả lập tài xế cho demo), thiết kế interface để thêm provider khác | Must |
| FR-DSP-04 | Hủy chuyến và gọi lại khi không tìm được tài xế | Should |

### 5.5 Theo dõi & sau mua
| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| FR-TRK-01 | Timeline trạng thái đơn realtime cho khách và quán | Must |
| FR-TRK-02 | Push (rung) mỗi khi trạng thái giao hàng thay đổi | Must |
| FR-TRK-03 | Cảnh báo quán khi đơn giao quá ETA + 15 phút | Should |
| FR-RV-01 | Đánh giá 1–5 sao + nhận xét sau khi giao thành công | Must |
| FR-HIS-01 | Lịch sử đơn có lọc theo trạng thái | Must |
| FR-RE-01 | "Đặt lại đơn này" khôi phục đúng món + cấu hình + ghi chú, có kiểm tra menu hiện tại | Must |

---

## 6. Business Rules

| ID | Quy tắc |
|---|---|
| BR-MENU-01 | Giá và giá chênh lệch là số nguyên VND ≥ 0 |
| BR-MENU-02 | Nhóm `SINGLE` bắt buộc phải chọn đúng 1; nhóm `MULTI` chọn trong [min, max] |
| BR-CART-01 | Hai lựa chọn chỉ gộp dòng khi trùng hoàn toàn món, option và ghi chú (ghi chú so sánh sau khi trim, chuẩn hóa khoảng trắng, không phân biệt hoa thường) |
| BR-CART-02 | Đơn giá dòng = giá option size (hoặc giá cơ bản nếu không có size) + Σ giá topping |
| BR-PRICE-01 | Tổng = (Tiền món + Tiền món thêm) − Giảm giá + Phí ship |
| BR-PRICE-02 | Giảm giá chỉ tính trên tiền món + món thêm, **không trừ vào phí ship**; giảm giá ≤ subtotal |
| BR-VOU-01 | Voucher % : giảm = floor(subtotal × %), không vượt `maxDiscount` (nếu có) |
| BR-VOU-02 | Voucher cố định: giảm = value, không vượt subtotal |
| BR-VOU-03 | Chỉ áp dụng khi subtotal ≥ minOrder |
| BR-VOU-04 | Chỉ áp dụng trong [startAt, endAt] |
| BR-VOU-05 | usedCount < usageLimit; tăng usedCount nguyên tử khi tạo đơn |
| BR-VOU-06 | Mỗi khách dùng tối đa perUserLimit lần |
| BR-VOU-07 | Mỗi đơn tối đa 1 voucher; đơn bị từ chối/hủy được hoàn lượt |
| BR-SHIP-01 | Phải có báo giá ship hợp lệ mới được đặt |
| BR-SHIP-02 | Quote hết hạn → báo giá lại; nếu phí thay đổi phải hỏi lại khách |
| BR-SHIP-03 | Tiền thu hộ = total nếu COD; = 0 nếu đã thanh toán online |
| BR-SHIP-04 | Chênh lệch phí ship khi gọi shipper so với lúc khách đặt do quán chịu |
| BR-ORD-01 | Chuyển trạng thái chỉ theo state machine đã định nghĩa |
| BR-ORD-02 | Từ chối chỉ được khi chưa gọi shipper |
| BR-ORD-03 | Đơn `PENDING_PAYMENT` quá 15 phút tự hủy |
| BR-PAY-01 | Số tiền thanh toán online luôn do server tính; client không gửi số tiền |
| BR-PAY-03 | Hoàn tiền toàn phần, đúng cổng đã thanh toán; COD không phát sinh hoàn tiền |
| BR-PAY-04 | Mọi callback thanh toán phải xác minh chữ ký VNPay HMAC-SHA512 và xử lý idempotent |
| BR-STORE-01 | Trạng thái hiệu lực: Đóng thủ công > Ngày đặc biệt > Bận còn hạn > Lịch tuần |
| BR-RV-01 | Mỗi đơn đánh giá 1 lần, trong 7 ngày kể từ khi hoàn thành |

**Ví dụ tính tiền:** 2 tô bún riêu lớn (45.000 + 10.000) có thêm chả (+8.000) → 63.000 × 2 = 126.000; 1 trà đá 5.000 → subtotal 131.000. Voucher 10% trần 20.000 → giảm 13.100. Ship 18.000. **Tổng = 131.000 − 13.100 + 18.000 = 135.900đ.**

---

## 7. Yêu cầu phi chức năng

| ID | Nhóm | Yêu cầu | Đo lường |
|---|---|---|---|
| NFR-PERF-01 | Hiệu năng | Khởi động nguội app | < 2,5s trên máy tầm trung |
| NFR-PERF-02 | | Báo giá ship trả về | p95 < 3s |
| NFR-PERF-03 | | Đơn mới tới thiết bị quán | < 5s kể từ khi tạo |
| NFR-PERF-04 | | Cập nhật tài xế tới khách | < 5s kể từ webhook |
| NFR-REL-01 | Tin cậy | Webhook idempotent, không tạo trạng thái trùng | 100% test lặp |
| NFR-REL-02 | | Giỏ hàng không mất khi tắt app | Room |
| NFR-SEC-01 | Bảo mật | Không có secret bên thứ ba trong APK | Kiểm tra decompile |
| NFR-SEC-02 | | Firestore Rules: khách chỉ đọc đơn của mình, quán chỉ đọc đơn của quán; client không ghi `orders` | Test Rules Emulator |
| NFR-SEC-03 | | Xác thực chữ ký webhook & IPN | |
| NFR-SEC-04 | | HTTPS toàn bộ; R8 obfuscation | |
| NFR-USE-01 | Khả dụng | Đặt 1 món từ trang quán tới đặt đơn ≤ 5 thao tác | Test người dùng |
| NFR-USE-02 | | Touch target ≥ 48dp; tương phản ≥ 4.5:1; hỗ trợ font scale, dark mode, TalkBack | |
| NFR-USE-03 | | Toàn bộ giao diện tiếng Việt, định dạng tiền "135.900đ" | |
| NFR-COMP-01 | Tương thích | Android 8.0 → 16, điện thoại; app quán hỗ trợ tablet | |
| NFR-MAINT-01 | Bảo trì | Clean Architecture, coverage logic domain ≥ 70% | |
| NFR-PRIV-01 | Riêng tư | Có Privacy Policy, Data safety khai báo vị trí/SĐT | |

---

## 8. UI/UX Design Proposal

### 8.1 API nội bộ (Cloud Functions callable)
| Hàm | Gọi bởi | Input chính | Output |
|---|---|---|---|
| `getDeliveryQuote` | Khách | storeId, dropoff{lat,lng,address} | fee, distanceKm, quotationId, expiresAt |
| `validateVoucher` | Khách | storeId, code, subtotal | discount hoặc errorCode |
| `placeOrder` | Khách | storeId, items[], addressId, quotationId, voucherCode?, paymentMethod, idempotencyKey | orderId, paymentUrl? |
| `confirmPayment` | Khách | orderId | paymentStatus (đối soát trạng thái thanh toán VNPay) |
| `cancelOrder` | Khách | orderId | ok |
| `confirmOrder` | Quán | orderId | ok |
| `rejectOrder` | Quán | orderId, reasonCode, reasonText | ok, refundStatus |
| `markReady` | Quán | orderId | ok |
| `dispatchDriver` | Quán | orderId | externalOrderId |
| `submitReview` | Khách | orderId, stars, comment | ok |

Mã lỗi chuẩn: `STORE_CLOSED`, `ITEM_UNAVAILABLE`, `PRICE_CHANGED`, `QUOTE_EXPIRED`, `OUT_OF_RANGE`, `VOUCHER_NOT_FOUND`, `VOUCHER_EXPIRED`, `VOUCHER_MIN_ORDER`, `VOUCHER_EXHAUSTED`, `VOUCHER_USER_LIMIT`, `INVALID_TRANSITION`.

### 8.2 Endpoint HTTP
| Endpoint | Nguồn |
|---|---|
| `POST /webhooks/lalamove` | Lalamove |
| `GET /payments/vnpay/return` · `/pay/return` | Trang trả về sau thanh toán (App Link quay lại app) |
| `GET /payments/vnpay/ipn` | VNPay |
| `POST /dev/mock-delivery/advance` | Chỉ môi trường dev — giả lập tài xế |

### 8.3 Dịch vụ bên thứ ba
Lalamove API v3 sandbox, market VN (quotations, orders, driver detail, webhook) · VNPay sandbox (pay URL HMAC-SHA512, IPN, refund) · Google Maps Platform (Maps SDK for Android, Places API Autocomplete, Geocoding API) · FCM HTTP v1.

---

## 9. Matrix thông báo

| Sự kiện | Khách (push) | Quán |
|---|---|---|
| Đơn mới `PLACED` | "Đã gửi đơn tới quán" | 🔔 Chuông lặp + thẻ đơn |
| `CONFIRMED` | "Quán đang chuẩn bị món 🍲" | — |
| `REJECTED` | "Quán xin lỗi, đơn bị từ chối vì {lý do}. {thông tin hoàn tiền}" | — |
| `FINDING_DRIVER` | "Đang tìm tài xế cho bạn" | Badge trạng thái |
| `DRIVER_ASSIGNED` | "Tài xế {tên} · {biển số} đang tới quán" | Thông tin tài xế |
| `ARRIVED_AT_STORE` | "Tài xế đã tới quán" | Thông báo nhẹ |
| `PICKED_UP` / `DELIVERING` | "Món đang trên đường tới bạn 🛵" | Cập nhật |
| `COMPLETED` | "Giao thành công! Chấm điểm cho quán nhé ⭐" | Cập nhật |
| Trễ ETA + 15' | — | ⚠️ Cảnh báo đỏ |
| `refundStatus = REFUNDED` | "Đã hoàn {số tiền}" | — |

---

## 10. Tiêu chí nghiệm thu

| ID | Given / When / Then |
|---|---|
| AC-01 | **Given** giỏ trống, **when** thêm 1 bún riêu "không hành" rồi 1 bún riêu "nhiều hành", **then** giỏ có 2 dòng, mỗi dòng SL 1 |
| AC-02 | **Given** voucher 20.000đ, đơn tối thiểu 100.000đ, **when** subtotal 95.000đ, **then** báo "thêm 5.000đ để dùng mã" và không giảm |
| AC-03 | **Given** subtotal 15.000đ, voucher cố định 20.000đ, ship 18.000đ, **then** tổng = 18.000đ (không trừ vào ship) |
| AC-04 | **Given** voucher còn 1 lượt, **when** 2 khách đặt đồng thời, **then** chỉ 1 đơn dùng được |
| AC-05 | **Given** đơn mới, **when** app quán ở nền, **then** chuông kêu trong ≤ 5s |
| AC-06 | **Given** đơn đã trả VNPay, **when** quán từ chối "Hết chả cá", **then** khách nhận push có lý do và refundStatus chuyển REFUNDED |
| AC-07 | **Given** đơn READY, **when** bấm Gọi Shipper và provider gán tài xế, **then** khách thấy tên, SĐT, biển số trong ≤ 5s |
| AC-08 | **Given** webhook cùng eventId gửi 3 lần, **then** chỉ 1 lần cập nhật và 1 push |
| AC-09 | **Given** quán đặt "Bận 30 phút", **then** khách không đặt được và thấy "Mở lại lúc HH:mm"; sau 30 phút tự mở lại |
| AC-10 | **Given** đơn COMPLETED có món đã ngừng bán, **when** "Đặt lại", **then** các món còn lại vào giỏ đúng cấu hình và có thông báo món bị bỏ |

---

## 11. Ma trận truy vết đề bài → yêu cầu

| Yêu cầu đề bài | FR / BR / UC |
|---|---|
| Cấu trúc thực đơn theo nhóm | FR-MENU-01, UC21 |
| Chọn một bắt buộc / chọn nhiều không bắt buộc | FR-MENU-03, BR-MENU-02, UC05 |
| Voucher %, cố định, điều kiện | FR-VOU-01..03, BR-VOU-01..07, UC07, UC24 |
| Giỏ hàng tách dòng theo yêu cầu khác nhau | FR-CART-01, BR-CART-01, UC06, AC-01 |
| Báo giá vận chuyển tự động theo km | FR-SHIP-01..02, UC08 |
| Công thức tổng tiền, voucher không trừ ship | BR-PRICE-01..02, AC-03 |
| Báo chuông, xác nhận → "Quán đang chuẩn bị món" | FR-MER-01..02, UC25, UC26 |
| Màn bếp nổi bật yêu cầu đặc biệt | FR-KDS-01, UC28 |
| Từ chối đơn + lý do + hoàn tiền | FR-MER-03, UC27, AC-06 |
| Trạng thái hoạt động & lịch | FR-OPS-01..03, BR-STORE-01, UC32, UC33 |
| Gọi shipper gửi đủ thông tin | FR-DSP-01, UC30 |
| Đồng bộ tên, SĐT, biển số tài xế | FR-DSP-02, UC40, AC-07 |
| Push theo tiến độ giao, quán kiểm soát trễ | FR-TRK-01..03, UC42, UC44 |
| Đánh giá 1–5 sao | FR-RV-01, UC12 |
| Đặt lại đơn | FR-RE-01, UC14, AC-10 |






