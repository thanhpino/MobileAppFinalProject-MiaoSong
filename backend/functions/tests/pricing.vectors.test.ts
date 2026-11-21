import { readFileSync } from "fs";
import { resolve } from "path";
import { describe, expect, it } from "vitest";
import {
  buildCartLineRawKey,
  calculatePriceBreakdown,
  computeCartLineKey,
  PricedLineInput,
  VoucherType,
} from "../src/modules/pricing";
import { AppError } from "../src/shared/errors";

interface SharedTestVectors {
  cartLineKeyVectors: Array<{
    id: string;
    description: string;
    lineA: {
      menuItemId: string;
      sizeOptionId?: string | null;
      toppingOptionIds: string[];
      note?: string;
    };
    lineB: {
      menuItemId: string;
      sizeOptionId?: string | null;
      toppingOptionIds: string[];
      note?: string;
    };
    expectedRawA: string;
    expectedRawB: string;
    expectEqual: boolean;
  }>;
  pricingVectors: Array<{
    id: string;
    description: string;
    items: PricedLineInput[];
    voucher?: {
      type: VoucherType;
      value: number;
      maxDiscount?: number | null;
      minOrder: number;
    } | null;
    shippingFee: number;
    expected?: {
      itemsTotal: number;
      toppingsTotal: number;
      subtotal: number;
      discount: number;
      shippingFee: number;
      total: number;
    };
    expectedError?: string;
    expectedShortfall?: number;
  }>;
}

const vectorsPath = resolve(__dirname, "../../../shared/test-vectors/pricing.json");
const vectors: SharedTestVectors = JSON.parse(readFileSync(vectorsPath, "utf8"));

describe("Shared Test Vectors — CartLineKey (shared/test-vectors/pricing.json)", () => {
  for (const v of vectors.cartLineKeyVectors) {
    it(`${v.id}: ${v.description}`, () => {
      expect(buildCartLineRawKey(v.lineA)).toBe(v.expectedRawA);
      expect(buildCartLineRawKey(v.lineB)).toBe(v.expectedRawB);
      const keyA = computeCartLineKey(v.lineA);
      const keyB = computeCartLineKey(v.lineB);
      expect(keyA === keyB).toBe(v.expectEqual);
    });
  }
});

describe("Shared Test Vectors — PricingCalculator & Voucher (shared/test-vectors/pricing.json)", () => {
  for (const v of vectors.pricingVectors) {
    it(`${v.id}: ${v.description}`, () => {
      if (v.expectedError) {
        try {
          calculatePriceBreakdown(v.items, v.voucher ?? null, v.shippingFee);
          expect.fail(`Expected AppError ${v.expectedError}`);
        } catch (e) {
          expect(e).toBeInstanceOf(AppError);
          const err = e as AppError;
          expect(err.code).toBe(v.expectedError);
          if (v.expectedShortfall != null) {
            expect(err.details?.shortfall).toBe(v.expectedShortfall);
          }
        }
      } else {
        const res = calculatePriceBreakdown(v.items, v.voucher ?? null, v.shippingFee);
        expect(res).toEqual(v.expected);
      }
    });
  }
});
