package vn.miaosong.core.domain

import kotlinx.coroutines.flow.Flow
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.DeliveryQuote
import vn.miaosong.core.model.ManualStoreStatus
import vn.miaosong.core.model.MenuCategory
import vn.miaosong.core.model.MenuItem
import vn.miaosong.core.model.Order
import vn.miaosong.core.model.PaymentMethod
import vn.miaosong.core.model.Store
import vn.miaosong.core.model.UserProfile
import vn.miaosong.core.model.Voucher

interface AuthRepository {
    fun observeCurrentUser(): Flow<UserProfile?>
    suspend fun signInWithEmail(email: String, password: String): AppResult<UserProfile>
    suspend fun signOut()
}

interface StoreRepository {
    fun observeStores(): Flow<List<Store>>
    fun observeStore(storeId: String): Flow<Store?>
    fun observeCategories(storeId: String): Flow<List<MenuCategory>>
    fun observeMenuItems(storeId: String): Flow<List<MenuItem>>
    fun observeVouchers(storeId: String): Flow<List<Voucher>>
}

interface MerchantCatalogRepository {
    suspend fun upsertCategory(storeId: String, category: MenuCategory): AppResult<Unit>
    suspend fun deleteCategory(storeId: String, categoryId: String): AppResult<Unit>
    suspend fun upsertMenuItem(storeId: String, item: MenuItem): AppResult<Unit>
    suspend fun toggleMenuItemAvailability(storeId: String, itemId: String, isAvailable: Boolean): AppResult<Unit>
    suspend fun deleteMenuItem(storeId: String, itemId: String): AppResult<Unit>
    suspend fun upsertVoucher(storeId: String, voucher: Voucher): AppResult<Unit>
    suspend fun toggleVoucherActive(storeId: String, voucherId: String, isActive: Boolean): AppResult<Unit>
    suspend fun deleteVoucher(storeId: String, voucherId: String): AppResult<Unit>
    suspend fun updateStoreStatus(storeId: String, status: ManualStoreStatus, busyUntilEpochMs: Long?): AppResult<Unit>
    suspend fun uploadStoreAvatar(storeId: String, imageBytes: ByteArray): AppResult<String>
    suspend fun uploadMenuItemImage(storeId: String, itemId: String, imageBytes: ByteArray): AppResult<String>
}

interface CartRepository {
    fun observeCart(): Flow<List<CartLine>>
    suspend fun upsertLine(line: CartLine)
    suspend fun updateQuantity(lineKey: String, quantity: Int)
    suspend fun removeLine(lineKey: String)
    suspend fun clear()
}

interface OrderRepository {
    fun observeCustomerOrders(uid: String): Flow<List<Order>>
    fun observeStoreOrders(storeId: String): Flow<List<Order>>
    fun observeOrder(orderId: String): Flow<Order?>
    suspend fun getDeliveryQuote(storeId: String, dropoff: DeliveryAddress): AppResult<DeliveryQuote>
    suspend fun placeOrder(
        storeId: String,
        items: List<CartLine>,
        address: DeliveryAddress,
        quote: DeliveryQuote,
        voucherCode: String?,
        paymentMethod: PaymentMethod,
        idempotencyKey: String,
    ): AppResult<Order>
    suspend fun confirmOrder(orderId: String): AppResult<Unit>
    suspend fun rejectOrder(orderId: String, reasonCode: String, reasonText: String): AppResult<Unit>
    suspend fun markReady(orderId: String): AppResult<Unit>
    suspend fun dispatchDriver(orderId: String): AppResult<Unit>
    suspend fun mockDeliveryAdvance(orderId: String, targetStatus: OrderStatus): AppResult<Unit>
}
