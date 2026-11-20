// Public API của module catalog (Luật R1)
import { FirestoreCatalogRepository } from "./adapters/firestoreCatalogRepository";
import { CatalogService } from "./application/catalogService";

export {
  Store,
  MenuItem,
  OptionGroup,
  MenuOption,
  StoreScheduleConfig,
  EffectiveStoreStatus,
  resolveEffectiveStoreStatus,
} from "./domain/store";

export const catalogService = new CatalogService(new FirestoreCatalogRepository());
