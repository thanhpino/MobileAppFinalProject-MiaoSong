package vn.miaosong.core.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import vn.miaosong.core.domain.StoreRepository
import vn.miaosong.core.model.DeliveryProviderType
import vn.miaosong.core.model.ManualStoreStatus
import vn.miaosong.core.model.MenuCategory
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.MenuOption
import vn.miaosong.core.model.OptionGroup
import vn.miaosong.core.model.OptionGroupType
import vn.miaosong.core.model.Review
import vn.miaosong.core.model.SpecialDay
import vn.miaosong.core.model.Store
import vn.miaosong.core.model.StoreSchedule
import vn.miaosong.core.model.TimeSlot
import vn.miaosong.core.model.Voucher
import vn.miaosong.core.model.VoucherType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StoreRepository {

    override fun observeStores(): Flow<List<Store>> = callbackFlow {
        val reg = firestore.collection("stores")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val stores = snap?.documents?.mapNotNull { it.toStore() } ?: emptyList()
                trySend(stores)
            }
        awaitClose { reg.remove() }
    }

    override fun observeStore(storeId: String): Flow<Store?> = callbackFlow {
        val reg = firestore.collection("stores").document(storeId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.takeIf { it.exists() }?.toStore())
            }
        awaitClose { reg.remove() }
    }

    override fun observeCategories(storeId: String): Flow<List<MenuCategory>> = callbackFlow {
        val reg = firestore.collection("stores").document(storeId)
            .collection("categories")
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { doc ->
                    MenuCategory(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt(),
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override fun observeMenuItems(storeId: String): Flow<List<MenuItem>> = callbackFlow {
        val reg = firestore.collection("stores").document(storeId)
            .collection("menuItems")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toMenuItem() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override fun observeVouchers(storeId: String): Flow<List<Voucher>> = callbackFlow {
        val reg = firestore.collection("stores").document(storeId)
            .collection("vouchers")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toVoucher() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override fun observeStoreReviews(storeId: String): Flow<List<Review>> = callbackFlow {
        val reg = firestore.collection("reviews")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    Review(
                        orderId = doc.getString("orderId") ?: doc.id,
                        storeId = doc.getString("storeId") ?: storeId,
                        customerId = doc.getString("customerId").orEmpty(),
                        stars = (doc.getLong("stars") ?: 5L).toInt(),
                        tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                        comment = doc.getString("comment").orEmpty(),
                        createdAtEpochMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }?.sortedByDescending { it.createdAtEpochMs }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toStore(): Store? {
        val name = getString("name") ?: return null
        val manualStatus = when (getString("manualStatus")) {
            "BUSY" -> ManualStoreStatus.BUSY
            "CLOSED" -> ManualStoreStatus.CLOSED
            else -> ManualStoreStatus.OPEN
        }
        val provider = if (getString("deliveryProvider") == "MOCK") {
            DeliveryProviderType.MOCK
        } else {
            DeliveryProviderType.LALAMOVE
        }

        val weeklyRaw = get("weeklySchedule") as? Map<String, Any?> ?: emptyMap()
        val weeklySchedule = weeklyRaw.mapValues { (_, v) ->
            val arr = v as? List<Map<String, Any?>> ?: emptyList()
            arr.mapNotNull { slot ->
                val open = slot["open"] as? String ?: return@mapNotNull null
                val close = slot["close"] as? String ?: return@mapNotNull null
                TimeSlot(open = open, close = close)
            }
        }

        val specialRaw = get("specialDays") as? List<Map<String, Any?>> ?: emptyList()
        val specialDays = specialRaw.mapNotNull { d ->
            val date = d["date"] as? String ?: return@mapNotNull null
            SpecialDay(
                date = date,
                isClosed = d["isClosed"] as? Boolean ?: true,
            )
        }

        return Store(
            id = id,
            name = name,
            slug = getString("slug").orEmpty(),
            address = getString("address").orEmpty(),
            avatarUrl = getString("avatarUrl").orEmpty(),
            lat = getDouble("lat") ?: 0.0,
            lng = getDouble("lng") ?: 0.0,
            phone = getString("phone").orEmpty(),
            maxDistanceKm = getDouble("maxDistanceKm") ?: (getLong("maxDistanceKm")?.toDouble() ?: 10.0),
            deliveryProvider = provider,
            schedule = StoreSchedule(
                manualStatus = manualStatus,
                busyUntilEpochMs = getTimestamp("busyUntil")?.toDate()?.time,
                weeklySchedule = weeklySchedule,
                specialDays = specialDays,
            ),
            ratingAvg = getDouble("ratingAvg") ?: 0.0,
            ratingCount = (getLong("ratingCount") ?: 0L).toInt(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toMenuItem(): MenuItem? {
        val name = getString("name") ?: return null
        val rawGroups = get("optionGroups") as? List<Map<String, Any?>> ?: emptyList()
        val groups = rawGroups.mapNotNull { g ->
            val gid = g["id"] as? String ?: return@mapNotNull null
            val gname = g["name"] as? String ?: return@mapNotNull null
            val gtype = if (g["type"] == "MULTI") OptionGroupType.MULTI else OptionGroupType.SINGLE
            val rawOpts = g["options"] as? List<Map<String, Any?>> ?: emptyList()
            val opts = rawOpts.mapNotNull { o ->
                val oid = o["id"] as? String ?: return@mapNotNull null
                val oname = o["name"] as? String ?: return@mapNotNull null
                MenuOption(
                    id = oid,
                    name = oname,
                    priceDelta = (o["priceDelta"] as? Number)?.toLong() ?: 0L,
                    isAvailable = o["isAvailable"] as? Boolean ?: true,
                )
            }
            OptionGroup(
                id = gid,
                name = gname,
                type = gtype,
                required = g["required"] as? Boolean ?: false,
                min = (g["min"] as? Number)?.toInt() ?: 0,
                max = (g["max"] as? Number)?.toInt() ?: 1,
                options = opts,
            )
        }

        return MenuItem(
            id = id,
            categoryId = getString("categoryId").orEmpty(),
            name = name,
            description = getString("description").orEmpty(),
            imageUrl = getString("imageUrl").orEmpty(),
            basePrice = getLong("basePrice") ?: 0L,
            isAvailable = getBoolean("isAvailable") ?: true,
            optionGroups = groups,
        )
    }

    private fun DocumentSnapshot.toVoucher(): Voucher? {
        val code = getString("code") ?: return null
        val type = if (getString("type") == "PERCENT") VoucherType.PERCENT else VoucherType.FIXED
        return Voucher(
            id = id,
            code = code,
            type = type,
            value = getLong("value") ?: 0L,
            maxDiscount = getLong("maxDiscount"),
            minOrder = getLong("minOrder") ?: 0L,
            startAtEpochMs = getTimestamp("startAt")?.toDate()?.time ?: 0L,
            endAtEpochMs = getTimestamp("endAt")?.toDate()?.time ?: Long.MAX_VALUE,
            usageLimit = (getLong("usageLimit") ?: 0L).toInt(),
            usedCount = (getLong("usedCount") ?: 0L).toInt(),
            perUserLimit = (getLong("perUserLimit") ?: 1L).toInt(),
            isActive = getBoolean("isActive") ?: true,
        )
    }
}
