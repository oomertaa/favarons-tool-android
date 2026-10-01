package ro.favaron.costuri

import ro.favaron.costuri.data.IngredientEntity
import ro.favaron.costuri.data.RecipeEntity
import ro.favaron.costuri.data.RecipeLineEntity
import ro.favaron.costuri.domain.RecipeLineView
import ro.favaron.costuri.domain.RecipeView
import java.math.BigDecimal

fun bd(s: String) = BigDecimal(s)

/** Asserts numeric equality regardless of scale. */
fun assertBd(expected: String, actual: BigDecimal, msg: String = "") {
    org.junit.Assert.assertTrue("$msg expected $expected but was ${actual.toPlainString()}", bd(expected).compareTo(actual) == 0)
}

object TestData {
    val flour = IngredientEntity(1, "Făină", "g", bd("1000"), bd("12.50"), false)
    val butter = IngredientEntity(2, "Unt", "g", bd("250"), bd("9.99"), false)
    val box = IngredientEntity(3, "Cutie", "buc", bd("10"), bd("15.00"), true)
    val paper = IngredientEntity(4, "Hârtie de copt", "buc", bd("100"), bd("20.00"), true)
    val cup = IngredientEntity(5, "Pahar", "buc", bd("50"), bd("20.00"), true)

    fun line(i: IngredientEntity, qty: String, fixed: Boolean = false, pos: Int = 0) =
        RecipeLineEntity(id = pos.toLong() + 1, recipeId = 1, position = pos, ingredientId = i.id, ingredientName = i.name, unit = i.unit, quantity = bd(qty), fixedQuantity = fixed)

    /**
     * "Tort" makes 10 pieces: 400 g flour, 200 g butter, 10 boxes (scaled),
     * 3 baking papers (scaled, CEIL), 4 cups (fixed). Selling price 25.00.
     */
    fun cake(): RecipeView {
        val lines = listOf(
            line(flour, "400", pos = 0),
            line(butter, "200", pos = 1),
            line(box, "10", pos = 2),
            line(paper, "3", pos = 3),
            line(cup, "4", fixed = true, pos = 4),
        )
        val byId = listOf(flour, butter, box, paper, cup).associateBy { it.id }
        return RecipeView(RecipeEntity(1, "Tort", 10, bd("25.00")), lines.map { RecipeLineView(it, byId[it.ingredientId]) })
    }
}
