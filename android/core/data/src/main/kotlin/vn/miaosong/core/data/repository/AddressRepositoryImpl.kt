package vn.miaosong.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.domain.AddressRepository
import vn.miaosong.core.model.DeliveryAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : AddressRepository {

    private fun addressesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("addresses")

    override fun observeAddresses(uid: String): Flow<List<DeliveryAddress>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = addressesCollection(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AddressDto::class.java)?.toModel(doc.id)
                }.orEmpty()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addAddress(uid: String, address: DeliveryAddress): AppResult<String> {
        return try {
            val coll = addressesCollection(uid)
            val docRef = coll.document()
            val id = docRef.id

            if (address.isDefault) {
                // Tắt isDefault của các địa chỉ hiện tại
                val currentDocs = coll.whereEqualTo("isDefault", true).get().await()
                val batch = firestore.batch()
                currentDocs.documents.forEach { doc ->
                    batch.update(doc.reference, "isDefault", false)
                }
                batch.set(docRef, address.copy(id = id).toDto())
                batch.commit().await()
            } else {
                docRef.set(address.copy(id = id).toDto()).await()
            }

            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error("ADDRESS_ADD_FAILED", e.localizedMessage ?: "Thêm địa chỉ thất bại.")
        }
    }

    override suspend fun updateAddress(uid: String, address: DeliveryAddress): AppResult<Unit> {
        return try {
            val coll = addressesCollection(uid)
            val docRef = coll.document(address.id)

            if (address.isDefault) {
                val currentDocs = coll.whereEqualTo("isDefault", true).get().await()
                val batch = firestore.batch()
                currentDocs.documents.forEach { doc ->
                    if (doc.id != address.id) {
                        batch.update(doc.reference, "isDefault", false)
                    }
                }
                batch.set(docRef, address.toDto())
                batch.commit().await()
            } else {
                docRef.set(address.toDto()).await()
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("ADDRESS_UPDATE_FAILED", e.localizedMessage ?: "Cập nhật địa chỉ thất bại.")
        }
    }

    override suspend fun deleteAddress(uid: String, addressId: String): AppResult<Unit> {
        return try {
            addressesCollection(uid).document(addressId).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("ADDRESS_DELETE_FAILED", e.localizedMessage ?: "Xóa địa chỉ thất bại.")
        }
    }

    override suspend fun setDefaultAddress(uid: String, addressId: String): AppResult<Unit> {
        return try {
            val coll = addressesCollection(uid)
            val allDocs = coll.get().await()
            val batch = firestore.batch()
            allDocs.documents.forEach { doc ->
                batch.update(doc.reference, "isDefault", doc.id == addressId)
            }
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("ADDRESS_SET_DEFAULT_FAILED", e.localizedMessage ?: "Đặt mặc định thất bại.")
        }
    }
}

private data class AddressDto(
    val label: String = "",
    val fullText: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val recipientName: String = "",
    val recipientPhone: String = "",
    val note: String = "",
    val isDefault: Boolean = false,
) {
    fun toModel(id: String) = DeliveryAddress(
        id = id,
        label = label,
        fullText = fullText,
        lat = lat,
        lng = lng,
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        note = note,
        isDefault = isDefault,
    )
}

private fun DeliveryAddress.toDto() = AddressDto(
    label = label,
    fullText = fullText,
    lat = lat,
    lng = lng,
    recipientName = recipientName,
    recipientPhone = recipientPhone,
    note = note,
    isDefault = isDefault,
)
