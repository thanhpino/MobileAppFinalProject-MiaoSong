package vn.miaosong.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.MerchantCatalogRepository
import vn.miaosong.core.model.ManualStoreStatus
import vn.miaosong.core.model.MenuCategory
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.SpecialDay
import vn.miaosong.core.model.TimeSlot
import vn.miaosong.core.model.Voucher
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class MerchantCatalogRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : MerchantCatalogRepository {

    private fun storeDoc(storeId: String) = firestore.collection("stores").document(storeId)

    override suspend fun upsertCategory(storeId: String, category: MenuCategory): AppResult<Unit> = runCatchingResult {
        val docId = category.id.ifBlank { "cat_${UUID.randomUUID().toString().take(8)}" }
        storeDoc(storeId).collection("categories").document(docId).set(
            mapOf(
                "name" to category.name.trim(),
                "sortOrder" to category.sortOrder,
            ),
        ).await()
    }

    override suspend fun deleteCategory(storeId: String, categoryId: String): AppResult<Unit> = runCatchingResult {
        storeDoc(storeId).collection("categories").document(categoryId).delete().await()
    }

    override suspend fun upsertMenuItem(storeId: String, item: MenuItem): AppResult<Unit> = runCatchingResult {
        val docId = item.id.ifBlank { "item_${UUID.randomUUID().toString().take(8)}" }
        val groupsPayload = item.optionGroups.map { g ->
            mapOf(
                "id" to g.id,
                "name" to g.name,
                "type" to g.type.name,
                "required" to g.required,
                "min" to g.min,
                "max" to g.max,
                "options" to g.options.map { o ->
                    mapOf(
                        "id" to o.id,
                        "name" to o.name,
                        "priceDelta" to o.priceDelta,
                        "isAvailable" to o.isAvailable,
                    )
                },
            )
        }
        storeDoc(storeId).collection("menuItems").document(docId).set(
            mapOf(
                "categoryId" to item.categoryId,
                "name" to item.name.trim(),
                "description" to item.description.trim(),
                "imageUrl" to item.imageUrl,
                "basePrice" to item.basePrice,
                "isAvailable" to item.isAvailable,
                "optionGroups" to groupsPayload,
            ),
        ).await()
    }

    override suspend fun toggleMenuItemAvailability(
        storeId: String,
        itemId: String,
        isAvailable: Boolean,
    ): AppResult<Unit> = runCatchingResult {
        storeDoc(storeId).collection("menuItems").document(itemId)
            .update("isAvailable", isAvailable)
            .await()
    }

    override suspend fun deleteMenuItem(storeId: String, itemId: String): AppResult<Unit> = runCatchingResult {
        storeDoc(storeId).collection("menuItems").document(itemId).delete().await()
    }

    override suspend fun upsertVoucher(storeId: String, voucher: Voucher): AppResult<Unit> = runCatchingResult {
        val docId = voucher.id.ifBlank { "v_${voucher.code.trim().lowercase()}" }
        storeDoc(storeId).collection("vouchers").document(docId).set(
            mapOf(
                "code" to voucher.code.trim().uppercase(),
                "type" to voucher.type.name,
                "value" to voucher.value,
                "maxDiscount" to voucher.maxDiscount,
                "minOrder" to voucher.minOrder,
                "startAt" to Timestamp(Date(voucher.startAtEpochMs)),
                "endAt" to Timestamp(Date(voucher.endAtEpochMs)),
                "usageLimit" to voucher.usageLimit,
                "usedCount" to voucher.usedCount,
                "perUserLimit" to voucher.perUserLimit,
                "isActive" to voucher.isActive,
            ),
        ).await()
    }

    override suspend fun toggleVoucherActive(
        storeId: String,
        voucherId: String,
        isActive: Boolean,
    ): AppResult<Unit> = runCatchingResult {
        storeDoc(storeId).collection("vouchers").document(voucherId)
            .update("isActive", isActive)
            .await()
    }

    override suspend fun deleteVoucher(storeId: String, voucherId: String): AppResult<Unit> = runCatchingResult {
        storeDoc(storeId).collection("vouchers").document(voucherId).delete().await()
    }

    override suspend fun updateStoreStatus(
        storeId: String,
        status: ManualStoreStatus,
        busyUntilEpochMs: Long?,
    ): AppResult<Unit> = runCatchingResult {
        storeDoc(storeId).update(
            mapOf(
                "manualStatus" to status.name,
                "busyUntil" to busyUntilEpochMs?.let { Timestamp(Date(it)) },
            ),
        ).await()
    }

    override suspend fun updateStoreInfo(
        storeId: String,
        name: String,
        address: String,
        phone: String,
    ): AppResult<Unit> = runCatchingResult {
        storeDoc(storeId).update(
            mapOf(
                "name" to name.trim(),
                "address" to address.trim(),
                "phone" to phone.trim(),
            ),
        ).await()
    }

    override suspend fun updateStoreSchedule(
        storeId: String,
        weeklySchedule: Map<String, List<TimeSlot>>,
        specialDays: List<SpecialDay>,
    ): AppResult<Unit> = runCatchingResult {
        val weeklyPayload = weeklySchedule.mapValues { (_, slots) ->
            slots.map { mapOf("open" to it.open, "close" to it.close) }
        }
        val specialPayload = specialDays.map { d ->
            mapOf(
                "date" to d.date,
                "isClosed" to d.isClosed,
                "slots" to d.slots.map { mapOf("open" to it.open, "close" to it.close) },
            )
        }
        storeDoc(storeId).update(
            mapOf(
                "weeklySchedule" to weeklyPayload,
                "specialDays" to specialPayload,
            ),
        ).await()
    }

    override suspend fun uploadStoreAvatar(storeId: String, imageBytes: ByteArray): AppResult<String> {
        return try {
            val optimizedBytes = compressImageIfNeeded(imageBytes)
            val ref = storage.reference.child("stores/$storeId/avatar.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            ref.putBytes(optimizedBytes, metadata).await()
            val url = ref.downloadUrl.await().toString()
            storeDoc(storeId).update("avatarUrl", url).await()
            AppResult.Success(url)
        } catch (e: Exception) {
            AppResult.Error(
                code = "UPLOAD_FAILED",
                message = e.localizedMessage ?: "Tải ảnh đại diện quán thất bại (hãy đăng nhập tài khoản Quán).",
            )
        }
    }

    override suspend fun uploadMenuItemImage(
        storeId: String,
        itemId: String,
        imageBytes: ByteArray,
    ): AppResult<String> {
        return try {
            val optimizedBytes = compressImageIfNeeded(imageBytes)
            val ref = storage.reference.child("stores/$storeId/items/$itemId.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            ref.putBytes(optimizedBytes, metadata).await()
            val url = ref.downloadUrl.await().toString()
            storeDoc(storeId).collection("menuItems").document(itemId).update("imageUrl", url).await()
            AppResult.Success(url)
        } catch (e: Exception) {
            AppResult.Error(
                code = "UPLOAD_FAILED",
                message = e.localizedMessage ?: "Tải ảnh món ăn thất bại (hãy đăng nhập tài khoản Quán).",
            )
        }
    }

    private fun compressImageIfNeeded(rawBytes: ByteArray): ByteArray {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes
        val maxDim = 800
        val longestSide = max(decoded.width, decoded.height)
        val scaled = if (longestSide > maxDim) {
            val ratio = maxDim.toFloat() / longestSide.toFloat()
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

    private suspend inline fun runCatchingResult(block: suspend () -> Unit): AppResult<Unit> {
        return try {
            block()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(
                code = "FIRESTORE_WRITE_FAILED",
                message = e.localizedMessage ?: "Thao tác thất bại. Hãy chắc chắn bạn đã đăng nhập tài khoản Quán.",
            )
        }
    }
}
