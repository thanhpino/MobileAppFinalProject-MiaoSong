# 05 — Sơ đồ Hoạt động Toàn hệ thống (Activity Diagrams)

> **Dự án:** MiaoSong (喵送) — Nền tảng đặt món & điều phối giao hàng tự động (D2C F&B)  
> **Người phụ trách:** Nhóm phát triển MiaoSong  
> **Tài liệu liên quan:** [01-SRS.md](01-SRS.md) · [02-architecture.md](02-architecture.md) · [03-use-case-diagrams.md](03-use-case-diagrams.md) · [04-sequence-diagrams.md](04-sequence-diagrams.md) · [06-state-data-class.md](06-state-data-class.md)  
> **Tệp mã nguồn PlantUML:** [diagrams/05-activity-diagrams/activity.puml](diagrams/05-activity-diagrams/activity.puml)

---

## Mục lục
1. [Tổng quan Luồng hoạt động](#1-tổng-quan-luồng-hoạt-động)
2. [Activity Diagram 1: Luồng Đặt hàng, Báo giá Ship & Thanh toán](#2-activity-diagram-1-luồng-đặt-hàng-báo-giá-ship--thanh-toán)
3. [Activity Diagram 2: Luồng Tiếp nhận đơn, Vận hành Bếp (KDS) & Gọi Shipper Lalamove](#3-activity-diagram-2-luồng-tiếp-nhận-đơn-vận-hành-bếp-kds--gọi-shipper-lalamove)
4. [Activity Diagram 3: Luồng Hủy đơn / Từ chối đơn & Hoàn tiền tự động ZaloPay Sandbox](#4-activity-diagram-3-luồng-hủy-đơn--từ-chối-đơn--hoàn-tiền-tự-động-zalopay-sandbox)
5. [Activity Diagram 4: Luồng Tác vụ Nền Tự động hóa & Xử lý Đơn quá hạn (Cloud Scheduler)](#5-activity-diagram-4-luồng-tác-vụ-nền-tự-động-hóa--xử-lý-đơn-quá-hạn-cloud-scheduler)
6. [Activity Diagram 5: Luồng Đánh giá & Tái đặt hàng (Reorder)](#6-activity-diagram-5-luồng-đánh-giá--tái-đặt-hàng-reorder)

---

## 1. Tổng quan Luồng hoạt động

MiaoSong vận hành theo mô hình D2C F&B khép kín với sự phối hợp chặt chẽ giữa ứng dụng Khách hàng, ứng dụng Quán (tích hợp Bếp KDS), Backend Firebase và các đối tác bên thứ ba (Lalamove, ZaloPay, OpenStreetMap, FCM). 

Hệ thống biểu đồ hoạt động (Activity Diagrams) mô tả chi tiết:
* Trật tự các bước thực hiện của từng tác nhân theo làn (Swimlanes).
* Các điểm quyết định logic nghiệp vụ (Decision Nodes) và rẽ nhánh điều kiện (IF/ELSE).
* Cơ chế xử lý đồng thời (Fork/Join) và các luồng ngoại lệ (Hoàn tiền, Timeout, Hủy đơn).

---

## 2. Activity Diagram 1: Luồng Đặt hàng, Báo giá Ship & Thanh toán

Sơ đồ mô tả hành trình từ khi khách hàng chọn món, tùy biến topping, tính toán phí giao hàng theo km thực tế, thẩm định voucher, đến khi hoàn tất giao dịch qua COD hoặc ZaloPay Sandbox.

```mermaid
flowchart TD
    START((Bắt đầu)) --> BROWSE["Khách chọn món ăn từ menu"]
    BROWSE --> SHEET["Mở Bottom Sheet tùy chọn"]
    SHEET --> SELECT_OPT["Chọn Size (bắt buộc) & Topping (tùy chọn)"]
    SELECT_OPT --> NOTE["Nhập ghi chú (không hành, ít cay...)"]
    NOTE --> CALC_KEY["Hệ thống tạo CartLineKey = sha1(...)"]
    CALC_KEY --> CHECK_KEY{"Trùng CartLineKey<br/>với dòng đã có?"}
    
    CHECK_KEY -- "Có" --> INC_QTY["Cộng dồn số lượng vào dòng cũ"]
    CHECK_KEY -- "Không" --> ADD_LINE["Thêm dòng mới vào giỏ hàng Room"]
    
    INC_QTY --> OPEN_CART["Khách mở Giỏ hàng & Checkout"]
    ADD_LINE --> OPEN_CART
    
    OPEN_CART --> INPUT_ADDR["Chọn địa chỉ nhận hàng (OpenStreetMap / Nominatim)"]
    INPUT_ADDR --> CALL_QUOTE["Gọi Lalamove API tính cước theo km"]
    
    CALL_QUOTE --> CHECK_RANGE{"Khoảng cách<br/>≤ maxDistanceKm?"}
    CHECK_RANGE -- "Không" --> ERR_RANGE["Báo lỗi: Quán chưa giao tới địa chỉ này"]
    ERR_RANGE --> INPUT_ADDR
    
    CHECK_RANGE -- "Có" --> SHOW_SHIP["Hiển thị phí giao hàng & khoảng cách"]
    SHOW_SHIP --> APPLY_VOUCHER{"Khách có nhập<br/>mã Voucher?"}
    
    APPLY_VOUCHER -- "Có" --> VAL_VOU["Thẩm định: HSD, minOrder, usageLimit, perUserLimit"]
    VAL_VOU --> CHECK_VOU_VALID{"Hợp lệ?"}
    CHECK_VOU_VALID -- "Không" --> ERR_VOU["Thông báo lý do từ chối voucher"]
    ERR_VOU --> CHOOSE_PAY
    CHECK_VOU_VALID -- "Có" --> CALC_DISCOUNT["Giảm trừ subtotal (không trừ vào ship)"]
    CALC_DISCOUNT --> CHOOSE_PAY
    
    APPLY_VOUCHER -- "Không" --> CHOOSE_PAY["Chọn phương thức thanh toán"]
    
    CHOOSE_PAY --> BTN_SUBMIT["Bấm 'Đặt đơn' kèm idempotencyKey"]
    BTN_SUBMIT --> SRV_VERIFY["Server kiểm tra lại giá, tồn kho & quán mở"]
    
    SRV_VERIFY --> CHECK_SRV_OK{"Hợp lệ?"}
    CHECK_SRV_OK -- "Không" --> REJECT_ORDER["Trả lỗi (Hết món / Đổi giá / Đóng cửa)"]
    REJECT_ORDER --> OPEN_CART
    
    CHECK_SRV_OK -- "Có" --> METHOD_FORK{"Phương thức<br/>thanh toán?"}
    
    METHOD_FORK -- "COD" --> COD_FLOW["Tạo đơn: status = PLACED"]
    COD_FLOW --> COMMIT_ORDER
    
    METHOD_FORK -- "ZaloPay" --> ZLP_INIT["Tạo đơn: status = PENDING_PAYMENT"]
    ZLP_INIT --> GEN_URL["Ký tạo đơn ZaloPay (HMAC-SHA256)"]
    GEN_URL --> OPEN_TAB["Mở Chrome Custom Tabs / ZaloPay App"]
    OPEN_TAB --> ZLP_PAY{"Khách thanh toán<br/>thành công?"}
    
    ZLP_PAY -- "Thất bại / Hủy" --> ZLP_FAIL["Đơn giữ PENDING_PAYMENT trong 15p"]
    ZLP_FAIL --> END_WAIT((Chờ thanh toán lại))
    
    ZLP_PAY -- "Thành công" --> ZLP_CB["ZaloPay gửi Callback server-to-server"]
    ZLP_CB --> ZLP_VERIFY["Server verify MAC key2 & cập nhật PLACED"]
    ZLP_VERIFY --> COMMIT_ORDER["Xóa giỏ hàng, mở màn hình Theo dõi đơn"]
    
    COMMIT_ORDER --> TRIGGER_FCM["Bắn chuông FCM tới thiết bị quán"]
    TRIGGER_FCM --> END_SUCCESS((Hoàn thành đặt đơn))
```

---

## 3. Activity Diagram 2: Luồng Tiếp nhận đơn, Vận hành Bếp (KDS) & Gọi Shipper Lalamove

Sơ đồ thể hiện quy trình xử lý của Quán ăn và Bếp, kết hợp điều phối chuyến giao hàng với Lalamove và đồng bộ trạng thái tài xế.

```mermaid
flowchart TD
    START((Đơn PLACED)) --> FCM_RING["FCM phát chuông lặp trên máy Quán"]
    FCM_RING --> STORE_DECIDE{"Quán kiểm tra đơn<br/>& ghi chú đặc biệt"}
    
    STORE_DECIDE -- "Từ chối đơn" --> REJECT_ACT["Quán chọn lý do (Hết món / Quá tải)"]
    REJECT_ACT --> DB_REJECT["Cập nhật đơn: status = REJECTED, hoàn voucher"]
    DB_REJECT --> CHECK_PAID{"Đơn đã thanh toán<br/>ZaloPay trước đó?"}
    
    CHECK_PAID -- "Có" --> CALL_REFUND["Gọi ZaloPay Refund API (/v2/refund)"]
    CALL_REFUND --> REFUND_OK{"Hoàn tiền<br/>thành công?"}
    REFUND_OK -- "Có" --> SET_REFUNDED["refundStatus = REFUNDED"]
    REFUND_OK -- "Không" --> SET_REFUND_FAIL["refundStatus = REFUND_FAILED (Báo quán xử lý)"]
    SET_REFUNDED --> PUSH_REJECT["Gửi Push thông báo lý do + tiền hoàn cho Khách"]
    SET_REFUND_FAIL --> PUSH_REJECT
    
    CHECK_PAID -- "Không (COD)" --> PUSH_REJECT
    PUSH_REJECT --> END_REJECT((Kết thúc từ chối))
    
    STORE_DECIDE -- "Xác nhận nhận đơn" --> CONFIRM_ACT["Bấm 'Xác nhận': status = CONFIRMED"]
    CONFIRM_ACT --> PUSH_COOKING["Push thông báo khách: 'Quán đang nấu 🍲'"]
    CONFIRM_ACT --> KDS_VIEW["Hiện vé đơn trên Màn hình Bếp (KDS)"]
    
    KDS_VIEW --> HIGHLIGHT["Nổi bật cảnh báo dị ứng (nền đỏ) & ghi chú (vàng)"]
    HIGHLIGHT --> COOKING["Đầu bếp chế biến món ăn"]
    COOKING --> PACKED["Bếp bấm 'Đã đóng gói': status = READY"]
    
    PACKED --> CALL_SHIPPER["Chủ quán bấm 'Gọi Shipper'"]
    CALL_SHIPPER --> CHECK_QUOTE{"Báo giá Lalamove<br/>còn hạn 5 phút?"}
    
    CHECK_QUOTE -- "Hết hạn" --> REQUOTE["Lấy quote mới (Quán chịu phí chênh lệch)"]
    REQUOTE --> CREATE_LALA
    CHECK_QUOTE -- "Còn hạn" --> CREATE_LALA["Gọi Lalamove API POST /v3/orders"]
    
    CREATE_LALA --> LALA_RESP{"Lalamove<br/>chấp nhận?"}
    LALA_RESP -- "Lỗi / Không có xe" --> LALA_RETRY["Báo lỗi, giữ READY, cho phép Gọi lại"]
    LALA_RETRY --> CALL_SHIPPER
    
    LALA_RESP -- "Thành công" --> FIND_DRIVER["status = FINDING_DRIVER"]
    FIND_DRIVER --> LALA_ASSIGN["Lalamove điều phối tài xế gần nhất"]
    
    LALA_ASSIGN --> WH_ASSIGN["Lalamove bắn Webhook: DRIVER_ASSIGNED"]
    WH_ASSIGN --> GET_DRIVER["Server lấy Tên, SĐT, Biển số xe"]
    GET_DRIVER --> UPDATE_DRIVER["Lưu vào đơn: status = DRIVER_ASSIGNED"]
    
    UPDATE_DRIVER --> SYNC_REALTIME["Realtime cập nhật thẻ tài xế trên app Khách"]
    UPDATE_DRIVER --> PUSH_DRIVER["Push rung: 'Tài xế {tên} · {biển số} đang tới quán'"]
    
    SYNC_REALTIME --> WH_ARRIVE["Webhook: ARRIVED_AT_STORE"]
    WH_ARRIVE --> WH_PICK["Webhook: PICKED_UP (Món trên đường giao)"]
    WH_PICK --> WH_DONE["Webhook: COMPLETED (Giao thành công)"]
    WH_DONE --> END_DELIVERY((Giao hàng thành công))
```

---

## 4. Activity Diagram 3: Luồng Hủy đơn / Từ chối đơn & Hoàn tiền tự động ZaloPay Sandbox

Sơ đồ mô tả tiến trình xử lý khi Quán từ chối đơn (`rejectOrder`) hoặc Khách tự hủy trước khi quán xác nhận (`cancelOrder`), đảm bảo hoàn trả lượt Voucher trong Transaction và tự động hoàn tiền qua ZaloPay Sandbox API.

```mermaid
flowchart TD
    START((Bắt đầu)) --> ACTOR{"Ai kích hoạt?"}
    ACTOR -- "Quán bấm Từ chối" --> REJECT_CMD["Gọi CF rejectOrder(orderId, reason)"]
    ACTOR -- "Khách bấm Hủy đơn" --> CANCEL_CMD["Gọi CF cancelOrder(orderId, reason)"]

    REJECT_CMD --> TX_CHECK["Mở Firestore Transaction & kiểm tra OrderStateMachine"]
    CANCEL_CMD --> TX_CHECK

    TX_CHECK --> HAS_VOUCHER{"Đơn có áp dụng<br/>Voucher?"}
    HAS_VOUCHER -- "Có" --> RESTORE_VOU["Hoàn trả lượt dùng: usedCount = usedCount - 1"]
    HAS_VOUCHER -- "Không" --> CHECK_PAID{"Đã thanh toán<br/>ZaloPay (PAID)?"}
    RESTORE_VOU --> CHECK_PAID

    CHECK_PAID -- "Không (COD / UNPAID)" --> COMMIT_NONE["Ghi status = REJECTED/CANCELLED, refundStatus = NONE"]
    CHECK_PAID -- "Có (ZaloPay PAID)" --> COMMIT_PENDING["Ghi status = REJECTED/CANCELLED, refundStatus = PENDING"]

    COMMIT_PENDING --> CALL_ZLP["Gọi POST /v2/refund (HMAC-SHA256 key1, zp_trans_id)"]
    CALL_ZLP --> ZLP_OK{"return_code<br/>in [1, 3]?"}
    ZLP_OK -- "Thành công" --> SET_REFUNDED["Cập nhật refundStatus = REFUNDED"]
    ZLP_OK -- "Thất bại" --> SET_FAILED["Cập nhật refundStatus = REFUND_FAILED & gửi cảnh báo LATE_ALERT"]

    COMMIT_NONE --> PUSH_FCM["Trigger onOrderUpdated gửi Push FCM tới Khách & Quán"]
    SET_REFUNDED --> PUSH_FCM
    SET_FAILED --> PUSH_FCM
    PUSH_FCM --> END_REFUND((Hoàn tất xử lý))
```

---

## 5. Activity Diagram 4: Luồng Tác vụ Nền Tự động hóa & Xử lý Đơn quá hạn (Cloud Scheduler)

Sơ đồ thể hiện chu trình quét ngầm định kỳ của hệ thống máy chủ để đảm bảo tính toàn vẹn của dữ liệu và trải nghiệm khách hàng.

```mermaid
flowchart TD
    START_CRON((Định kỳ mỗi 5 phút)) --> SCHEDULER["Cloud Scheduler gửi tín hiệu kích hoạt"]
    SCHEDULER --> SCAN_UNPAID["Quét orders: status == PENDING_PAYMENT & createdAt < now - 15p"]
    
    SCAN_UNPAID --> HAS_UNPAID{"Có đơn quá hạn<br/>chưa thanh toán?"}
    HAS_UNPAID -- "Có" --> LOOP_CANCEL["Bắt đầu vòng lặp xử lý từng đơn"]
    
    LOOP_CANCEL --> TX_CANCEL["Mở Firestore Transaction"]
    TX_CANCEL --> CANCEL_ORDER["Cập nhật đơn: status = CANCELLED"]
    CANCEL_ORDER --> RELEASE_VOUCHER["Hoàn trả lượt dùng Voucher (nếu có)"]
    RELEASE_VOUCHER --> COMMIT_CANCEL["Commit Transaction"]
    COMMIT_CANCEL --> NEXT_UNPAID{"Còn đơn khác?"}
    NEXT_UNPAID -- "Còn" --> LOOP_CANCEL
    NEXT_UNPAID -- "Hết" --> SCAN_LATE
    
    HAS_UNPAID -- "Không" --> SCAN_LATE["Quét orders: status in [FINDING_DRIVER, DELIVERING] & etaAt + 15p < now"]
    
    SCAN_LATE --> HAS_LATE{"Có đơn giao trễ<br/>vượt ETA + 15p?"}
    HAS_LATE -- "Có" --> LOOP_LATE["Lặp từng đơn trễ"]
    
    LOOP_LATE --> CHECK_ALERTED{"Đã cảnh báo<br/>trước đó chưa?"}
    CHECK_ALERTED -- "Đã cảnh báo" --> NEXT_LATE
    CHECK_ALERTED -- "Chưa" --> SET_LATE_FLAG["Cập nhật lateFlag = true"]
    SET_LATE_FLAG --> PUSH_ALERT["Bắn cảnh báo đỏ tới thiết bị Quán"]
    PUSH_ALERT --> NEXT_LATE{"Còn đơn trễ khác?"}
    NEXT_LATE -- "Còn" --> LOOP_LATE
    NEXT_LATE -- "Hết" --> END_CYCLE
    
    HAS_LATE -- "Không" --> END_CYCLE((Kết thúc chu kỳ quét))
```

---

## 6. Activity Diagram 5: Luồng Đánh giá & Tái đặt hàng (Reorder)

Sơ đồ thể hiện trải nghiệm khách hàng sau khi nhận món ăn thành công: đánh giá chất lượng phục vụ và khả năng đặt lại đơn hàng cũ nhanh chóng mà không cần cấu hình lại từng món.

```mermaid
flowchart TD
    START((Đơn hàng COMPLETED)) --> PROMPT_REVIEW["Hệ thống hiển thị Bottom Sheet Đánh giá"]
    PROMPT_REVIEW --> CUS_DECIDE{"Khách có muốn<br/>đánh giá ngay?"}
    
    CUS_DECIDE -- "Để sau" --> SAVE_HISTORY["Đơn lưu vào Lịch sử đơn hàng"]
    
    CUS_DECIDE -- "Đánh giá ngay" --> SELECT_STARS["Chọn số sao (1-5 sao bắt buộc)"]
    SELECT_STARS --> SELECT_TAGS["Chọn tag nhanh (Nóng hổi, Đóng gói kỹ...)"]
    SELECT_TAGS --> INPUT_COMMENT["Nhập nhận xét (tùy chọn ≤ 500 ký tự)"]
    INPUT_COMMENT --> SUBMIT_REV["Nhấn 'Gửi đánh giá'"]
    
    SUBMIT_REV --> TX_REV["Server Transaction: Tạo reviews/{orderId}"]
    TX_REV --> RECALC_RATING["Tính lại stores.ratingAvg & ratingCount của Quán"]
    RECALC_RATING --> COMMIT_REV["Commit thành công, hiển thị mèo cảm ơn Miu"]
    COMMIT_REV --> SAVE_HISTORY
    
    SAVE_HISTORY --> REORDER_TRIGGER["Khách mở lại Lịch sử & bấm 'Đặt lại đơn này'"]
    REORDER_TRIGGER --> FETCH_MENU["Hệ thống lấy thực đơn hiện tại của Quán"]
    
    FETCH_MENU --> LOOP_ITEMS["Duyệt từng dòng món ăn trong đơn cũ"]
    LOOP_ITEMS --> CHECK_ITEM_AVAIL{"Món & Topping cũ<br/>còn bán không?"}
    
    CHECK_ITEM_AVAIL -- "Không còn bán" --> SKIP_ITEM["Bỏ qua dòng này & ghi nhận thông báo bỏ món"]
    CHECK_ITEM_AVAIL -- "Còn bán" --> CHECK_PRICE{"Giá có thay đổi?"}
    
    CHECK_PRICE -- "Đổi giá" --> USE_NEW_PRICE["Dùng giá mới & ghi nhận nhãn 'Giá cập nhật'"]
    CHECK_PRICE -- "Giữ giá" --> KEEP_PRICE["Giữ nguyên đơn giá cũ"]
    
    USE_NEW_PRICE --> ADD_TO_CART["Nạp dòng món vào giỏ hàng Room"]
    KEEP_PRICE --> ADD_TO_CART
    SKIP_ITEM --> NEXT_REORDER_ITEM{"Còn món khác?"}
    ADD_TO_CART --> NEXT_REORDER_ITEM
    
    NEXT_REORDER_ITEM -- "Còn" --> LOOP_ITEMS
    NEXT_REORDER_ITEM -- "Hết" --> OPEN_NEW_CART["Chuyển thẳng khách tới màn hình Giỏ hàng"]
    OPEN_NEW_CART --> SHOW_BANNER["Hiển thị Banner tóm tắt các thay đổi (nếu có)"]
    SHOW_BANNER --> END_REORDER((Sẵn sàng Checkout mới))
```
