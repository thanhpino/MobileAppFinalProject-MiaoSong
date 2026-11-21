package vn.miaosong.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import vn.miaosong.core.data.local.CartDao
import vn.miaosong.core.data.local.CartLineEntity
import vn.miaosong.core.domain.CartRepository
import vn.miaosong.core.model.CartLine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val cartDao: CartDao,
) : CartRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeCart(): Flow<List<CartLine>> =
        cartDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertLine(line: CartLine) {
        val existing = cartDao.getByKey(line.lineKey)
        if (existing != null) {
            cartDao.updateQuantity(line.lineKey, existing.quantity + line.quantity)
        } else {
            cartDao.upsert(line.toEntity())
        }
    }

    override suspend fun updateQuantity(lineKey: String, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteByKey(lineKey)
        } else {
            cartDao.updateQuantity(lineKey, quantity)
        }
    }

    override suspend fun removeLine(lineKey: String) {
        cartDao.deleteByKey(lineKey)
    }

    override suspend fun clear() {
        cartDao.clearAll()
    }

    private fun CartLine.toEntity() = CartLineEntity(
        lineKey = lineKey,
        storeId = storeId,
        menuItemId = menuItemId,
        itemName = itemName,
        imageUrl = imageUrl,
        basePrice = basePrice,
        sizeOptionId = sizeOptionId,
        sizeName = sizeName,
        sizeDelta = sizeDelta,
        toppingOptionIdsJson = json.encodeToString(ListSerializer(String.serializer()), toppingOptionIds),
        toppingNamesJson = json.encodeToString(ListSerializer(String.serializer()), toppingNames),
        toppingsDeltaJson = json.encodeToString(ListSerializer(Long.serializer()), toppingsDelta),
        note = note,
        quantity = quantity,
    )

    private fun CartLineEntity.toDomain() = CartLine(
        lineKey = lineKey,
        storeId = storeId,
        menuItemId = menuItemId,
        itemName = itemName,
        imageUrl = imageUrl,
        basePrice = basePrice,
        sizeOptionId = sizeOptionId,
        sizeName = sizeName,
        sizeDelta = sizeDelta,
        toppingOptionIds = json.decodeFromString(ListSerializer(String.serializer()), toppingOptionIdsJson),
        toppingNames = json.decodeFromString(ListSerializer(String.serializer()), toppingNamesJson),
        toppingsDelta = json.decodeFromString(ListSerializer(Long.serializer()), toppingsDeltaJson),
        note = note,
        quantity = quantity,
    )
}
