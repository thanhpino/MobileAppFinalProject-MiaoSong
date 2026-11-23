import { db } from "../../../shared/firebase";
import { MenuItem, Store } from "../domain/store";
import { CatalogRepository } from "../ports/catalogRepository";

export class FirestoreCatalogRepository implements CatalogRepository {
  async getStore(storeId: string): Promise<Store | null> {
    const snap = await db.collection("stores").doc(storeId).get();
    if (!snap.exists) return null;
    const d = snap.data()!;
    return {
      id: snap.id,
      name: String(d.name ?? ""),
      slug: String(d.slug ?? ""),
      address: String(d.address ?? ""),
      avatarUrl: String(d.avatarUrl ?? ""),
      lat: Number(d.lat ?? 0),
      lng: Number(d.lng ?? 0),
      phone: String(d.phone ?? ""),
      maxDistanceKm: Number(d.maxDistanceKm ?? 10),
      deliveryProvider: d.deliveryProvider === "MOCK" ? "MOCK" : "LALAMOVE",
      schedule: {
        manualStatus: d.manualStatus ?? "OPEN",
        busyUntil: d.busyUntil?.toDate ? d.busyUntil.toDate() : null,
        weeklySchedule: d.weeklySchedule ?? {},
        specialDays: d.specialDays ?? [],
      },
    };
  }

  async getMenuItems(storeId: string, itemIds: string[]): Promise<Map<string, MenuItem>> {
    const uniqueIds = [...new Set(itemIds)];
    const result = new Map<string, MenuItem>();
    await Promise.all(
      uniqueIds.map(async (id) => {
        const snap = await db.collection("stores").doc(storeId).collection("menuItems").doc(id).get();
        if (!snap.exists) return;
        const d = snap.data()!;
        result.set(snap.id, {
          id: snap.id,
          categoryId: String(d.categoryId ?? ""),
          name: String(d.name ?? ""),
          basePrice: Number(d.basePrice ?? 0),
          isAvailable: Boolean(d.isAvailable),
          optionGroups: Array.isArray(d.optionGroups) ? d.optionGroups : [],
        });
      }),
    );
    return result;
  }
}
