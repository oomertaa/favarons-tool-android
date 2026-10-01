package ro.favaron.costuri.domain

import java.math.BigDecimal
import java.time.LocalDate

/*
 * History domain model: a saved order is a self-contained snapshot.
 * All derived values (product cost, revenue, order totals, profit) are
 * computed ONLY from the values stored in the snapshot itself.
 */

data class OrderLine(
    /** Local identity inside the order (DB row id, or negative for unsaved lines). */
    val key: Long,
    val name: String,
    val unit: String,
    val consumable: Boolean,
    val fixedQuantity: Boolean,
    val quantity: BigDecimal,
    val basePerPiece: BigDecimal,
    val unitPrice: BigDecimal,
    val lineCost: BigDecimal,
    val costManual: Boolean = false,
)

data class OrderProduct(
    val key: Long,
    val name: String,
    val pieces: Int,
    val pricePerPiece: BigDecimal,
    val recipeYield: Int,
    val lines: List<OrderLine>,
) {
    val cost: BigDecimal get() = Calc.sum(lines.map { it.lineCost })
    val revenue: BigDecimal get() = Calc.revenue(pricePerPiece, pieces)
    val profit: BigDecimal get() = revenue.subtract(cost)
}

data class Order(
    val id: Long = 0,
    val date: LocalDate,
    val customer: String = "",
    val note: String = "",
    val manuallyEdited: Boolean = false,
    val createdAt: Long = 0,
    val products: List<OrderProduct>,
) {
    val cost: BigDecimal get() = Calc.sum(products.map { it.cost })
    val revenue: BigDecimal get() = Calc.sum(products.map { it.revenue })
    val profit: BigDecimal get() = revenue.subtract(cost)
}
