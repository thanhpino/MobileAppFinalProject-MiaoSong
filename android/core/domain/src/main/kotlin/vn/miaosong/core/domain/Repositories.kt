package vn.miaosong.core.domain

import kotlinx.coroutines.flow.Flow
import vn.miaosong.core.common.AppResult
import vn.miaosong.core.model.CartLine
import vn.miaosong.core.model.DeliveryAddress
import vn.miaosong.core.model.DeliveryQuote
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
    suspend fun signUpWithEmail(email: String, password: String, name: String): AppResult<UserProfile>
    suspend fun signInWithGoogle(idToken: String): AppResult<UserProfile>
    suspend fun signInWithFacebook(token: String): AppResult<UserProfile>
    suspend fun sendPasswordResetEmail(email: String): AppResult<Unit>
    suspend fun changePassword(newPassword: String): AppResult<Unit>
    suspend fun updateProfile(name: String, phone: String): AppResult<UserProfile>
    suspend fun signOut()
}

interface StoreRepository {
    fun observeStores(): Flow<List<Store>>
    fun observeStore(storeId: String): Flow<Store?>
    fun observeCategories(storeId: String): Flow<List<MenuCategory>>
    fun observeMenuItems(storeId: String): Flow<List<MenuItem>>
    fun observeVouchers(storeId: String): Flow<List<Voucher>>
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
}
