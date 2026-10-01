package ro.favaron.costuri.domain

import java.math.BigDecimal
import java.time.LocalDate

data class ProductSummary(
    val name: String,
    val pieces: Long,
    val revenue: BigDecimal,
    val cost: BigDecimal,
) {
    val profit: BigDecimal get() = revenue.subtract(cost)
}

data class Summary(
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val products: List<ProductSummary>,
) {
    val profit: BigDecimal get() = revenue.subtract(cost)
}

/** Summaries are computed only from stored history values. */
object Summaries {
    fun filter(orders: List<Order>, from: LocalDate?, to: LocalDate?): List<Order> =
        orders.filter { (from == null || !it.date.isBefore(from)) && (to == null || !it.date.isAfter(to)) }

    fun summarize(orders: List<Order>): Summary {
        val all = orders.flatMap { it.products }
        val perProduct = all.groupBy { it.name.trim() }.map { (name, ps) ->
            ProductSummary(
                name = name,
                pieces = ps.sumOf { it.pieces.toLong() },
                revenue = Calc.sum(ps.map { it.revenue }),
                cost = Calc.sum(ps.map { it.cost }),
            )
        }.sortedBy { it.name.lowercase() }
        return Summary(
            revenue = Calc.sum(orders.map { it.revenue }),
            cost = Calc.sum(orders.map { it.cost }),
            products = perProduct,
        )
    }
}
