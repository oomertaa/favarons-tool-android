package ro.favaron.costuri.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.favaron.costuri.assertBd
import ro.favaron.costuri.bd

class RecipeScalingTest {

    @Test
    fun ingredients_scale_exactly_proportionally() {
        assertBd("800", RecipeScaling.scaleQuantity(bd("400"), 10, 20, consumable = false, fixed = false))
        assertBd("600", RecipeScaling.scaleQuantity(bd("400"), 10, 15, consumable = false, fixed = false))
        assertBd("12.5", RecipeScaling.scaleQuantity(bd("25"), 10, 5, consumable = false, fixed = false))
        assertBd("233.3333333333", RecipeScaling.scaleQuantity(bd("100"), 3, 7, consumable = false, fixed = false))
        // no exponent form (2.4E+2) in memory
        assertEquals("800", RecipeScaling.scaleQuantity(bd("400"), 10, 20, false, false).toPlainString())
    }

    @Test
    fun consumables_round_up_and_fixed_ones_stay() {
        assertBd("5", RecipeScaling.scaleQuantity(bd("3"), 10, 15, consumable = true, fixed = false)) // 4.5 -> 5
        assertBd("20", RecipeScaling.scaleQuantity(bd("10"), 10, 20, consumable = true, fixed = false))
        assertBd("4", RecipeScaling.scaleQuantity(bd("4"), 10, 50, consumable = true, fixed = true))
    }

    @Test
    fun costs_follow_the_scaled_quantities() {
        val up = Calc.unitPrice(bd("12.50"), bd("1000"))
        val scaled = RecipeScaling.scaleQuantity(bd("400"), 10, 20, false, false)
        assertBd("10", Calc.lineCost(scaled, up)) // was 5
    }

    @Test
    fun factor() {
        assertEquals("1,5", Fmt.qty(RecipeScaling.factor(10, 15)))
        assertEquals("0,5", Fmt.qty(RecipeScaling.factor(10, 5)))
    }
}
