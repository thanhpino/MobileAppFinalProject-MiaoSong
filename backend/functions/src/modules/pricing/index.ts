// Public API của module pricing (Luật R1)
import { FirestoreVoucherRepository } from "./adapters/firestoreVoucherRepository";
import { ValidateVoucherUseCase } from "./application/validateVoucherUseCase";

export { buildCartLineRawKey, computeCartLineKey, CartLineKeyInput } from "./domain/cartLineKey";
export {
  calculatePriceBreakdown,
  PricedLineInput,
  PriceBreakdown,
} from "./domain/pricingCalculator";
export {
  Voucher,
  VoucherType,
  validateVoucherContext,
  calculateVoucherDiscount,
} from "./domain/voucher";
export { validateVoucher } from "./handlers/validateVoucher";

export const pricingService = {
  validateVoucher: new ValidateVoucherUseCase(new FirestoreVoucherRepository()),
};
