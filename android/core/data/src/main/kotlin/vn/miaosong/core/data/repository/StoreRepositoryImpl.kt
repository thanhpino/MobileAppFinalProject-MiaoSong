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

private const val AVATAR_BUN_RIEU_CO_BA =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuDLEGTUDj1U_Yg11MfTv01vY_3AF_TF1ilrOTa0QYJDXkEjB2s9pLTEReU_XCfyVoTyXJo0-kGTiFUK-NolEEMFXJUUkBnNP63g97dPZEVofOtqWwywcKNYST6_QkTJ9nZoAxUuCD7YrBZLfgU4NZVVeXQnRpx6EdyIbVSI4sxXHPTmWpJlM_5zc3rC4k10r3U8w22Gw-jC7ahY7tLQ_DZMT3xl8an6mM7CBAleZ5Q"

private const val AVATAR_BUN_CHA_CA =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCwcq0m3nINlmuB2KG0iqONcr9xO1cdiAGHK9vo-O-DQWz1pnjK0Nu1_RIQwaVrBh9N7xhlx-o16KMQ8IozPwPuN1tkWXvtlbDfpGAv3bN_sKGDLCzEG_TUd7lWBGFli6mMqC0ETSO8HPa_A2NHYPdjq_bQy7kT7x38PQ-g6z1OWiJFIg-jOuYMnR76klNR4TsQEmpDN5U8D2UmfWcqbB_gh9OgivjXWr7YoJeu7eM"

private const val AVATAR_COM_TAM =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCEBiL4AIGbHXLkxb8FuNt105Uiyo-yCjJyl_aYulwSvznF-WMFeATEfdS1BewJT_79vsXkamcjtjFm0k2Ol7x6fXhfkgnIMzflDM0dKyATESjoncv2YYyc6QC450rFQNkm9JKJzwazUcVsWTEHq1RVcnD1rtqGRfjBs0UXwuYbWp3dXtgtmUXqeiq0UyytMCp0FtcG-zAIFAi7WD6KIQWPjosxlrPeicLA4CUY3wU"

private const val AVATAR_DRINKS =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCSgFyvurAxNBhD3FUkp4d0APxADrWGjNc9X_5_AOXQjP4TwbH_wdpsFm6aVPUBhWoLH_fdK8ceBr-87CWNz5uBcTTq38rA02dXH962HqoGod8HHChH1Q4DycVg82qLHCpeKc1Y_jq7X-OJFCTzK7rJrQtImY9ZbDijQ5HhQIosKRSFocPs4hKiUka1dvlVvz35YVbwUFx9EN3FYhhySWx9iXe-GbmLhRnRzd-P1iE"

