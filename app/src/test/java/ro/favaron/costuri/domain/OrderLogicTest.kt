package ro.favaron.costuri.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.favaron.costuri.TestData
import ro.favaron.costuri.assertBd
import ro.favaron.costuri.bd
import ro.favaron.costuri.data.RecipeEntity
import java.time.LocalDate

class OrderLogicTest {

    private fun order(pieces: Int, price: String = "25.00", recipe: RecipeView = TestData.cake()) =
        Order(id = 1, date = LocalDate.of(2026, 10, 1), products = listOf(OrderFactory.buildProduct(recipe, pieces, bd(price), key = 7)))

    private fun Order.p() = products.single()
    private fun Order.line(name: String) = p().lines.single { it.name == name }

    @Test
    fun scaling_by_pieces_sold() {
        val o = order(3)
        assertBd("120", o.line("Făină").quantity) // 400 / 10 * 3, exact
        assertBd("60", o.line("Unt").quantity)
        assertBd("1.5", o.line("Făină").lineCost) // 120 * 0.0125
        assertBd("2.3976", o.line("Unt").lineCost) // 60 * 0.03996
        // per-piece base stored unrounded
        assertBd("40", o.line("Făină").basePerPiece)
        assertBd("0.3", o.line("Hârtie de copt").basePerPiece)
    }

    @Test
    fun consumables_are_ceiled_to_whole_numbers() {
        val o = order(3)
        assertBd("3", o.line("Cutie").quantity) // 10/10*3 = 3
        assertBd("1", o.line("Hârtie de copt").quantity) // 3/10*3 = 0.9 -> 1
        assertBd("4", order(11).line("Hârtie de copt").quantity) // 3.3 -> 4
        assertBd("0.2", o.line("Hârtie de copt").lineCost)
    }

    @Test
    fun fixed_consumables_are_not_scaled() {
        listOf(1, 3, 10, 25).forEach { p ->
            val l = order(p).line("Pahar")
            assertTrue(l.fixedQuantity)
            assertBd("4", l.quantity)
            assertBd("1.6", l.lineCost)
        }
    }

    @Test
    fun order_totals_from_lines() {
        val o = order(3)
        // 1.5 + 2.3976 + 4.5 + 0.2 + 1.6
        assertBd("10.1976", o.cost)
        assertBd("75", o.revenue)
        assertBd("64.8024", o.profit)
        assertEquals("10,20", Fmt.money(o.cost))
    }

    @Test
    fun price_override_recalculates_revenue_and_profit_only() {
        val o = order(3, price = "24.99")
        assertBd("74.97", o.revenue)
        val o2 = OrderEditor.setPrice(o, 7, bd("30"))
        assertBd("90", o2.revenue)
        assertEquals(o.cost, o2.cost)
        assertBd("79.8024", o2.profit)
    }

    @Test
    fun manual_quantity_in_new_order_dialog_wins_and_fixes_consumables() {
        val p = OrderFactory.buildProduct(TestData.cake(), 3, bd("25"), mapOf(0 to bd("150"), 3 to bd("2")))
        assertBd("150", p.lines[0].quantity)
        assertBd("1.875", p.lines[0].lineCost)
        assertFalse(p.lines[0].fixedQuantity)
        assertBd("2", p.lines[3].quantity)
        assertTrue(p.lines[3].fixedQuantity)
    }

    @Test
    fun editing_quantity_or_unit_price_recalculates_line_cost_with_stored_values() {
        var o = order(3)
        val flour = o.line("Făină")
        o = OrderEditor.setQuantity(o, 7, flour.key, bd("200"))
        assertBd("2.5", o.line("Făină").lineCost) // 200 * stored 0.0125
        o = OrderEditor.setUnitPrice(o, 7, flour.key, bd("0.02"))
        assertBd("4", o.line("Făină").lineCost)
        assertFalse(o.line("Făină").fixedQuantity) // ingredient stays non-fixed
    }

    @Test
    fun manual_cost_override_wins_until_quantity_or_price_edited() {
        var o = order(3)
        val key = o.line("Făină").key
        o = OrderEditor.setLineCost(o, 7, key, bd("9.99"))
        assertTrue(o.line("Făină").costManual)
        assertBd("9.99", o.line("Făină").lineCost)
        assertBd("18.6876", o.cost) // 10.1976 - 1.5 + 9.99
        // editing pieces or price keeps the override
        o = OrderEditor.setPieces(o, 7, 5)
        assertBd("9.99", o.line("Făină").lineCost)
        // editing quantity recalculates and drops the override
        o = OrderEditor.setQuantity(o, 7, key, bd("120"))
        assertFalse(o.line("Făină").costManual)
        assertBd("1.5", o.line("Făină").lineCost)
        // same for unit price
        o = OrderEditor.setLineCost(o, 7, key, bd("3"))
        o = OrderEditor.setUnitPrice(o, 7, key, bd("0.0125"))
        assertBd("1.5", o.line("Făină").lineCost)
    }

    @Test
    fun manually_editing_a_consumable_quantity_sets_fixed_flag() {
        var o = order(3)
        val box = o.line("Cutie")
        assertFalse(box.fixedQuantity)
        o = OrderEditor.setQuantity(o, 7, box.key, bd("5"))
        assertTrue(o.line("Cutie").fixedQuantity)
        o = OrderEditor.setFixed(o, 7, box.key, false)
        assertFalse(o.line("Cutie").fixedQuantity)
    }

