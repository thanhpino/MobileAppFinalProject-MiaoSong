package vn.miaosong.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.OrderRepository
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.DeliveryProviderType
import vn.miaosong.core.model.DeliveryQuote
import vn.miaosong.core.model.DriverInfo
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.OrderDelivery
import vn.miaosong.core.model.OrderItemSnapshot
import vn.miaosong.core.model.OrderPayment
import vn.miaosong.core.model.OrderStatus
import vn.miaosong.core.model.PaymentMethod
import vn.miaosong.core.model.PaymentStatus
import vn.miaosong.core.model.PriceBreakdown
import vn.miaosong.core.model.RefundStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) : OrderRepository {

    override fun observeCustomerOrders(uid: String): Flow<List<Order>> = callbackFlow {
        val reg = firestore.collection("orders")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snap?.documents
                    ?.mapNotNull { it.toOrderOrNull() }
                    ?.sortedByDescending { it.createdAtEpochMs }
                    .orEmpty()
                trySend(orders)
            }
        awaitClose { reg.remove() }
    }

    override fun observeCustomerReviews(uid: String): Flow<Map<String, Int>> = callbackFlow {
        val reg = firestore.collection("reviews")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }
                val map = snap?.documents?.associate { doc ->
                    val orderId = doc.getString("orderId") ?: doc.id
                    val stars = (doc.getLong("stars") ?: 5L).toInt().coerceIn(1, 5)
                    orderId to stars
                }.orEmpty()
                trySend(map)
            }
        awaitClose { reg.remove() }
    }

    override fun observeStoreOrders(storeId: String): Flow<List<Order>> = callbackFlow {
        val reg = firestore.collection("orders")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snap?.documents
                    ?.mapNotNull { it.toOrderOrNull() }
                    ?.sortedByDescending { it.createdAtEpochMs }
                    .orEmpty()
                trySend(orders)
            }
        awaitClose { reg.remove() }
    }

    override fun observeOrder(orderId: String): Flow<Order?> = callbackFlow {
        val reg = firestore.collection("orders")
            .document(orderId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.toOrderOrNull())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getDeliveryQuote(
        storeId: String,
        dropoff: DeliveryAddress,
    ): AppResult<DeliveryQuote> {
        return try {
            val payload = mapOf(
                "storeId" to storeId,
                "dropoff" to mapOf(
                    "address" to dropoff.fullText,
                    "lat" to dropoff.lat,
                    "lng" to dropoff.lng,
                    "name" to dropoff.recipientName,
                    "phone" to dropoff.recipientPhone,
                ),
            )
            val result = functions.getHttpsCallable("getDeliveryQuote").call(payload).await()
            val root = result.getData() as? Map<*, *>
            val data = root?.get("data") as? Map<*, *>
                ?: return AppResult.Error("INVALID_RESPONSE", "Không nhận được báo giá giao hàng.")

            val providerStr = (data["provider"] as? String).orEmpty()
            val provider = runCatching { DeliveryProviderType.valueOf(providerStr) }
                .getOrDefault(DeliveryProviderType.LALAMOVE)

            AppResult.Success(
                DeliveryQuote(
                    quotationId = (data["quotationId"] as? String).orEmpty(),
                    provider = provider,
                    distanceKm = (data["distanceKm"] as? Number)?.toDouble() ?: 0.0,
                    shippingFee = (data["shippingFee"] as? Number)?.toLong() ?: 0L,
                    expiresAtIso = (data["expiresAtIso"] as? String).orEmpty(),
                ),
            )
        } catch (e: Exception) {
            AppResult.Error("QUOTE_FAILED", e.localizedMessage ?: "Không thể lấy báo giá giao hàng.")
        }
    }

    override suspend fun placeOrder(
        storeId: String,
        items: List<CartLine>,
        address: DeliveryAddress,
        quote: DeliveryQuote,
        voucherCode: String?,
        paymentMethod: PaymentMethod,
        idempotencyKey: String,
    ): AppResult<Order> {
        return try {
            val payload = mapOf(
                "idempotencyKey" to idempotencyKey,
                "storeId" to storeId,
                "address" to mapOf(
                    "label" to address.label,
                    "fullText" to address.fullText,
                    "lat" to address.lat,
                    "lng" to address.lng,
                    "recipientName" to address.recipientName,
                    "recipientPhone" to address.recipientPhone,
                    "deliveryNote" to address.note,
                ),
                "quotationId" to quote.quotationId,
                "shippingFee" to quote.shippingFee,
                "voucherCode" to voucherCode,
                "paymentMethod" to paymentMethod.name,
                "items" to items.map { line ->
                    mapOf(
                        "menuItemId" to line.menuItemId,
                        "sizeOptionId" to line.sizeOptionId,
                        "toppingOptionIds" to line.toppingOptionIds,
                        "note" to line.note,
                        "quantity" to line.quantity,
                    )
                },
            )
            val result = functions.getHttpsCallable("placeOrder").call(payload).await()
            val root = result.getData() as? Map<*, *>
            val data = root?.get("data") as? Map<*, *>
                ?: return AppResult.Error("INVALID_RESPONSE", "Đặt đơn thất bại.")

            val orderId = (data["orderId"] as? String).orEmpty()
            val snap = firestore.collection("orders").document(orderId).get().await()
            val order = snap.toOrderOrNull()
                ?: return AppResult.Error("ORDER_NOT_FOUND", "Không tải được đơn hàng vừa tạo.")
            AppResult.Success(order)
        } catch (e: Exception) {
            AppResult.Error("PLACE_ORDER_FAILED", e.localizedMessage ?: "Đặt đơn hàng thất bại.")
        }
    }

    override suspend fun confirmOrder(orderId: String): AppResult<Unit> =
        callUnit("confirmOrder", mapOf("orderId" to orderId))

    override suspend fun rejectOrder(
        orderId: String,
        reasonCode: String,
        reasonText: String,
    ): AppResult<Unit> = callUnit(
        "rejectOrder",
        mapOf(
            "orderId" to orderId,
            "reasonCode" to reasonCode,
            "reasonText" to reasonText,
        ),
    )

    override suspend fun markReady(orderId: String): AppResult<Unit> =
        callUnit("markReady", mapOf("orderId" to orderId))

    override suspend fun dispatchDriver(orderId: String): AppResult<Unit> =
        callUnit("dispatchDriver", mapOf("orderId" to orderId))

    override suspend fun mockDeliveryAdvance(
        orderId: String,
        targetStatus: OrderStatus,
    ): AppResult<Unit> = callUnit(
        "mockDeliveryAdvance",
        mapOf(
            "orderId" to orderId,
            "targetStatus" to targetStatus.name,
        ),
    )

    override suspend fun submitReview(
        orderId: String,
        stars: Int,
        tags: List<String>,
        comment: String,
        imageBytes: ByteArray?,
    ): AppResult<Unit> {
        var uploadedUrl = ""
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                runCatching {
                    val compressed = compressReviewPhoto(imageBytes)
                    val ref = storage.reference.child("users/$uid/reviews/$orderId.jpg")
                    val meta = StorageMetadata.Builder().setContentType("image/jpeg").build()
                    ref.putBytes(compressed, meta).await()
                    uploadedUrl = ref.downloadUrl.await().toString()
                }
            }
        }
        return callUnit(
            "submitReview",
            mapOf(
                "orderId" to orderId,
                "stars" to stars,
                "tags" to tags,
                "comment" to comment.trim(),
                "imageUrl" to uploadedUrl,
            ),
        )
    }

    private fun compressReviewPhoto(rawBytes: ByteArray): ByteArray {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes
        val maxDim = 900
        val longest = max(decoded.width, decoded.height)
        val scaled = if (longest > maxDim) {
            val ratio = maxDim.toFloat() / longest.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).roundToInt().coerceAtLeast(1),
                (decoded.height * ratio).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return out.toByteArray()
    }

    private suspend fun callUnit(name: String, payload: Map<String, Any?>): AppResult<Unit> {
        return try {
            functions.getHttpsCallable(name).call(payload).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(
                code = "${name.uppercase()}_FAILED",
                message = e.localizedMessage ?: "Thao tác đơn hàng thất bại.",
            )
        }
    }

    private fun DocumentSnapshot.toOrderOrNull(): Order? {
        if (!exists()) return null
        val pricingMap = get("pricing") as? Map<*, *>
        val pricing = PriceBreakdown(
            itemsTotal = (pricingMap?.get("itemsTotal") as? Number)?.toLong() ?: 0L,
            toppingsTotal = (pricingMap?.get("toppingsTotal") as? Number)?.toLong() ?: 0L,
            subtotal = (pricingMap?.get("subtotal") as? Number)?.toLong() ?: 0L,
            discount = (pricingMap?.get("discount") as? Number)?.toLong() ?: 0L,
            shippingFee = (pricingMap?.get("shippingFee") as? Number)?.toLong() ?: 0L,
            total = (pricingMap?.get("total") as? Number)?.toLong() ?: 0L,
        )

        val addrMap = get("address") as? Map<*, *>
        val address = DeliveryAddress(
            label = (addrMap?.get("label") as? String).orEmpty(),
            fullText = (addrMap?.get("fullText") as? String).orEmpty(),
            lat = (addrMap?.get("lat") as? Number)?.toDouble() ?: 0.0,
            lng = (addrMap?.get("lng") as? Number)?.toDouble() ?: 0.0,
            recipientName = (addrMap?.get("recipientName") as? String).orEmpty(),
            recipientPhone = (addrMap?.get("recipientPhone") as? String).orEmpty(),
            note = (addrMap?.get("deliveryNote") as? String)
                ?: (addrMap?.get("note") as? String).orEmpty(),
        )

        val payMap = get("payment") as? Map<*, *>
        val payment = OrderPayment(
            method = runCatching {
                PaymentMethod.valueOf((payMap?.get("method") as? String) ?: "COD")
            }.getOrDefault(PaymentMethod.COD),
            status = runCatching {
                PaymentStatus.valueOf((payMap?.get("status") as? String) ?: "UNPAID")
            }.getOrDefault(PaymentStatus.UNPAID),
            amountVnd = (payMap?.get("amountVnd") as? Number)?.toLong() ?: pricing.total,
            appTransId = payMap?.get("appTransId") as? String,
            paymentUrl = payMap?.get("paymentUrl") as? String,
        )

        val delivMap = get("delivery") as? Map<*, *>
        val driverMap = delivMap?.get("driver") as? Map<*, *>
        val driver = if (driverMap != null) {
            DriverInfo(
                name = (driverMap["name"] as? String).orEmpty(),
                phone = (driverMap["phone"] as? String).orEmpty(),
                plate = (driverMap["plate"] as? String).orEmpty(),
            )
        } else {
            null
        }
        val delivery = OrderDelivery(
            provider = runCatching {
                DeliveryProviderType.valueOf((delivMap?.get("provider") as? String) ?: "LALAMOVE")
            }.getOrDefault(DeliveryProviderType.LALAMOVE),
            quotationId = (delivMap?.get("quotationId") as? String).orEmpty(),
            externalId = delivMap?.get("externalId") as? String,
            driver = driver,
            shareLink = delivMap?.get("shareLink") as? String,
        )

        val rawItems = get("items") as? List<*>
        val items = rawItems?.mapNotNull { entry ->
            val m = entry as? Map<*, *> ?: return@mapNotNull null
            OrderItemSnapshot(
                lineKey = (m["lineKey"] as? String).orEmpty(),
                menuItemId = (m["menuItemId"] as? String).orEmpty(),
                itemName = (m["itemName"] as? String).orEmpty(),
                sizeOptionId = m["sizeOptionId"] as? String,
                sizeName = (m["sizeName"] as? String).orEmpty(),
                toppingOptionIds = (m["toppingOptionIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                toppingNames = (m["toppingNames"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                note = (m["note"] as? String).orEmpty(),
                unitPrice = (m["unitPrice"] as? Number)?.toLong() ?: 0L,
                quantity = (m["quantity"] as? Number)?.toInt() ?: 1,
            )
        }.orEmpty()

        val status = runCatching {
            OrderStatus.valueOf(getString("status") ?: "PLACED")
        }.getOrDefault(OrderStatus.PLACED)

        val refundStatus = runCatching {
            RefundStatus.valueOf(getString("refundStatus") ?: "NONE")
        }.getOrDefault(RefundStatus.NONE)

        val createdAtMs = when (val ts = get("createdAt")) {
            is Timestamp -> ts.toDate().time
            is Number -> ts.toLong()
            else -> 0L
        }

        return Order(
            id = id,
            code = getString("code") ?: id.take(8).uppercase(),
            customerId = getString("customerId").orEmpty(),
            storeId = getString("storeId").orEmpty(),
            storeName = getString("storeName").orEmpty(),
            items = items,
            pricing = pricing,
            voucherCode = getString("voucherCode"),
            address = address,
            payment = payment,
            refundStatus = refundStatus,
            status = status,
            delivery = delivery,
            rejectReason = getString("rejectReason"),
            createdAtEpochMs = createdAtMs,
        )
    }
}