private const val AVATAR_PHO_BO =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuBBVh_q7NEC4Dm6gzcSsG3DmZX0eOm-fgSIC4fA1_nj6SMhyv621X-N2CTGYkfYoLFKxTe8yr0L6BJT8Xmkd40lu00vCD-bOz7Qjk5s7jH5TVOikXs9Eu6PhGVnsFY_xBY1r8n2_cz7hIaPUXnFSir28sX50aWOlizhfdSiY_T9YYKO-xBcKKNqOR05OJMAPPrQevEJaLXqsuG5wc4v7VWjS_mEhROqPQqfR_q211A"

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
                val store = snap?.takeIf { it.exists() }?.toStore() ?: getFallbackStore(storeId)
                trySend(store)
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
                        imageUrl = doc.getString("imageUrl").orEmpty(),
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

        val slug = getString("slug").orEmpty()
        val rawAvatar = getString("avatarUrl").orEmpty()
        val avatarUrl = rawAvatar.ifBlank {
            resolveStoreAvatar(name = name, slug = slug)
        }

        return Store(
            id = id,
            name = name,
            slug = slug,
            address = getString("address").orEmpty(),
            avatarUrl = avatarUrl,
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

    private fun resolveStoreAvatar(name: String, slug: String): String {
        val lower = (name + " " + slug).lowercase()
        return when {
            lower.contains("cha-ca") || lower.contains("chả cá") || lower.contains("ca vien") ->
                AVATAR_BUN_CHA_CA
            lower.contains("com") || lower.contains("cơm") || lower.contains("suon") || lower.contains("sườn") ->
                AVATAR_COM_TAM
            lower.contains("tra") || lower.contains("trà") || lower.contains("ca-phe") || lower.contains("cà phê") || lower.contains("coffee") || lower.contains("tea") || lower.contains("sua") || lower.contains("sữa") ->
                AVATAR_DRINKS
            lower.contains("pho") || lower.contains("phở") || lower.contains("bo") || lower.contains("bò") ->
                AVATAR_PHO_BO
            lower.contains("rieu") || lower.contains("riêu") || lower.contains("co ba") || lower.contains("cô ba") ->
                AVATAR_BUN_RIEU_CO_BA
            else ->
                AVATAR_BUN_RIEU_CO_BA
        }
    }

    private fun getFallbackStore(storeId: String): Store? {
        return when (storeId) {
            "store_bun_cha_ca_nt" -> Store(
                id = "store_bun_cha_ca_nt",
                name = "Bún Chả Cá Nha Trang",
                slug = "bun-cha-ca-nha-trang",
                address = "112 Lê Văn Lương, Tân Kiểng, Quận 7, TP.HCM",
                avatarUrl = AVATAR_BUN_CHA_CA,
                lat = 10.7421,
                lng = 106.7042,
                phone = "+84909333444",
                maxDistanceKm = 10.0,
                deliveryProvider = DeliveryProviderType.LALAMOVE,
                schedule = StoreSchedule(manualStatus = ManualStoreStatus.OPEN),
                ratingAvg = 4.8,
                ratingCount = 86,
            )
            "store_com_tam_ba_ghien" -> Store(
                id = "store_com_tam_ba_ghien",
                name = "Cơm Tấm Ba Ghiền — Sườn Bì Chả",
                slug = "com-tam-ba-ghien",
                address = "84 Đặng Văn Ngữ, Phường 10, Phú Nhuận, TP.HCM",
                avatarUrl = AVATAR_COM_TAM,
                lat = 10.7932,
                lng = 106.6715,
                phone = "+84908555666",
                maxDistanceKm = 12.0,
                deliveryProvider = DeliveryProviderType.MOCK,
                schedule = StoreSchedule(manualStatus = ManualStoreStatus.OPEN),
                ratingAvg = 4.9,
                ratingCount = 340,
            )
            "store_tra_sua_miaotea" -> Store(
                id = "store_tra_sua_miaotea",
                name = "Trà Sữa & Cà Phê MiaoTea",
                slug = "tra-sua-miaotea",
                address = "65 Đường D1, Tân Hưng, Quận 7, TP.HCM",
                avatarUrl = AVATAR_DRINKS,
                lat = 10.7485,
                lng = 106.6980,
                phone = "+84909777888",
                maxDistanceKm = 8.0,
                deliveryProvider = DeliveryProviderType.MOCK,
                schedule = StoreSchedule(manualStatus = ManualStoreStatus.OPEN),
                ratingAvg = 4.7,
                ratingCount = 195,
            )
            "store_bun_rieu_co_ba" -> Store(
                id = "store_bun_rieu_co_ba",
                name = "Bún Riêu Cô Ba",
                slug = "bun-rieu-co-ba",
                address = "420 Nguyễn Thị Thập, Tân Quy, Quận 7, TP.HCM",
                avatarUrl = AVATAR_BUN_RIEU_CO_BA,
                lat = 10.7385,
                lng = 106.7124,
                phone = "+84909111222",
                maxDistanceKm = 10.0,
                deliveryProvider = DeliveryProviderType.MOCK,
                schedule = StoreSchedule(manualStatus = ManualStoreStatus.OPEN),
                ratingAvg = 4.9,
                ratingCount = 128,
            )
            else -> null
        }
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
