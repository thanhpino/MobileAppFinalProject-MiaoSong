# MiaoSong (喵送)

> **Gọi một tiếng, món tới liền.**
> Nền tảng đặt món trực tiếp từ quán (D2C F&B) & điều phối giao hàng tự động qua Lalamove.
> Đồ án cuối kỳ môn **Phát triển ứng dụng di động** — HK1/2026-2027.

## Thành viên

| Họ tên | MSSV | Vai trò | Nhánh Git |
|---|---|---|---|
| Trương Minh Thành | 524H0032 | Tech Lead · Architecture · Backend · Chế độ Quán (xử lý đơn) | `thanhpino` |
| Huỳnh Trần Thiên Phú | 524H0023 | Business Analyst · Android Chế độ Khách · Design System · UI/UX | `thienphu` |

## Tech stack

- **Android:** Kotlin 2.x · Jetpack Compose · Material 3 · Clean Architecture + MVVM (UDF) · Hilt · Coroutines/Flow · Room · Navigation Compose
- **Backend:** Firebase (Auth, Firestore, Cloud Functions TypeScript — Modular Monolith, FCM, Storage, Hosting)
- **Tích hợp:** Lalamove API v3 · VNPay Sandbox · Google Maps API (Maps SDK, Places, Geocoding)

## Danh mục tài liệu thiết kế (`docs/`)

| # | Tài liệu | Nội dung | Phụ trách | Trạng thái |
|---|---|---|---|---|
| 00 | [Project Plan](docs/00-project-plan.md) | Phạm vi MoSCoW, tech stack, timeline 12 ngày, rủi ro & CH Play | Thành | ✅ Hoàn thành |
| 01 | [SRS](docs/01-SRS.md) | Đặc tả yêu cầu phần mềm đầy đủ (36 Use Cases, 39 FRs, 27 BRs) | Thành | ✅ Hoàn thành |
| 02 | [Kiến trúc](docs/02-architecture.md) | ADR, C4 (L1–L3), Modular Monolith, Hexagonal, Android, Bảo mật | Thành | ✅ Hoàn thành |
| 03 | `docs/03-use-case-diagrams.md` | Bộ sơ đồ Use Case (Actor, Tổng quan, Khách, Quán/Bếp, Hệ thống) | **Phú** (`thienphu`) | ⏳ Phú thực hiện |
| 04 | [Sequence Diagrams](docs/04-sequence-diagrams.md) | 13 Sequence Diagrams (`SD-01` → `SD-13`) | Thành | ✅ Hoàn thành |
| 05 | `docs/05-activity-diagrams.md` | 8 Activity Diagrams (`AD-01` → `AD-08`) có Swimlane | **Phú** (`thienphu`) | ⏳ Phú thực hiện |
| 06 | [State · Data · Class](docs/06-state-data-class.md) | State Machine, ERD 14 thực thể, Firestore Schema, Class Diagrams | Thành | ✅ Hoàn thành |
| 07 | [Phân công thiết kế](docs/07-design-assignment.md) | Bảng chia việc tách biệt file giữa Thành & Phú, quy trình Git | Thành | ✅ Hoàn thành |
| 08 | [API Contract & Rules](docs/08-api-contract.md) | 10 Callables, 3 Endpoints/Webhooks, Error Codes, FCM, Firestore Rules, [Test Vectors](shared/test-vectors/pricing.json) | Thành | ✅ Hoàn thành |
| 09 | `docs/09-ui-ux-design.md` | Thiết kế UI/UX (Design Tokens OKLCH, Typography, Sitemap, Wireframe) | **Phú** (`thienphu`) | ⏳ Phú thực hiện |
| 10 | `docs/10-test-plan.md` | Kế hoạch kiểm thử (Test Cases theo 11 AC) & Kịch bản Demo 2 máy thật | **Phú** (`thienphu`) | ⏳ Phú thực hiện |

## Trạng thái

🟡 **Giai đoạn thiết kế** (20/11 – 21/11/2026) · Design freeze: 22/11 9:00
