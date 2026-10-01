package ro.favaron.costuri.domain

import ro.favaron.costuri.data.IngredientEntity
import ro.favaron.costuri.data.RecipeEntity
import ro.favaron.costuri.data.RecipeLineEntity
import ro.favaron.costuri.data.RecipeWithLines
import java.math.BigDecimal

fun IngredientEntity.unitPrice(): BigDecimal = Calc.unitPrice(packagePrice, packageQuantity)

/** A recipe line joined (in memory) with its current live ingredient. */
data class RecipeLineView(val line: RecipeLineEntity, val ingredient: IngredientEntity?) {
    val missing: Boolean get() = ingredient == null
    val name: String get() = ingredient?.name ?: line.ingredientName
    val unit: String get() = ingredient?.unit ?: line.unit
    val consumable: Boolean get() = ingredient?.isConsumable ?: false
    val fixedQuantity: Boolean get() = consumable && line.fixedQuantity
    val unitPrice: BigDecimal? get() = ingredient?.unitPrice()
    val cost: BigDecimal? get() = unitPrice?.let { Calc.lineCost(line.quantity, it) }
}

/** Live costs of a recipe, computed from the CURRENT ingredient prices. */
data class RecipeView(val recipe: RecipeEntity, val lines: List<RecipeLineView>) {
    val hasMissing: Boolean get() = lines.any { it.missing }

    /** A recipe can be sold only when every line points to an existing ingredient. */
    val canBeOrdered: Boolean get() = !hasMissing

    val batchCost: BigDecimal get() = Calc.sum(lines.mapNotNull { it.cost })
    val costPerPiece: BigDecimal get() = Calc.costPerPiece(batchCost, recipe.yieldPieces)
    val marginPerPiece: BigDecimal? get() = recipe.sellingPrice?.subtract(costPerPiece)
    val marginPercent: BigDecimal?
        get() = recipe.sellingPrice?.let { p -> marginPerPiece?.let { Calc.marginPercent(it, p) } }
}

fun buildRecipeViews(recipes: List<RecipeWithLines>, ingredients: List<IngredientEntity>): List<RecipeView> {
    val byId = ingredients.associateBy { it.id }
    return recipes.map { rw ->
        RecipeView(
            recipe = rw.recipe,
            lines = rw.lines.sortedBy { it.position }.map { l ->
                RecipeLineView(l, l.ingredientId?.let { byId[it] })
            },
        )
    }
}
