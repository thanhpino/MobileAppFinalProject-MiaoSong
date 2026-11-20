import { describe, expect, it } from "vitest";
import { resolveEffectiveStoreStatus, StoreScheduleConfig } from "../src/modules/catalog";
import { OrderStateMachine } from "../src/modules/orders";
import { AppError } from "../src/shared/errors";

describe("OrderStateMachine", () => {
  it("cho phép các chuyển trạng thái hợp lệ theo vòng đời chuẩn", () => {
    expect(OrderStateMachine.canTransition("PENDING_PAYMENT", "PLACED")).toBe(true);
    expect(OrderStateMachine.canTransition("PLACED", "CONFIRMED")).toBe(true);
    expect(OrderStateMachine.canTransition("CONFIRMED", "READY")).toBe(true);
    expect(OrderStateMachine.canTransition("READY", "FINDING_DRIVER")).toBe(true);
    expect(OrderStateMachine.canTransition("FINDING_DRIVER", "DRIVER_ASSIGNED")).toBe(true);
    expect(OrderStateMachine.canTransition("DRIVER_ASSIGNED", "ARRIVED_AT_STORE")).toBe(true);
    expect(OrderStateMachine.canTransition("ARRIVED_AT_STORE", "PICKED_UP")).toBe(true);
    expect(OrderStateMachine.canTransition("PICKED_UP", "DELIVERING")).toBe(true);
    expect(OrderStateMachine.canTransition("DELIVERING", "COMPLETED")).toBe(true);
  });

  it("chặn nhảy cóc trạng thái hoặc từ chối sau khi đã gọi shipper", () => {
    expect(OrderStateMachine.canTransition("PLACED", "COMPLETED")).toBe(false);
    expect(OrderStateMachine.canTransition("FINDING_DRIVER", "REJECTED")).toBe(false);
    expect(OrderStateMachine.canTransition("COMPLETED", "CANCELLED")).toBe(false);
    expect(() => OrderStateMachine.assertTransition("PLACED", "DELIVERING")).toThrow(AppError);
  });
});

describe("StoreStatusResolver (BR-STORE-01)", () => {
  const baseSchedule: StoreScheduleConfig = {
    manualStatus: "OPEN",
    busyUntil: null,
    weeklySchedule: {
      fri: [{ open: "07:00", close: "21:00" }],
    },
    specialDays: [],
  };

  // Thứ Sáu 20/11/2026 10:00 giờ VN = 03:00 UTC
  const friday10amVN = new Date("2026-11-20T03:00:00.000Z");

  it("trả OPEN khi trong khung giờ hoạt động", () => {
    expect(resolveEffectiveStoreStatus(baseSchedule, friday10amVN).status).toBe("OPEN");
  });

  it("ưu tiên Đóng thủ công cao nhất", () => {
    expect(
      resolveEffectiveStoreStatus({ ...baseSchedule, manualStatus: "CLOSED" }, friday10amVN).status,
    ).toBe("CLOSED");
  });

  it("trả BUSY khi busyUntil chưa hết hạn, và tự OPEN lại sau khi hết hạn", () => {
    const busyUntil = new Date(friday10amVN.getTime() + 30 * 60 * 1000);
    expect(
      resolveEffectiveStoreStatus({ ...baseSchedule, busyUntil }, friday10amVN).status,
    ).toBe("BUSY");

    const after35Min = new Date(friday10amVN.getTime() + 35 * 60 * 1000);
    expect(
      resolveEffectiveStoreStatus({ ...baseSchedule, busyUntil }, after35Min).status,
    ).toBe("OPEN");
  });

  it("ưu tiên Ngày đặc biệt đóng cửa cao hơn lịch tuần", () => {
    const withHoliday: StoreScheduleConfig = {
      ...baseSchedule,
      specialDays: [{ date: "2026-11-20", isClosed: true }],
    };
    expect(resolveEffectiveStoreStatus(withHoliday, friday10amVN).status).toBe("CLOSED");
  });
});
