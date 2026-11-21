import { AppError } from "../../../shared/errors";

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PLACED"
  | "CONFIRMED"
  | "READY"
  | "FINDING_DRIVER"
  | "DRIVER_ASSIGNED"
  | "ARRIVED_AT_STORE"
  | "PICKED_UP"
  | "DELIVERING"
  | "COMPLETED"
  | "DELIVERY_FAILED"
  | "REJECTED"
  | "CANCELLED";

const ALLOWED_TRANSITIONS: Record<OrderStatus, readonly OrderStatus[]> = {
  PENDING_PAYMENT: ["PLACED", "CANCELLED"],
  PLACED: ["CONFIRMED", "REJECTED", "CANCELLED"],
  CONFIRMED: ["READY", "REJECTED"],
  READY: ["FINDING_DRIVER"],
  FINDING_DRIVER: ["DRIVER_ASSIGNED", "READY"],
  DRIVER_ASSIGNED: ["ARRIVED_AT_STORE", "PICKED_UP", "READY"],
  ARRIVED_AT_STORE: ["PICKED_UP"],
  PICKED_UP: ["DELIVERING", "COMPLETED"],
  DELIVERING: ["COMPLETED", "DELIVERY_FAILED"],
  COMPLETED: [],
  DELIVERY_FAILED: [],
  REJECTED: [],
  CANCELLED: [],
};

export class OrderStateMachine {
  static canTransition(from: OrderStatus, to: OrderStatus): boolean {
    return ALLOWED_TRANSITIONS[from]?.includes(to) ?? false;
  }

  static assertTransition(from: OrderStatus, to: OrderStatus): void {
    if (!this.canTransition(from, to)) {
      throw new AppError(
        "INVALID_TRANSITION",
        `Không thể chuyển trạng thái đơn từ ${from} sang ${to}.`,
        { currentStatus: from, targetStatus: to },
      );
    }
  }
}
