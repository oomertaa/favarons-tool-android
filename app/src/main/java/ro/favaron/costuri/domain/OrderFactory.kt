package ro.favaron.costuri.domain

import java.math.BigDecimal

/**
 * Builds the snapshot of a product line for a NEW order from a live recipe.
 * This is the only place where live data flows into history; after saving,
 * the snapshot never looks at live data again.
 */
object OrderFactory {

    class MissingIngredientException(recipe: String) :
        IllegalArgumentException("Rețeta \"$recipe\" are ingrediente lipsă")

    /** Quantity for one recipe line when selling [pieces] pieces. */
    fun quantityFor(line: RecipeLineView, yieldPieces: Int, pieces: Int): BigDecimal {
        val batch = line.line.quantity
        return if (line.fixedQuantity) batch
        else Calc.scaledQuantity(Calc.basePerPiece(batch, yieldPieces), pieces, line.consumable)
    }

    /**
     * @param manualQuantities quantities the user typed in the new-order dialog,
     *   by line index; they win over the computed ones.
     */
    fun buildProduct(
        recipe: RecipeView,
        pieces: Int,
        pricePerPiece: BigDecimal,
        manualQuantities: Map<Int, BigDecimal> = emptyMap(),
        key: Long = 0,
    ): OrderProduct {
        if (!recipe.canBeOrdered) throw MissingIngredientException(recipe.recipe.name)
        require(pieces >= 1) { "Pieces must be >= 1" }
        val y = recipe.recipe.yieldPieces
        val lines = recipe.lines.mapIndexed { i, lv ->
            val ing = lv.ingredient!!
            val unitPrice = lv.unitPrice!!
            val manual = manualQuantities[i]
            val qty = manual ?: quantityFor(lv, y, pieces)
            OrderLine(
                key = -(i + 1).toLong(),
                name = ing.name,
                unit = ing.unit,
                consumable = ing.isConsumable,
                // Manually typing a consumable's quantity fixes it (same rule as editing).
                fixedQuantity = lv.fixedQuantity || (manual != null && ing.isConsumable),
                quantity = qty,
                basePerPiece = Calc.basePerPiece(lv.line.quantity, y),
                unitPrice = unitPrice,
                lineCost = Calc.lineCost(qty, unitPrice),
            )
        }
        return OrderProduct(
            key = key,
            name = recipe.recipe.name,
            pieces = pieces,
            pricePerPiece = pricePerPiece,
            recipeYield = y,
            lines = lines,
        )
    }
}
