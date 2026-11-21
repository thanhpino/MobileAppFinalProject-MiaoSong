export type ManualStoreStatus = "OPEN" | "BUSY" | "CLOSED";
export type EffectiveStoreStatus = "OPEN" | "BUSY" | "CLOSED";

export interface TimeSlot {
  open: string;  // "HH:mm"
  close: string; // "HH:mm"
}

export interface SpecialDay {
  date: string; // "YYYY-MM-DD"
  isClosed: boolean;
  slots?: TimeSlot[];
}

export type DayOfWeekKey = "sun" | "mon" | "tue" | "wed" | "thu" | "fri" | "sat";

export interface StoreScheduleConfig {
  manualStatus: ManualStoreStatus;
  busyUntil?: Date | null;
  weeklySchedule: Partial<Record<DayOfWeekKey, TimeSlot[]>>;
  specialDays?: SpecialDay[];
}

export interface Store {
  id: string;
  name: string;
  slug: string;
  address: string;
  avatarUrl?: string;
  lat: number;
  lng: number;
  phone: string;
  maxDistanceKm: number;
  deliveryProvider: "LALAMOVE" | "MOCK";
  schedule: StoreScheduleConfig;
}

export interface MenuOption {
  id: string;
  name: string;
  priceDelta: number;
  isAvailable: boolean;
}

export interface OptionGroup {
  id: string;
  name: string;
  type: "SINGLE" | "MULTI";
  required: boolean;
  min: number;
  max: number;
  options: MenuOption[];
}

export interface MenuItem {
  id: string;
  categoryId: string;
  name: string;
  basePrice: number;
  isAvailable: boolean;
  optionGroups: OptionGroup[];
}

const DAY_KEYS: DayOfWeekKey[] = ["sun", "mon", "tue", "wed", "thu", "fri", "sat"];

function toMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(":").map(Number);
  return h * 60 + m;
}

/**
 * BR-STORE-01: Trạng thái hiệu lực: Đóng thủ công > Ngày đặc biệt > Bận còn hạn > Lịch tuần.
 */
export function resolveEffectiveStoreStatus(
  config: StoreScheduleConfig,
  now: Date = new Date(),
): { status: EffectiveStoreStatus; reopenAt?: Date } {
  if (config.manualStatus === "CLOSED") {
    return { status: "CLOSED" };
  }

  // Giờ Việt Nam (UTC+7)
  const vnTime = new Date(now.getTime() + 7 * 60 * 60 * 1000);
  const dateStr = vnTime.toISOString().slice(0, 10);
  const nowMinutes = vnTime.getUTCHours() * 60 + vnTime.getUTCMinutes();

  const special = config.specialDays?.find((d) => d.date === dateStr);
  if (special) {
    if (special.isClosed) {
      return { status: "CLOSED" };
    }
    const inSpecialSlot = (special.slots ?? []).some(
      (s) => nowMinutes >= toMinutes(s.open) && nowMinutes < toMinutes(s.close),
    );
    if (!inSpecialSlot) {
      return { status: "CLOSED" };
    }
  }

  if (config.busyUntil && config.busyUntil.getTime() > now.getTime()) {
    return { status: "BUSY", reopenAt: config.busyUntil };
  }

  if (special && !special.isClosed) {
    return { status: "OPEN" };
  }

  const dayKey = DAY_KEYS[vnTime.getUTCDay()];
  const slots = config.weeklySchedule[dayKey] ?? [];
  const inWeeklySlot = slots.some(
    (s) => nowMinutes >= toMinutes(s.open) && nowMinutes < toMinutes(s.close),
  );
  return { status: inWeeklySlot ? "OPEN" : "CLOSED" };
}
