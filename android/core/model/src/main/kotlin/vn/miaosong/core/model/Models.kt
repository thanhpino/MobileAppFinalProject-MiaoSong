package vn.miaosong.core.model

import kotlinx.serialization.Serializable

enum class UserRole { CUSTOMER, MERCHANT }

enum class ManualStoreStatus { OPEN, BUSY, CLOSED }

enum class EffectiveStoreStatus { OPEN, BUSY, CLOSED }

enum class OptionGroupType { SINGLE, MULTI }

enum class VoucherType { PERCENT, FIXED }

enum class PaymentMethod { COD, ZALOPAY }

enum class PaymentStatus { UNPAID, PAID, FAILED }

enum class RefundStatus { NONE, PENDING, REFUNDED, REFUND_FAILED }

enum class DeliveryProviderType { LALAMOVE, MOCK }

enum class OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    CONFIRMED,
    READY,
    FINDING_DRIVER,
    DRIVER_ASSIGNED,
    ARRIVED_AT_STORE,
    PICKED_UP,
    DELIVERING,
    COMPLETED,
    DELIVERY_FAILED,
    REJECTED,
    CANCELLED,
}

@Serializable
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val storeId: String? = null,
    val avatarUrl: String = "",
)

@Serializable
data class DeliveryAddress(
    val id: String = "",
    val label: String = "",
    val fullText: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val recipientName: String = "",
    val recipientPhone: String = "",
    val note: String = "",
    val isDefault: Boolean = false,
)

@Serializable
data class TimeSlot(
    val open: String,  // "HH:mm"
    val close: String, // "HH:mm"
)

@Serializable
data class SpecialDay(
    val date: String, // "YYYY-MM-DD"
    val isClosed: Boolean,
    val slots: List<TimeSlot> = emptyList(),
)

@Serializable
data class StoreSchedule(
    val manualStatus: ManualStoreStatus = ManualStoreStatus.OPEN,
    val busyUntilEpochMs: Long? = null,
    val weeklySchedule: Map<String, List<TimeSlot>> = emptyMap(),
    val specialDays: List<SpecialDay> = emptyList(),
)

@Serializable
data class Store(
    val id: String,
    val name: String,
    val slug: String,
    val address: String,
    val avatarUrl: String = "",
    val lat: Double,
    val lng: Double,
    val phone: String,
    val maxDistanceKm: Double = 10.0,
    val deliveryProvider: DeliveryProviderType = DeliveryProviderType.LALAMOVE,
    val schedule: StoreSchedule = StoreSchedule(),
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
)

@Serializable
data class MenuCategory(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
)

@Serializable
data class MenuOption(
    val id: String,
    val name: String,
    val priceDelta: Long = 0L,
    val isAvailable: Boolean = true,
)

@Serializable
data class OptionGroup(
    val id: String,
    val name: String,
    val type: OptionGroupType,
    val required: Boolean = false,
    val min: Int = 0,
    val max: Int = 1,
    val options: List<MenuOption> = emptyList(),
)

@Serializable
data class MenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String = "",
    val imageUrl: String = "",
    val basePrice: Long,
    val isAvailable: Boolean = true,
    val optionGroups: List<OptionGroup> = emptyList(),
)

@Serializable
data class Voucher(
    val id: String,
    val code: String,
    val type: VoucherType,
    val value: Long,
    val maxDiscount: Long? = null,
    val minOrder: Long = 0L,
    val startAtEpochMs: Long = 0L,
    val endAtEpochMs: Long = Long.MAX_VALUE,
    val usageLimit: Int = 0,
    val usedCount: Int = 0,
    val perUserLimit: Int = 1,
    val isActive: Boolean = true,
)

@Serializable
data class CartLine(
    val lineKey: String,
    val storeId: String,
    val menuItemId: String,
    val itemName: String,
    val imageUrl: String = "",
    val basePrice: Long,
    val sizeOptionId: String? = null,
    val sizeName: String = "",
    val sizeDelta: Long = 0L,
    val toppingOptionIds: List<String> = emptyList(),
    val toppingNames: List<String> = emptyList(),
    val toppingsDelta: List<Long> = emptyList(),
    val note: String = "",
    val quantity: Int = 1,
) {
    val unitPrice: Long
        get() = basePrice + sizeDelta + toppingsDelta.sum()

    val lineTotal: Long
        get() = unitPrice * quantity
}

@Serializable
data class PriceBreakdown(
    val itemsTotal: Long,
    val toppingsTotal: Long,
    val subtotal: Long,
    val discount: Long,
    val shippingFee: Long,
    val total: Long,
)

@Serializable
data class DeliveryQuote(
    val quotationId: String,
    val provider: DeliveryProviderType,
    val distanceKm: Double,
    val shippingFee: Long,
    val expiresAtIso: String,
)

@Serializable
data class DriverInfo(
    val name: String,
    val phone: String,
    val plate: String,
)

@Serializable
data class OrderPayment(
    val method: PaymentMethod,
    val status: PaymentStatus = PaymentStatus.UNPAID,
    val amountVnd: Long = 0L,
    val appTransId: String? = null,
    val paymentUrl: String? = null,
)

@Serializable
data class OrderDelivery(
    val provider: DeliveryProviderType = DeliveryProviderType.LALAMOVE,
    val quotationId: String = "",
    val externalId: String? = null,
    val driver: DriverInfo? = null,
    val shareLink: String? = null,
)

@Serializable
data class OrderItemSnapshot(
    val lineKey: String,
    val menuItemId: String,
    val itemName: String,
    val sizeOptionId: String? = null,
    val sizeName: String = "",
    val toppingOptionIds: List<String> = emptyList(),
    val toppingNames: List<String> = emptyList(),
    val note: String = "",
    val unitPrice: Long,
    val quantity: Int,
)

@Serializable
data class Order(
    val id: String,
    val code: String,
    val customerId: String,
    val storeId: String,
    val storeName: String = "",
    val items: List<OrderItemSnapshot> = emptyList(),
    val pricing: PriceBreakdown,
    val voucherCode: String? = null,
    val address: DeliveryAddress,
    val payment: OrderPayment,
    val refundStatus: RefundStatus = RefundStatus.NONE,
    val status: OrderStatus = OrderStatus.PLACED,
    val delivery: OrderDelivery = OrderDelivery(),
    val rejectReason: String? = null,
    val createdAtEpochMs: Long = 0L,
)

@Serializable
data class Review(
    val orderId: String,
    val storeId: String,
    val customerId: String,
    val stars: Int,
    val tags: List<String> = emptyList(),
    val comment: String = "",
    val imageUrl: String = "",
    val createdAtEpochMs: Long = 0L,
)

@Serializable
enum class NotificationType {
    ORDER_STATUS,
    PROMOTION,
    SYSTEM,
}

@Serializable
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timestampEpochMs: Long = 0L,
    val type: NotificationType = NotificationType.ORDER_STATUS,
    val orderId: String? = null,
    val deepLink: String? = null,
    val isRead: Boolean = false,
)

