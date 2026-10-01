package ro.favaron.costuri.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.favaron.costuri.domain.Order
import ro.favaron.costuri.domain.OrderLine
import ro.favaron.costuri.domain.OrderProduct
import java.time.LocalDate

/**
 * Sales history (HISTORY domain). Reads and writes ONLY the denormalized
 * history tables; each table is queried on its own and assembled in memory.
 */
class HistoryRepository(private val db: AppDatabase) {
    private val dao = db.historyDao()

    /** All orders, newest first; re-emitted whenever a history table changes. */
    val orders: Flow<List<Order>> =
        db.invalidationTracker.createFlow("orders", "order_products", "order_ingredient_lines")
            .map { loadAll() }

    suspend fun loadAll(): List<Order> = db.withTransaction {
        assemble(dao.getOrders(), dao.getProducts(), dao.getLines())
    }

    /**
     * Saves a snapshot. New orders (id == 0) are inserted; existing ones are
     * overwritten from the edited snapshot and flagged "modificat manual".
     * Stored totals are re-derived from the snapshot's own lines.
     */
    suspend fun save(order: Order, now: Long = System.currentTimeMillis()): Long = db.withTransaction {
        val isNew = order.id == 0L
        val entity = OrderEntity(
            id = order.id,
            date = order.date.toString(),
            customer = order.customer.trim(),
            note = order.note.trim(),
            manuallyEdited = if (isNew) order.manuallyEdited else true,
            totalCost = order.cost,
            totalRevenue = order.revenue,
            profit = order.profit,
            createdAt = if (isNew || order.createdAt == 0L) now else order.createdAt,
        )
        val id = if (isNew) dao.insertOrder(entity) else {
            dao.updateOrder(entity)
            dao.deleteLinesOf(order.id)
            dao.deleteProductsOf(order.id)
            order.id
        }
        order.products.forEachIndexed { pi, p ->
            val pid = dao.insertProduct(
                OrderProductEntity(
                    orderId = id,
                    position = pi,
                    productName = p.name.trim(),
                    piecesSold = p.pieces,
                    sellingPricePerPiece = p.pricePerPiece,
                    recipeYield = p.recipeYield,
                    productCost = p.cost,
                    productRevenue = p.revenue,
                ),
            )
            dao.insertLines(
                p.lines.mapIndexed { li, l ->
                    OrderIngredientLineEntity(
                        orderId = id,
                        productLineId = pid,
                        position = li,
                        name = l.name.trim(),
                        unit = l.unit.trim(),
                        isConsumable = l.consumable,
                        fixedQuantity = l.fixedQuantity,
                        quantity = l.quantity,
                        basePerPiece = l.basePerPiece,
                        unitPrice = l.unitPrice,
                        lineCost = l.lineCost,
                        costManual = l.costManual,
                    )
                },
            )
        }
        id
    }

    suspend fun delete(orderId: Long) = db.withTransaction {
        dao.deleteLinesOf(orderId)
        dao.deleteProductsOf(orderId)
        dao.deleteOrder(orderId)
    }

    companion object {
        fun assemble(
            orders: List<OrderEntity>,
            products: List<OrderProductEntity>,
            lines: List<OrderIngredientLineEntity>,
        ): List<Order> {
            val linesByProduct = lines.groupBy { it.productLineId }
            val productsByOrder = products.groupBy { it.orderId }
            return orders.map { o ->
                Order(
                    id = o.id,
                    date = LocalDate.parse(o.date),
                    customer = o.customer,
                    note = o.note,
                    manuallyEdited = o.manuallyEdited,
                    createdAt = o.createdAt,
                    products = (productsByOrder[o.id] ?: emptyList()).sortedBy { it.position }.map { p ->
                        OrderProduct(
                            key = p.id,
                            name = p.productName,
                            pieces = p.piecesSold,
                            pricePerPiece = p.sellingPricePerPiece,
                            recipeYield = p.recipeYield,
                            lines = (linesByProduct[p.id] ?: emptyList()).sortedBy { it.position }.map { l ->
                                OrderLine(
                                    key = l.id,
                                    name = l.name,
                                    unit = l.unit,
                                    consumable = l.isConsumable,
                                    fixedQuantity = l.fixedQuantity,
                                    quantity = l.quantity,
                                    basePerPiece = l.basePerPiece,
                                    unitPrice = l.unitPrice,
                                    lineCost = l.lineCost,
                                    costManual = l.costManual,
                                )
                            },
                        )
                    },
                )
            }.sortedWith(compareByDescending<Order> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })
        }
    }
}
