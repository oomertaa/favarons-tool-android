package ro.favaron.costuri.domain

/**
 * The exact strings the history screen shows for stored values. The history
 * UI and the CSV export both use these, so they can never diverge.
 */
object HistoryDisplay {
    data class Line(
        val name: String,
        val unit: String,
        val consumable: String,
        val quantity: String,
        val unitPrice: String,
        val lineCost: String,
    )

    fun line(l: OrderLine) = Line(
        name = l.name,
        unit = l.unit,
        consumable = Fmt.yesNo(l.consumable),
        quantity = Fmt.qty(l.quantity),
        unitPrice = Fmt.adaptive(l.unitPrice),
        lineCost = Fmt.adaptive(l.lineCost),
    )

    fun date(o: Order): String = Fmt.date(o.date)
    fun pieces(p: OrderProduct): String = Fmt.int(p.pieces)
    fun price(p: OrderProduct): String = Fmt.money(p.pricePerPiece)
}
