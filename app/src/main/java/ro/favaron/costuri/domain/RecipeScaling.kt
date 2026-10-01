package ro.favaron.costuri.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * "Scalează": changes how many pieces a recipe makes AND multiplies the line
 * quantities by newYield / oldYield (so the costs follow).
 *
 * Only this explicit action scales. Typing a new yield in the recipe editor is
 * a plain correction and leaves the quantities as they are.
 *
 * - ingredients: exactly proportional (scale 10, trailing zeros trimmed);
 * - consumables: proportional, rounded UP to whole pieces;
 * - consumables with "Cantitate fixă": unchanged.
 */
object RecipeScaling {
    fun factor(oldYield: Int, newYield: Int): BigDecimal {
        require(oldYield >= 1 && newYield >= 1)
        return BigDecimal(newYield).divide(BigDecimal(oldYield), Calc.QTY_SCALE, RoundingMode.HALF_UP)
    }

    fun scaleQuantity(
        quantity: BigDecimal,
        oldYield: Int,
        newYield: Int,
        consumable: Boolean,
        fixed: Boolean,
    ): BigDecimal {
        require(oldYield >= 1 && newYield >= 1)
        if (fixed) return quantity
        // multiply first, then divide once: exact whenever the result terminates
        val q = quantity.multiply(BigDecimal(newYield)).divide(BigDecimal(oldYield), Calc.QTY_SCALE, RoundingMode.HALF_UP)
        return if (consumable) q.setScale(0, RoundingMode.CEILING) else Calc.plain(q.stripTrailingZeros())
    }
}
