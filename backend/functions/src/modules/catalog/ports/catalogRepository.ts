import { MenuItem, Store } from "../domain/store";

export interface CatalogRepository {
  getStore(storeId: string): Promise<Store | null>;
  getMenuItems(storeId: string, itemIds: string[]): Promise<Map<string, MenuItem>>;
}
