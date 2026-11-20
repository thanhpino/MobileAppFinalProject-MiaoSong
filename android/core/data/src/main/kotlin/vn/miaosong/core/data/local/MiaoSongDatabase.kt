package vn.miaosong.core.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Lưu giỏ hàng offline (NFR-REL-02).
 * Khóa chính là `lineKey` (SHA-1) nên 2 tô bún khác ghi chú ("không hành" vs "nhiều hành")
 * luôn được lưu thành 2 dòng độc lập trong Room.
 */
@Entity(tableName = "cart_lines")
data class CartLineEntity(
    @PrimaryKey val lineKey: String,
    val storeId: String,
    val menuItemId: String,
    val itemName: String,
    val imageUrl: String,
    val basePrice: Long,
    val sizeOptionId: String?,
    val sizeName: String,
    val sizeDelta: Long,
    val toppingOptionIdsJson: String,
    val toppingNamesJson: String,
    val toppingsDeltaJson: String,
    val note: String,
    val quantity: Int,
)

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_lines")
    fun observeAll(): Flow<List<CartLineEntity>>

    @Query("SELECT * FROM cart_lines WHERE lineKey = :lineKey LIMIT 1")
    suspend fun getByKey(lineKey: String): CartLineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CartLineEntity)

    @Query("UPDATE cart_lines SET quantity = :quantity WHERE lineKey = :lineKey")
    suspend fun updateQuantity(lineKey: String, quantity: Int)

    @Query("DELETE FROM cart_lines WHERE lineKey = :lineKey")
    suspend fun deleteByKey(lineKey: String)

    @Query("DELETE FROM cart_lines")
    suspend fun clearAll()
}

@Database(entities = [CartLineEntity::class], version = 1, exportSchema = false)
abstract class MiaoSongDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}
