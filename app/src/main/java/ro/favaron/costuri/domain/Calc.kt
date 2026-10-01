package ro.favaron.costuri.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * All cost arithmetic. Everything is BigDecimal and nothing is rounded to
 * 2 decimals here: rounding happens only in [Fmt] when displaying/exporting.
 */
object Calc {
    /** Scale for unit prices (price / package quantity). */
    const val UNIT_PRICE_SCALE = 10

    /** Scale for the exact per-piece base quantity stored in history. */
    const val BASE_SCALE = 20

    /** Scale for scaled quantities (base * pieces); removes division artefacts like 99.999...9. */
    const val QTY_SCALE = 10

    /** Scale for divisions of money (cost per piece, margins). */
    const val DIV_SCALE = 20

    fun unitPrice(packagePrice: BigDecimal, packageQuantity: BigDecimal): BigDecimal {
        require(packageQuantity.signum() > 0) { "Package quantity must be > 0" }
        return packagePrice.divide(packageQuantity, UNIT_PRICE_SCALE, RoundingMode.HALF_UP)
    }

    /** Line cost at full precision (no rounding). */
    fun lineCost(quantity: BigDecimal, unitPrice: BigDecimal): BigDecimal = quantity.multiply(unitPrice)

    fun sum(values: Iterable<BigDecimal>): BigDecimal = values.fold(BigDecimal.ZERO, BigDecimal::add)

    fun costPerPiece(batchCost: BigDecimal, yieldPieces: Int): BigDecimal {
        require(yieldPieces >= 1) { "Yield must be >= 1" }
        return batchCost.divide(BigDecimal(yieldPieces), DIV_SCALE, RoundingMode.HALF_UP)
    }

    /** Exact per-piece base quantity: batch quantity / yield. */
    fun basePerPiece(batchQuantity: BigDecimal, yieldPieces: Int): BigDecimal {
        require(yieldPieces >= 1) { "Yield must be >= 1" }
        return batchQuantity.divide(BigDecimal(yieldPieces), BASE_SCALE, RoundingMode.HALF_UP)
    }

    /**
     * Quantity for [pieces] pieces computed from the stored per-piece base.
     * Ingredients: exactly proportional. Consumables: rounded UP to a whole number.
     * (Fixed-quantity consumables are never passed through here.)
     */
    fun scaledQuantity(basePerPiece: BigDecimal, pieces: Int, consumable: Boolean): BigDecimal {
        val q = basePerPiece.multiply(BigDecimal(pieces)).setScale(QTY_SCALE, RoundingMode.HALF_UP)
        return if (consumable) q.setScale(0, RoundingMode.CEILING) else plain(q.stripTrailingZeros())
    }

    /** Avoids exponent forms like 2.4E+2 so in-memory values equal their stored text. */
    private fun plain(v: BigDecimal): BigDecimal = if (v.scale() < 0) v.setScale(0) else v

    fun revenue(pricePerPiece: BigDecimal, pieces: Int): BigDecimal = pricePerPiece.multiply(BigDecimal(pieces))

    fun marginPercent(margin: BigDecimal, price: BigDecimal): BigDecimal? =
        if (price.signum() == 0) null
        else margin.multiply(BigDecimal(100)).divide(price, DIV_SCALE, RoundingMode.HALF_UP)
}