    @Test
    fun editing_pieces_changes_revenue_only() {
        val o = order(3)
        val o2 = OrderEditor.setPieces(o, 7, 10)
        assertEquals(10, o2.p().pieces)
        assertBd("250", o2.revenue)
        assertEquals(o.p().lines, o2.p().lines) // quantities and costs untouched
        assertEquals(o.cost, o2.cost)
    }

    @Test
    fun rescale_10_to_12_to_10_returns_original_values() {
        // non-terminating divisions on purpose: yield 3
        val recipe = TestData.cake().let { r -> r.copy(recipe = RecipeEntity(1, "Tort", 3, bd("25"))) }
        val original = order(10, recipe = recipe)
        val up = OrderEditor.rescale(original, 7, 12)
        assertEquals(12, up.p().pieces)
        assertBd("1600", up.line("Făină").quantity) // 400/3*12
        assertBd("40", up.line("Cutie").quantity) // 10/3*12
        assertBd("12", up.line("Hârtie de copt").quantity) // 3/3*12
        val back = OrderEditor.rescale(up, 7, 10)
        assertEquals(original, back) // identical values, no accumulated rounding
        assertBd("1333.3333333333", back.line("Făină").quantity)
        assertBd("34", back.line("Cutie").quantity) // 33.33 -> 34
    }

    @Test
    fun rescale_uses_base_not_previously_rounded_quantity() {
        val o = order(1) // paper: 0.3 -> 1
        assertBd("1", o.line("Hârtie de copt").quantity)
        // from the rounded quantity 1 * 3 we would get 3; from the base 0.3*3 = 0.9 -> 1
        assertBd("1", OrderEditor.rescale(o, 7, 3).line("Hârtie de copt").quantity)
    }

    @Test
    fun rescale_overwrites_manual_edits_but_keeps_fixed_lines() {
        var o = order(3)
        val flourKey = o.line("Făină").key
        val boxKey = o.line("Cutie").key
        val paperKey = o.line("Hârtie de copt").key
        o = OrderEditor.setQuantity(o, 7, flourKey, bd("999"))
        o = OrderEditor.setLineCost(o, 7, paperKey, bd("5"))
        o = OrderEditor.setQuantity(o, 7, boxKey, bd("7")) // consumable -> becomes fixed
        val r = OrderEditor.rescale(o, 7, 6)
        assertBd("240", r.line("Făină").quantity) // manual edit overwritten
        assertBd("3", r.line("Făină").lineCost)
        assertBd("2", r.line("Hârtie de copt").quantity) // 1.8 -> 2
        assertBd("0.4", r.line("Hârtie de copt").lineCost) // manual cost overwritten
        assertFalse(r.line("Hârtie de copt").costManual)
        assertBd("7", r.line("Cutie").quantity) // fixed by the manual edit: untouched
        assertBd("4", r.line("Pahar").quantity) // fixed: untouched
        // switching fixed off lets rescale recompute it
        val r2 = OrderEditor.rescale(OrderEditor.setFixed(r, 7, boxKey, false), 7, 6)
        assertBd("6", r2.line("Cutie").quantity)
    }

    @Test
    fun rescale_uses_stored_unit_prices() {
        var o = order(3)
        val key = o.line("Făină").key
        o = OrderEditor.setUnitPrice(o, 7, key, bd("0.1")) // stored price changed in the snapshot
        val r = OrderEditor.rescale(o, 7, 4)
        assertBd("16", r.line("Făină").lineCost) // 160 * 0.1
    }

    @Test
    fun adding_and_deleting_manual_lines_updates_totals() {
        var o = order(3)
        val before = o.cost
        o = OrderEditor.addLine(o, 7, -99, "Panglică", "buc", true, bd("2"), bd("0.75"))
        assertBd(before.add(bd("1.5")).toPlainString(), o.cost)
        assertBd("0.66666666666666666667", o.line("Panglică").basePerPiece)
        assertBd(bd("75").subtract(o.cost).toPlainString(), o.profit)
        // a manual line rescales like the others (consumable -> CEIL)
        assertBd("4", OrderEditor.rescale(o, 7, 6).line("Panglică").quantity)
        o = OrderEditor.deleteLine(o, 7, -99)
        assertEquals(before, o.cost)
        o = OrderEditor.deleteLine(o, 7, o.line("Făină").key)
        assertBd(before.subtract(bd("1.5")).toPlainString(), o.cost)
    }

    @Test(expected = OrderFactory.MissingIngredientException::class)
    fun recipe_with_missing_ingredient_cannot_be_ordered() {
        val r = TestData.cake()
        val broken = r.copy(lines = r.lines.mapIndexed { i, l -> if (i == 1) l.copy(ingredient = null) else l })
        assertFalse(broken.canBeOrdered)
        OrderFactory.buildProduct(broken, 1, bd("25"))
    }

    @Test
    fun summary_totals_per_product() {
        val a = order(3)
        val b = order(2, price = "20").copy(id = 2, date = LocalDate.of(2026, 9, 1))
        val s = Summaries.summarize(listOf(a, b))
        assertBd(a.revenue.add(b.revenue).toPlainString(), s.revenue)
        assertBd(a.cost.add(b.cost).toPlainString(), s.cost)
        assertEquals(5L, s.products.single().pieces)
        assertEquals(listOf(a), Summaries.filter(listOf(a, b), LocalDate.of(2026, 9, 15), null))
        assertEquals(listOf(b), Summaries.filter(listOf(a, b), null, LocalDate.of(2026, 9, 1)))
    }
}
