import { createHash } from "crypto";

export interface CartLineKeyInput {
  menuItemId: string;
  sizeOptionId?: string | null;
  toppingOptionIds: string[];
  note?: string | null;
}

/**
 * BR-CART-01: Hai lựa chọn chỉ gộp dòng khi trùng hoàn toàn món, size, topping đã sắp xếp và ghi chú chuẩn hóa.
 * Ví dụ: 2 tô bún "không hành" vs "nhiều hành" -> 2 CartLineKey khác nhau.
 */
export function buildCartLineRawKey(input: CartLineKeyInput): string {
  const normalizedNote = (input.note ?? "").trim().replace(/\s+/g, " ").toLowerCase();
  const sortedToppings = [...input.toppingOptionIds].sort().join(",");
  const sizePart = input.sizeOptionId ?? "";
  return `${input.menuItemId}|${sizePart}|${sortedToppings}|${normalizedNote}`;
}

export function computeCartLineKey(input: CartLineKeyInput): string {
  const raw = buildCartLineRawKey(input);
  return createHash("sha1").update(raw, "utf8").digest("hex");
}
