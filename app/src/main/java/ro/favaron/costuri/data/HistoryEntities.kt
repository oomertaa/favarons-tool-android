package ro.favaron.costuri.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

// ---------------------------------------------------------------------------
// HISTORY data domain: fully denormalized snapshots of sales.
// Every value is a plain copy. There are NO foreign keys and NO references to
// ingredients or recipes; live data can change or disappear freely.
// orderId / productLineId only group rows inside the history itself.
// ---------------------------------------------------------------------------

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO date, yyyy-MM-dd. */
    val date: String,
    val customer: String,
    val note: String,
    /** "modificat manual": edited after it was first saved. */
    val manuallyEdited: Boolean,
    val totalCost: BigDecimal,
    val totalRevenue: BigDecimal,
    val profit: BigDecimal,
    val createdAt: Long,
)

@Entity(tableName = "order_products", indices = [Index("orderId")])
data class OrderProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val position: Int,
    val productName: String,
    val piecesSold: Int,
    val sellingPricePerPiece: BigDecimal,
    val recipeYield: Int,
    val productCost: BigDecimal,
    val productRevenue: BigDecimal,
)

@Entity(tableName = "order_ingredient_lines", indices = [Index("orderId"), Index("productLineId")])
data class OrderIngredientLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productLineId: Long,
    val position: Int,
    val name: String,
    val unit: String,
    val isConsumable: Boolean,
    val fixedQuantity: Boolean,
    val quantity: BigDecimal,
    /** Exact (unrounded) quantity per piece; source for "Rescalează automat". */
    val basePerPiece: BigDecimal,
    /** Unit price at the moment of the sale, full precision. */
    val unitPrice: BigDecimal,
    /** Full-precision line cost. */
    val lineCost: BigDecimal,
    /** True when the line cost was typed in manually (overrides quantity * unit price). */
    val costManual: Boolean,
)
