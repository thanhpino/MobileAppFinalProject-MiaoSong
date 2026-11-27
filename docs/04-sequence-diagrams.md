# 04 — Sequence Diagrams

> Phụ trách: **Trương Minh Thành (524H0032)** · Nhánh: `thanhpino`
> Quy ước: `APP` = MiaoSong Android · `CF` = Cloud Functions · `FS` = Firestore · đường nét đứt = phản hồi / bất đồng bộ.

| ID | Luồng | Use case |
|---|---|---|
| [SD-01](#sd-01--đăng-nhập--điều-hướng-theo-role) | Đăng nhập & điều hướng theo role | UC01, UC20 |
| [SD-02](#sd-02--tùy-chỉnh-món--thêm-vào-giỏ) | Tùy chỉnh món & thêm vào giỏ | UC05, UC06 |
| [SD-03](#sd-03--chọn-địa-chỉ--báo-giá-ship) | Chọn địa chỉ & báo giá ship | UC02, UC08 |
| [SD-04](#sd-04--áp-dụng-voucher) | Áp dụng voucher | UC07 |
| [SD-05](#sd-05--đặt-đơn-cod) | Đặt đơn COD | UC09 |
| [SD-06](#sd-06--thanh-toán-zalopay) | Thanh toán ZaloPay | UC09, UC41 |
| [SD-07](#sd-07--quán-nhận--xác-nhận-đơn) | Quán nhận & xác nhận đơn | UC25, UC26 |
| [SD-08](#sd-08--từ-chối-đơn--hoàn-tiền) | Từ chối đơn & hoàn tiền | UC27 |
| [SD-09](#sd-09--đóng-gói-tại-bếp--gọi-shipper) | Đóng gói tại bếp & gọi shipper | UC28–UC30 |
| [SD-10](#sd-10--đồng-bộ-tài-xế--trạng-thái-giao-webhook) | Đồng bộ tài xế & trạng thái giao | UC40, UC44 |
| [SD-11](#sd-11--phát-hiện-đơn-trễ--hủy-đơn-chưa-thanh-toán) | Phát hiện đơn trễ & hủy đơn chưa thanh toán | UC42, UC43 |
| [SD-12](#sd-12--đánh-giá-đơn) | Đánh giá đơn | UC12 |
| [SD-13](#sd-13--đặt-lại-đơn) | Đặt lại đơn | UC14 |
| [SD-14](#sd-14--khách-tự-hủy-đơn--hoàn-tiền-zalopay-tự-động) | Khách tự hủy đơn & hoàn tiền ZaloPay tự động | UC15 |
| [SD-15](#sd-15--vận-hành-màn-hình-bếp-kds--đóng-gói) | Vận hành màn hình bếp (KDS) & đóng gói | UC28, UC29 |
| [SD-16](#sd-16--đổi-trạng-thái-quán--lịch-bận-tức-thời) | Đổi trạng thái quán & lịch bận tức thời | UC32, UC33 |

---

## SD-01 — Đăng nhập & điều hướng theo role

```mermaid
sequenceDiagram
    actor U as Người dùng
    participant APP as MiaoSong
    participant AUTH as Firebase Auth
    participant CF as Cloud Functions
    participant FS as Firestore
    U->>APP: Bấm "Đăng nhập với Google"
    APP->>AUTH: signInWithCredential(googleIdToken)
    AUTH-->>APP: FirebaseUser
    alt Lần đầu đăng nhập
        AUTH->>CF: trigger onUserCreated
        CF->>FS: tạo users/{uid} role = CUSTOMER
        CF->>AUTH: setCustomUserClaims(role = CUSTOMER)
    end
    APP->>AUTH: getIdToken(forceRefresh = true)
    AUTH-->>APP: token kèm claims role, storeId
    APP->>APP: lưu FCM token vào users/{uid}.fcmTokens
    alt role = CUSTOMER
        APP-->>U: CustomerNavGraph (Home)
    else role = MERCHANT
        APP-->>U: MerchantNavGraph (Đơn · Bếp · Menu · Quán)
    end
```

## SD-02 — Tùy chỉnh món & thêm vào giỏ

```mermaid
sequenceDiagram
    actor KH as Khách
    participant UI as ItemSheet (Compose)
    participant VM as ItemViewModel
    participant UC as AddToCartUseCase
    participant KEY as CartLineKey
    participant REPO as CartRepository
    participant DB as Room
    KH->>UI: Chạm món "Bún riêu"
    UI->>VM: onOpen(itemId)
    VM-->>UI: UiState(optionGroups, giá cơ bản)
    KH->>UI: Chọn "Tô lớn", tích "Thêm chả", ghi chú "không hành", SL 1
    UI->>VM: onSelectionChanged(...)
    VM->>VM: validate nhóm bắt buộc, tính đơn giá
    VM-->>UI: canAdd = true, giá = 63.000đ
    KH->>UI: Bấm "Thêm vào giỏ"
    UI->>VM: onAddToCart()
    VM->>UC: invoke(item, selections, note, qty)
    UC->>REPO: currentStoreId()
    alt Giỏ đang chứa món quán khác
        UC-->>VM: Result.Conflict
        VM-->>UI: hỏi "Xóa giỏ cũ?"
    else Cùng quán hoặc giỏ trống
        UC->>KEY: of(itemId, sizeId, toppingIds, note)
        KEY-->>UC: key = sha1(...)
        UC->>REPO: upsert(line)
        REPO->>DB: SELECT theo key
        alt Đã có dòng cùng key
            REPO->>DB: UPDATE quantity = quantity + 1
        else Chưa có
            REPO->>DB: INSERT dòng mới
        end
        UC-->>VM: Result.Success
        VM-->>UI: đóng sheet, snackbar "Đã thêm vào giỏ"
    end
```

## SD-03 — Chọn địa chỉ & báo giá ship

```mermaid
sequenceDiagram
    actor KH as Khách
    participant APP as MiaoSong
    participant OSM as OpenStreetMap (Nominatim API)
    participant CF as Cloud Functions (delivery)
    participant LL as Lalamove
    KH->>APP: Gõ "12 Nguyễn Văn Bảo"
    APP->>OSM: Search / Nominatim API (debounce 300ms)
    OSM-->>APP: danh sách gợi ý
    KH->>APP: Chọn gợi ý
    APP->>OSM: Geocoding / Address Details
    OSM-->>APP: lat, lng, địa chỉ chuẩn (display_name)
    APP->>CF: getDeliveryQuote(storeId, dropoff)
    CF->>CF: kiểm tra khoảng cách ≤ maxDistanceKm
    alt Ngoài bán kính
        CF-->>APP: lỗi OUT_OF_RANGE
        APP-->>KH: "Quán chưa giao tới địa chỉ này"
    else Trong bán kính
        CF->>LL: POST /v3/quotations (ký HMAC)
        alt Lalamove phản hồi
            LL-->>CF: quotationId, total, distance, expiresAt
            CF-->>APP: fee, distanceKm, quotationId, expiresAt
            APP-->>KH: "Phí giao 18.000đ · 3,2 km"
        else Timeout 8s / lỗi
            CF-->>APP: lỗi QUOTE_UNAVAILABLE
            APP-->>KH: "Chưa lấy được phí ship" + nút Thử lại (khóa nút Đặt)
        end
    end
```

## SD-04 — Áp dụng voucher

```mermaid
sequenceDiagram
    actor KH as Khách
    participant APP as MiaoSong
    participant CF as Cloud Functions (pricing)
    participant FS as Firestore
    KH->>APP: Nhập mã "MIAO10"
    APP->>CF: validateVoucher(storeId, code, subtotal)
    CF->>FS: đọc stores/{id}/vouchers where code
    CF->>FS: đếm voucherRedemptions của uid
    CF->>CF: Specification: Active AND DateRange AND MinOrder AND UsageLimit AND PerUserLimit
    alt Hợp lệ
        CF->>CF: discount = min(floor(subtotal x 10%), maxDiscount, subtotal)
        CF-->>APP: discount = 13.100
        APP-->>KH: dòng "Giảm giá −13.100đ", tổng cập nhật
    else Không đạt
        CF-->>APP: VOUCHER_MIN_ORDER (thiếu 12.000)
        APP-->>KH: "Thêm 12.000đ nữa để dùng mã nhé"
    end
    Note over APP,CF: Chỉ kiểm tra, CHƯA giữ lượt. Lượt được giữ trong transaction của placeOrder (SD-05).
```

## SD-05 — Đặt đơn COD

```mermaid
sequenceDiagram
    actor KH as Khách
    participant APP as MiaoSong
    participant ORD as CF orders
    participant CAT as catalog
    participant PRI as pricing
    participant DEL as delivery
    participant FS as Firestore
    participant NOT as notification
    KH->>APP: Bấm "Đặt đơn" (COD)
    APP->>APP: tạo idempotencyKey (UUID) nếu chưa có
    APP->>ORD: placeOrder(storeId, items, addressId, quotationId, voucherCode, COD, idempotencyKey)
    ORD->>FS: tìm đơn theo idempotencyKey
    alt Đã tồn tại
        ORD-->>APP: trả lại orderId cũ
    else Chưa có
        ORD->>CAT: isStoreOpen(storeId, now)
        ORD->>CAT: resolveItems(items) lấy giá & option hiện tại
        alt Món hết / giá đổi
            ORD-->>APP: ITEM_UNAVAILABLE hoặc PRICE_CHANGED
        else Hợp lệ
            ORD->>DEL: ensureQuoteValid(quotationId)
            ORD->>PRI: calculate(lines, voucher, shippingFee)
            PRI-->>ORD: subtotal, discount, shippingFee, total
            ORD->>FS: runTransaction
            Note over ORD,FS: reserveVoucher (usedCount + 1, kiểm tra usageLimit)<br/>tạo orders/{id} status = PLACED, snapshot items, pricing
            FS-->>ORD: commit OK
            ORD-->>APP: orderId
            FS-)NOT: trigger onOrderUpdated (PLACED)
            NOT-)NOT: FCM high-priority tới thiết bị quán
            APP-->>KH: chuyển màn Theo dõi đơn, xóa giỏ
        end
    end
```

## SD-06 — Thanh toán ZaloPay

```mermaid
sequenceDiagram
    actor KH as Khách
    participant APP as MiaoSong
    participant CF as Cloud Functions
    participant FS as Firestore
    participant ZLP as ZaloPay Sandbox
    participant HOST as Firebase Hosting
    KH->>APP: Chọn ZaloPay, bấm Đặt
    APP->>CF: placeOrder(..., ZALOPAY)
    CF->>FS: tạo đơn status = PENDING_PAYMENT (giữ voucher)
    CF->>CF: build order: app_id, app_trans_id, amount, mac = HMAC-SHA256
    CF->>ZLP: POST /v2/create (tạo đơn thanh toán)
    ZLP-->>CF: order_url, zp_trans_token
    CF-->>APP: paymentUrl (order_url / applink)
    APP->>ZLP: mở Chrome Custom Tab / ZaloPay App
    KH->>ZLP: chọn thẻ / ví test, xác nhận thanh toán
    par Server-to-server
        ZLP->>CF: POST /payments/zalopay/callback (data, mac)
        CF->>CF: verify MAC (HMAC-SHA256 với key2), parse data, idempotent
        CF->>FS: payment.status = PAID, status = PLACED
        CF-->>ZLP: {"return_code": 1, "return_message": "success"}
    and Trình duyệt quay về
        ZLP->>HOST: redirect /pay/return?orderId
        HOST-->>APP: App Link mở lại MiaoSong
    end
    APP->>CF: confirmPayment(orderId)
    CF-->>APP: PAID
    FS-->>APP: snapshot realtime status = PLACED
    APP-->>KH: "Thanh toán thành công, đã gửi đơn tới quán"
    opt Thanh toán thất bại / khách đóng tab
        APP-->>KH: "Chưa thanh toán" + nút Thanh toán lại (còn 15 phút)
    end
```

## SD-07 — Quán nhận & xác nhận đơn

```mermaid
sequenceDiagram
    participant FS as Firestore
    participant NOT as CF notification
    participant FCM as FCM
    participant MAPP as Máy quán
    actor Q as Quán
    participant CF as CF orders
    participant KAPP as Máy khách
    FS-)NOT: onOrderUpdated: status = PLACED
    NOT->>FS: đọc fcmTokens của quán
    NOT->>FCM: data message priority HIGH, type = NEW_ORDER
    FCM-)MAPP: nhận tin
    MAPP->>MAPP: Notification channel "new_order" phát chuông lặp
    Note over MAPP: Dự phòng: Foreground Service lắng nghe Firestore khi quán đang mở
    Q->>MAPP: Mở đơn, xem món, ghi chú nổi bật
    Q->>MAPP: Bấm "Xác nhận nhận đơn"
    MAPP->>CF: confirmOrder(orderId)
    CF->>CF: OrderStateMachine.can(PLACED → CONFIRMED) và kiểm tra storeId của token
    CF->>FS: status = CONFIRMED, statusHistory += {CONFIRMED, at, by}
    CF-->>MAPP: OK, dừng chuông, đơn sang tab "Đang làm"
    FS-)NOT: onOrderUpdated: CONFIRMED
    NOT->>FCM: push tới khách
    FCM-)KAPP: "Quán đang chuẩn bị món 🍲"
```

## SD-08 — Từ chối đơn & hoàn tiền

```mermaid
sequenceDiagram
    actor Q as Quán
    participant MAPP as Máy quán
    participant ORD as CF orders
    participant PRI as pricing
    participant FS as Firestore
    participant PAY as CF payment
    participant GW as ZaloPay
    participant KAPP as Máy khách
    Q->>MAPP: "Từ chối đơn" → lý do "Hết nguyên liệu: chả cá"
    MAPP->>ORD: rejectOrder(orderId, OUT_OF_STOCK, "Hết chả cá")
    ORD->>ORD: chỉ cho phép khi PLACED / CONFIRMED
    ORD->>FS: transaction: status = REJECTED, rejectReason, releaseVoucher
    ORD-->>MAPP: OK + gợi ý tắt món "Bún chả cá"
    FS-)PAY: onOrderUpdated: REJECTED và payment.status = PAID
    PAY->>FS: refundStatus = PENDING
    PAY->>GW: POST /v2/refund (m_refund_id, zp_trans_id, amount, mac)
    alt Thành công
        GW-->>PAY: OK
        PAY->>FS: refundStatus = REFUNDED
    else Lỗi
        PAY->>FS: refundStatus = FAILED
        PAY-)MAPP: cảnh báo "Hoàn tiền lỗi, cần xử lý thủ công"
    end
    FS-)KAPP: push "Quán xin lỗi, đơn bị từ chối vì hết chả cá. Đã hoàn tiền."
```

## SD-09 — Đóng gói tại bếp & gọi shipper

```mermaid
sequenceDiagram
    actor Q as Quán
    participant MAPP as Máy quán
    participant ORD as CF orders
    participant DEL as CF delivery
    participant LL as Lalamove
    participant FS as Firestore
    Q->>MAPP: Tab Bếp (KDS) → "Đã đóng gói"
    MAPP->>ORD: markReady(orderId)
    ORD->>FS: status = READY
    Q->>MAPP: "Gọi Shipper"
    MAPP->>ORD: dispatchDriver(orderId)
    ORD->>DEL: dispatch(order)
    alt Quote đã hết hạn
        DEL->>LL: POST /v3/quotations
        LL-->>DEL: quotationId mới (chênh lệch phí quán chịu)
    end
    DEL->>LL: POST /v3/orders (sender: quán, recipient: khách, remarks: ghi chú + tiền thu hộ)
    alt Thành công
        LL-->>DEL: lalamoveOrderId, shareLink
        DEL-->>ORD: ExternalOrder
        ORD->>FS: status = FINDING_DRIVER, delivery.externalId
        ORD-->>MAPP: "Đang tìm tài xế..."
    else Lỗi
        LL-->>DEL: 4xx/5xx
        ORD-->>MAPP: giữ READY, "Gọi shipper thất bại" + Thử lại
    end
```

## SD-10 — Đồng bộ tài xế & trạng thái giao (webhook)

```mermaid
sequenceDiagram
    participant LL as Lalamove
    participant WH as CF /webhooks/lalamove
    participant DEL as delivery
    participant BUS as EventBus
    participant ORD as orders
    participant FS as Firestore
    participant NOT as notification
    participant KAPP as Máy khách
    participant MAPP as Máy quán
    LL->>WH: POST DRIVER_ASSIGNED
    WH->>DEL: handleWebhook(req)
    DEL->>DEL: verify chữ ký HMAC
    DEL->>FS: kiểm tra processedEvents/{eventId}
    alt Đã xử lý
        DEL-->>LL: 200 (bỏ qua)
    else Mới
        DEL->>LL: GET /v3/orders/{id}/drivers/{driverId}
        LL-->>DEL: name, phone, plateNumber
        DEL->>BUS: DeliveryStatusChanged(DRIVER_ASSIGNED, driver)
        BUS->>ORD: transition(orderId, DRIVER_ASSIGNED)
        ORD->>FS: delivery.driver, status, processedEvents
        DEL-->>LL: 200
        FS-->>KAPP: realtime: thẻ tài xế (tên, SĐT, biển số, nút Gọi)
        FS-->>MAPP: realtime: cập nhật đơn
        FS-)NOT: onOrderUpdated
        NOT-)KAPP: push rung "Tài xế Minh · 59X1-123.45 đang tới quán"
    end
    loop Mỗi lần đổi trạng thái: ARRIVED_AT_STORE → PICKED_UP → DELIVERING → COMPLETED
        LL->>WH: ORDER_STATUS_CHANGED
        WH->>ORD: (cùng luồng trên) transition
        NOT-)KAPP: push theo trạng thái
    end
```

## SD-11 — Phát hiện đơn trễ & hủy đơn chưa thanh toán

```mermaid
sequenceDiagram
    participant SCH as Cloud Scheduler
    participant CF as CF schedulers
    participant FS as Firestore
    participant PRI as pricing
    participant NOT as notification
    participant MAPP as Máy quán
    SCH->>CF: mỗi 5 phút
    CF->>FS: query orders PENDING_PAYMENT, createdAt < now - 15 phút
    loop Mỗi đơn
        CF->>FS: transaction: status = CANCELLED, releaseVoucher
    end
    CF->>FS: query orders đang giao, etaAt + 15 phút < now, chưa cảnh báo
    loop Mỗi đơn trễ
        CF->>FS: lateFlag = true
        CF->>NOT: notifyLate(storeId, orderId)
        NOT-)MAPP: "⚠️ Đơn MS-0012 trễ 18 phút"
    end
```

## SD-12 — Đánh giá đơn

```mermaid
sequenceDiagram
    actor KH as Khách
    participant APP as MiaoSong
    participant CF as CF review
    participant FS as Firestore
    FS-->>APP: status = COMPLETED
    APP-->>KH: bottom sheet "Chấm điểm cho quán nhé ⭐"
    KH->>APP: 5 sao, chip "Nóng hổi", nhận xét
    APP->>CF: submitReview(orderId, 5, tags, comment)
    CF->>CF: kiểm tra chủ đơn, COMPLETED, ≤ 7 ngày, chưa đánh giá
    CF->>FS: transaction: tạo reviews/{orderId}, cập nhật stores.ratingAvg, ratingCount
    CF-->>APP: OK
    APP-->>KH: "Cảm ơn bạn! Miu đã chuyển lời tới quán 🐱"
```

## SD-13 — Đặt lại đơn

```mermaid
sequenceDiagram
    actor KH as Khách
    participant UI as HistoryScreen
    participant UC as ReorderUseCase
    participant MENU as MenuRepository
    participant CART as CartRepository
    KH->>UI: Chọn đơn đã giao → "Đặt lại đơn này"
    UI->>UC: invoke(order)
    UC->>MENU: getMenu(storeId) (realtime cache)
    loop Mỗi dòng trong order.items
        UC->>UC: đối chiếu item, size, topping còn bán
        alt Còn bán đủ
            UC->>UC: thêm vào danh sách hợp lệ với giá hiện tại
        else Thiếu món / option
            UC->>UC: thêm vào danh sách bị bỏ
        end
    end
    UC->>CART: replaceCart(validLines)
    UC-->>UI: ReorderResult(added, skipped, priceChanged)
    UI-->>KH: mở Giỏ hàng + banner "1 món không còn bán, giá đã cập nhật"
```

---

## SD-14 — Khách tự hủy đơn & hoàn tiền ZaloPay tự động

```mermaid
sequenceDiagram
    actor KH as Khách hàng
    participant APP as MiaoSong (OrderTrackingScreen)
    participant CF as CF cancelOrder
    participant FS as Firestore
    participant ZLP as ZaloPay Sandbox (/v2/refund)
    participant TRG as CF onOrderUpdated
    participant FCM as FCM

    KH->>APP: Bấm "Hủy đơn & Hoàn tiền ZaloPay" (khi status in [PENDING_PAYMENT, PLACED])
    APP->>CF: cancelOrder({ orderId, reason })
    CF->>FS: runTransaction: kiểm tra OrderStateMachine.assertTransition(status, CANCELLED)
    alt Có dùng Voucher
        CF->>FS: decrement stores/{storeId}/vouchers/{voucherId}.usedCount (-1)
    end
    alt Đơn đã thanh toán ZaloPay (payment.status == PAID)
        CF->>FS: update { status: CANCELLED, refundStatus: PENDING, rejectReason }
        CF->>ZLP: POST /v2/refund (HMAC-SHA256 key1, zp_trans_id, amountVnd)
        ZLP-->>CF: { return_code: 1 | 3, refund_id }
        CF->>FS: update { refundStatus: REFUNDED }
    else Đơn COD hoặc chưa thanh toán
        CF->>FS: update { status: CANCELLED, refundStatus: NONE, rejectReason }
    end
    CF-->>APP: { ok: true, status: CANCELLED, refundStatus }
    FS-)TRG: onDocumentWritten(orders/{orderId})
    TRG->>FCM: push Khách (đã hủy / đã hoàn tiền) + push Bếp Quán (nếu trước đó là PLACED)
```

---

## SD-15 — Vận hành màn hình bếp (KDS) & đóng gói

```mermaid
sequenceDiagram
    participant FS as Firestore
    participant KDS as Màn hình Bếp (KDS)
    actor BEP as Đầu bếp
    participant CF as CF markReady
    participant QAPP as App quản lý Quán

    FS-)KDS: snapshot orders (status = CONFIRMED)
    KDS->>KDS: Hiển thị ticket mới (sắp xếp cũ -> mới)
    KDS->>KDS: Nổi bật cảnh báo dị ứng (nền đỏ), ghi chú (nền vàng)
    KDS->>KDS: Bắt đầu đồng hồ đếm phút chế biến (elapsedMinutes)
    BEP->>BEP: Chế biến món theo ticket
    BEP->>KDS: Chế biến xong, đóng gói, bấm "Đã đóng gói"
    KDS->>CF: markReady(orderId)
    CF->>CF: OrderStateMachine.validate(CONFIRMED -> READY)
    CF->>FS: status = READY, readyAt = now
    CF-->>KDS: Thành công, vé biến mất khỏi KDS
    FS-)QAPP: snapshot realtime status = READY
    QAPP->>QAPP: Chuyển đơn sang tab "Chờ ship", sáng nút "Gọi Shipper"
```

---

## SD-16 — Đổi trạng thái quán & lịch bận tức thời

```mermaid
sequenceDiagram
    actor Q as Chủ quán
    participant MAPP as App Quán
    participant FS as Firestore
    participant KAPP as App Khách
    participant CF as CF placeOrder
    actor KH as Khách hàng

    Q->>MAPP: Chọn "Bận 30 phút"
    MAPP->>FS: update stores/{id} {manualStatus: "BUSY", busyUntil: now + 30m}
    FS-->>MAPP: Cập nhật thành công, hiện đếm ngược mở lại
    
    par Khách đang xem quán
        FS-)KAPP: snapshot stores/{id}
        KAPP->>KAPP: StoreStatusResolver tính effectiveStatus = BUSY
        KAPP-->>KH: Hiện banner "Quán đang bận, nhận đơn lại lúc HH:mm", khóa nút Đặt
    and Khách cố tình gọi API đặt đơn
        KH->>CF: placeOrder(storeId, ...)
        CF->>FS: đọc stores/{id}
        CF->>CF: kiểm tra effectiveStatus == BUSY
        CF-->>KH: lỗi STORE_CLOSED {effectiveStatus: "BUSY", reopenAt: timestamp}
    end
    
    opt Hết thời gian bận (now > busyUntil)
        KAPP->>KAPP: StoreStatusResolver tự động chuyển effectiveStatus = OPEN
        KAPP-->>KH: Gỡ banner bận, mở lại nút Đặt
    end
```


