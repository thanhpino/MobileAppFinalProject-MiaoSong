import { onCall } from "firebase-functions/v2/https";
import { requireAuth, toHttpsError } from "../../../shared/auth";
import { FirestoreVoucherRepository } from "../adapters/firestoreVoucherRepository";
import { ValidateVoucherUseCase } from "../application/validateVoucherUseCase";

const useCase = new ValidateVoucherUseCase(new FirestoreVoucherRepository());

export const validateVoucher = onCall(async (request) => {
  try {
    const user = requireAuth(request.auth);
    const { storeId, code, subtotal } = request.data as {
      storeId: string;
      code: string;
      subtotal: number;
    };
    const data = await useCase.execute({
      storeId,
      code,
      subtotal: Number(subtotal),
      uid: user.uid,
    });
    return { ok: true, data };
  } catch (err) {
    throw toHttpsError(err);
  }
});
