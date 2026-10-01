package ro.favaron.costuri.domain

/**
 * CSV for Romanian Excel: UTF-8 with BOM, ';' separator, CRLF line endings.
 * Every number goes through [Fmt] (the same formatter as the screens), so
 * the file contains exactly the displayed values, never raw stored precision.
 * Numeric cells hold only the number (decimal comma, no thousands separator,
 * no currency or unit text).
 */
object Csv {
    const val BOM = "\uFEFF"
    const val SEP = ";"
    const val EOL = "\r\n"

    val SALES_HEADER = listOf(
        "Data", "Client", "Produs", "Bucăți", "Ingredient", "Unitate", "Consumabil",
        "Cantitate", "Preț unitar", "Cost linie", "Preț vânzare/buc",
    )

    fun escape(s: String): String =
        if (s.contains(';') || s.contains('"') || s.contains('\n') || s.contains('\r'))
            "\"" + s.replace("\"", "\"\"") + "\""
        else s

    fun build(header: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder(BOM)
        (listOf(header) + rows).forEach { r -> sb.append(r.joinToString(SEP) { escape(it) }).append(EOL) }
        return sb.toString()
    }

    /** One row per ingredient line per order, read only from history. */
    fun salesRows(orders: List<Order>): List<List<String>> = orders.flatMap { o ->
        o.products.flatMap { p ->
            val head = listOf(HistoryDisplay.date(o), o.customer, p.name, HistoryDisplay.pieces(p))
            val price = HistoryDisplay.price(p)
            if (p.lines.isEmpty()) listOf(head + listOf("", "", "", "", "", "", price))
            else p.lines.map { l ->
                val d = HistoryDisplay.line(l)
                head + listOf(d.name, d.unit, d.consumable, d.quantity, d.unitPrice, d.lineCost, price)
            }
        }
    }

    fun sales(orders: List<Order>): String = build(SALES_HEADER, salesRows(orders))

    val INGREDIENTS_HEADER = listOf(
        "Nume", "Unitate", "Consumabil", "Cantitate pachet", "Preț pachet", "Preț unitar",
    )

    fun ingredients(list: List<ro.favaron.costuri.data.IngredientEntity>): String =
        build(INGREDIENTS_HEADER, list.map {
            listOf(
                it.name, it.unit, Fmt.yesNo(it.isConsumable), Fmt.qty(it.packageQuantity),
                Fmt.money(it.packagePrice), Fmt.adaptive(it.unitPrice()),
            )
        })

    val RECIPES_HEADER = listOf(
        "Rețetă", "Bucăți rețetă", "Preț vânzare/buc", "Cost total rețetă", "Cost/buc",
        "Ingredient", "Unitate", "Cantitate", "Cantitate fixă", "Preț unitar", "Cost linie", "Lipsă",
    )

    fun recipes(list: List<RecipeView>): String =
        build(RECIPES_HEADER, list.flatMap { r ->
            val head = listOf(
                r.recipe.name, Fmt.int(r.recipe.yieldPieces), r.recipe.sellingPrice?.let { Fmt.money(it) } ?: "",
                Fmt.money(r.batchCost), Fmt.money(r.costPerPiece),
            )
            if (r.lines.isEmpty()) listOf(head + List(7) { "" })
            else r.lines.map { l ->
                head + listOf(
                    l.name, l.unit, Fmt.qty(l.line.quantity), Fmt.yesNo(l.fixedQuantity),
                    l.unitPrice?.let { Fmt.adaptive(it) } ?: "", l.cost?.let { Fmt.adaptive(it) } ?: "",
                    Fmt.yesNo(l.missing),
                )
            }
        })
}
