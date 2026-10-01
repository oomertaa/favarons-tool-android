package ro.favaron.costuri.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.favaron.costuri.TestData
import ro.favaron.costuri.assertBd
import ro.favaron.costuri.bd
import ro.favaron.costuri.data.Converters
import java.math.BigDecimal

class CalcTest {

    @Test
    fun unitPrice_12_50_per_1000g_is_0_0125_at_scale_10() {
        val up = Calc.unitPrice(bd("12.50"), bd("1000"))
        assertEquals(bd("0.0125000000"), up)
        assertEquals(10, up.scale())
    }

    @Test
    fun line_of_40g_costs_exactly_0_5() {
        val cost = Calc.lineCost(bd("40"), Calc.unitPrice(bd("12.50"), bd("1000")))
        assertBd("0.5", cost)
    }

    @Test
    fun one_ron_per_1000g_line_of_2g_costs_0_002_and_is_stored_unrounded() {
        val up = Calc.unitPrice(bd("1"), bd("1000"))
        assertBd("0.001", up)
        val cost = Calc.lineCost(bd("2"), up)
        assertBd("0.002", cost)
        // Room stores it through the converter: the exact value survives, never "0.00".
        val c = Converters()
        val stored = c.fromBigDecimal(cost)!!
        assertBd("0.002", c.toBigDecimal(stored)!!)
        assertNotEquals(0, c.toBigDecimal(stored)!!.signum())
        assertEquals("0,002", Fmt.adaptive(cost))
    }

    @Test
    fun unitPrice_uses_half_up_at_scale_10() {
        // 10 / 3 = 3.33333333333... -> 3.3333333333 ; 20 / 3 = 6.666... -> 6.6666666667
        assertEquals(bd("3.3333333333"), Calc.unitPrice(bd("10"), bd("3")))
        assertEquals(bd("6.6666666667"), Calc.unitPrice(bd("20"), bd("3")))
    }

    @Test
    fun batch_cost_and_cost_per_piece() {
        val r = TestData.cake()
        // flour 400*0.0125=5 ; butter 200*0.03996=7.992 ; box 10*1.5=15 ; paper 3*0.2=0.6 ; cups 4*0.4=1.6
        assertBd("30.192", r.batchCost)
        assertBd("3.0192", r.costPerPiece)
        assertEquals("30,19", Fmt.money(r.batchCost))
        assertEquals("3,02", Fmt.money(r.costPerPiece))
        assertBd("21.9808", r.marginPerPiece!!)
        assertEquals("21,98", Fmt.money(r.marginPerPiece!!))
        assertEquals("87,9", Fmt.percent(r.marginPercent!!))
    }

    @Test
    fun cost_per_piece_is_not_rounded_before_display() {
        // 10 RON for a batch of 3: 3.3333..., displayed 3,33 but kept precise.
        val cpp = Calc.costPerPiece(bd("10"), 3)
        assertTrue(cpp.scale() > 2)
        assertEquals("3,33", Fmt.money(cpp))
        assertBd("10", cpp.multiply(bd("3")).setScale(10, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun many_small_lines_sum_without_rounding_drift() {
        // 1000 lines of 1 g at 1.40 RON / 1000 g = 0.0014 each. Rounding each to
        // 2 decimals would give 0.00; full precision gives exactly 1.40.
        val up = Calc.unitPrice(bd("1.40"), bd("1000"))
        val lines = List(1000) { Calc.lineCost(BigDecimal.ONE, up) }
        assertBd("1.4", Calc.sum(lines))
        assertEquals("1,40", Fmt.money(Calc.sum(lines)))
        assertEquals("0,0014", Fmt.adaptive(lines.first()))

        // 3 lines of 0.005 -> 0.015 -> displayed 0,02 (each line alone would display 0,005)
        val three = List(3) { Calc.lineCost(bd("5"), Calc.unitPrice(bd("1"), bd("1000"))) }
        assertBd("0.015", Calc.sum(three))
    }

    @Test
    fun scaling_ingredients_exactly_and_consumables_with_ceil() {
        val base = Calc.basePerPiece(bd("400"), 10)
        assertBd("120", Calc.scaledQuantity(base, 3, consumable = false))
        // 3 papers per 10 pieces, 3 pieces sold -> 0.9 -> CEIL 1
        assertBd("1", Calc.scaledQuantity(Calc.basePerPiece(bd("3"), 10), 3, consumable = true))
        // 3 papers per 10 pieces, 11 sold -> 3.3 -> 4
        assertBd("4", Calc.scaledQuantity(Calc.basePerPiece(bd("3"), 10), 11, consumable = true))
        // exact whole numbers are not bumped: 1/3 * 3 = 1 (not 2)
        assertBd("1", Calc.scaledQuantity(Calc.basePerPiece(bd("1"), 3), 3, consumable = true))
        // non-terminating ingredient division: 100 g / 3 * 3 = 100 exactly
        assertBd("100", Calc.scaledQuantity(Calc.basePerPiece(bd("100"), 3), 3, consumable = false))
        // proportional with decimals: 12.5 g / 10 * 7 = 8.75
        assertBd("8.75", Calc.scaledQuantity(Calc.basePerPiece(bd("12.5"), 10), 7, consumable = false))
    }
}
