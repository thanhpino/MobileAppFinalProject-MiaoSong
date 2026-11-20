import { AppError } from "../../../shared/errors";
import { MenuItem, Store, resolveEffectiveStoreStatus } from "../domain/store";
import { CatalogRepository } from "../ports/catalogRepository";

export class CatalogService {
  constructor(private readonly repo: CatalogRepository) {}

  async getStoreForOrder(storeId: string, now: Date = new Date()): Promise<Store> {
    const store = await this.repo.getStore(storeId);
    if (!store) {
      throw new AppError("STORE_CLOSED", "Không tìm thấy quán ăn.");
    }
    const effective = resolveEffectiveStoreStatus(store.schedule, now);
    if (effective.status !== "OPEN") {
      throw new AppError("STORE_CLOSED", "Quán hiện đang bận hoặc đóng cửa.", {
        effectiveStatus: effective.status,
        reopenAt: effective.reopenAt?.toISOString() ?? null,
      });
    }
    return store;
  }

  async getStoreInfo(storeId: string): Promise<Store> {
    const store = await this.repo.getStore(storeId);
    if (!store) {
      throw new AppError("STORE_CLOSED", "Không tìm thấy quán ăn.");
    }
    return store;
  }

  async resolveMenuItems(storeId: string, itemIds: string[]): Promise<Map<string, MenuItem>> {
    return this.repo.getMenuItems(storeId, itemIds);
  }
}
