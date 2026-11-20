import { Money } from "../../../shared/money";
import { Voucher, calculateVoucherDiscount } from "./voucher";

export interface PricedLineInput {
  basePrice: number;
  sizeDelta: number;
  toppingsDelta: number[];
  quantity: number;
}

export interface PriceBreakdown {
  itemsTotal: number;
  toppingsTotal: number;
  subtotal: number;
  discount: number;
  shippingFee: number;
  total: number;
}

/**
 * BR-PRICE-01 & BR-PRICE-02:
 * Total = (Tiền món ăn + Tiền món thêm) - Tiền giảm giá khuyến mãi + Phí giao hàng.
 */
export function calculatePriceBreakdown(
  lines: PricedLineInput[],
  voucher: Pick<Voucher, "type" | "value" | "maxDiscount" | "minOrder"> | null | undefined,
  shippingFee: number,
): PriceBreakdown {
  let itemsTotal = Money.zero();
  let toppingsTotal = Money.zero();

  for (const line of lines) {
    const itemUnit = Money.of(line.basePrice).plus(Money.of(line.sizeDelta));
    const toppingUnit = line.toppingsDelta.reduce((acc, d) => acc.plus(Money.of(d)), Money.zero());
    itemsTotal = itemsTotal.plus(itemUnit.times(line.quantity));
    toppingsTotal = toppingsTotal.plus(toppingUnit.times(line.quantity));
  }

  const subtotal = itemsTotal.plus(toppingsTotal);
  const discount = voucher ? Money.of(calculateVoucherDiscount(voucher, subtotal.vnd)) : Money.zero();
  const ship = Money.of(shippingFee);
  const total = subtotal.minus(discount).plus(ship);

  return {
    itemsTotal: itemsTotal.vnd,
    toppingsTotal: toppingsTotal.vnd,
    subtotal: subtotal.vnd,
    discount: discount.vnd,
    shippingFee: ship.vnd,
    total: total.vnd,
  };
}
