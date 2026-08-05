package com.pos.offline.data.local.entity
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["productId"], unique = true)],
)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val name: String,
    val unitPrice: Long,
    val quantity: Double = 1.0,
) {
    val lineTotal: Long get() = kotlin.math.round(unitPrice * quantity).toLong()
}
