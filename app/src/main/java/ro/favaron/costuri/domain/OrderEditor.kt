package ro.favaron.costuri.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Manual editing of a saved order. Every operation works ONLY on the values
 * stored in the order itself (stored unit prices, stored base quantities);
 * live ingredients and recipes are never consulted.
 */
object OrderEditor {

    private fun Order.mapProduct(productKey: Long, f: (OrderProduct) -> OrderProduct) =
        copy(products = products.map { if (it.key == productKey) f(it) else it })

    private fun Order.mapLine(productKey: Long, lineKey: Long, f: (OrderLine) -> OrderLine) =
        mapProduct(productKey) { p -> p.copy(lines = p.lines.map { if (it.key == lineKey) f(it) else it }) }

    // ---- order-level text ----
    fun setCustomer(o: Order, v: String) = o.copy(customer = v)
    fun setNote(o: Order, v: String) = o.copy(note = v)
    fun setDate(o: Order, v: LocalDate) = o.copy(date = v)

    // ---- product line ----
    fun setProductName(o: Order, productKey: Long, v: String) = o.mapProduct(productKey) { it.copy(name = v) }

    /** Changes revenue only: quantities are NOT rescaled (use [rescale]). */
    fun setPieces(o: Order, productKey: Long, pieces: Int): Order {
        require(pieces >= 1)
        return o.mapProduct(productKey) { it.copy(pieces = pieces) }
    }

    fun setPrice(o: Order, productKey: Long, price: BigDecimal): Order {
        require(price.signum() >= 0)
        return o.mapProduct(productKey) { it.copy(pricePerPiece = price) }
    }

    /**
     * "Rescalează automat": recomputes every non-fixed line from the stored
     * per-piece base (not from the current, possibly rounded, quantity),
     * overwriting manual quantity/cost edits; fixed lines stay untouched.
     * Costs use the STORED unit prices.
     */
    fun rescale(o: Order, productKey: Long, pieces: Int): Order {
        require(pieces >= 1)
        return o.mapProduct(productKey) { p ->
            p.copy(
                pieces = pieces,
                lines = p.lines.map { l ->
                    if (l.fixedQuantity) l
                    else {
                        val q = Calc.scaledQuantity(l.basePerPiece, pieces, l.consumable)
                        l.copy(quantity = q, lineCost = Calc.lineCost(q, l.unitPrice), costManual = false)
                    }
                },
            )
        }
    }

    // ---- ingredient line ----
    fun setLineName(o: Order, productKey: Long, lineKey: Long, v: String) =
        o.mapLine(productKey, lineKey) { it.copy(name = v) }

    fun setLineUnit(o: Order, productKey: Long, lineKey: Long, v: String) =
        o.mapLine(productKey, lineKey) { it.copy(unit = v) }

    /** Recalculates the line cost; a manually edited consumable becomes "Cantitate fixă". */
    fun setQuantity(o: Order, productKey: Long, lineKey: Long, q: BigDecimal): Order {
        require(q.signum() >= 0)
        return o.mapLine(productKey, lineKey) {
            it.copy(
                quantity = q,
                lineCost = Calc.lineCost(q, it.unitPrice),
                costManual = false,
                fixedQuantity = it.fixedQuantity || it.consumable,
            )
        }
    }

    fun setUnitPrice(o: Order, productKey: Long, lineKey: Long, price: BigDecimal): Order {
        require(price.signum() >= 0)
        return o.mapLine(productKey, lineKey) {
            it.copy(unitPrice = price, lineCost = Calc.lineCost(it.quantity, price), costManual = false)
        }
    }

    /** Manual override: wins until quantity or unit price is edited again. */
    fun setLineCost(o: Order, productKey: Long, lineKey: Long, cost: BigDecimal): Order {
        require(cost.signum() >= 0)
        return o.mapLine(productKey, lineKey) { it.copy(lineCost = cost, costManual = true) }
    }

    fun setFixed(o: Order, productKey: Long, lineKey: Long, fixed: Boolean) =
        o.mapLine(productKey, lineKey) { it.copy(fixedQuantity = fixed) }

    /** Adds a free-text line (no link to live ingredients). */
    fun addLine(
        o: Order,
        productKey: Long,
        lineKey: Long,
        name: String,
        unit: String,
        consumable: Boolean,
        quantity: BigDecimal,
        unitPrice: BigDecimal,
    ): Order {
        require(quantity.signum() >= 0 && unitPrice.signum() >= 0)
        return o.mapProduct(productKey) { p ->
            p.copy(
                lines = p.lines + OrderLine(
                    key = lineKey,
                    name = name,
                    unit = unit,
                    consumable = consumable,
                    fixedQuantity = false,
                    quantity = quantity,
                    basePerPiece = quantity.divide(BigDecimal(p.pieces), Calc.BASE_SCALE, RoundingMode.HALF_UP),
                    unitPrice = unitPrice,
                    lineCost = Calc.lineCost(quantity, unitPrice),
                ),
            )
        }
    }

    fun deleteLine(o: Order, productKey: Long, lineKey: Long) =
        o.mapProduct(productKey) { p -> p.copy(lines = p.lines.filterNot { it.key == lineKey }) }
}
