package ro.favaron.costuri.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/** Ingredients and recipes (LIVE domain). Never touches history tables. */
class LiveRepository(private val db: AppDatabase) {
    private val dao = db.liveDao()

    val ingredients: Flow<List<IngredientEntity>> = dao.observeIngredients()
    val recipes: Flow<List<RecipeWithLines>> = dao.observeRecipes()

    /** Inserts (id == 0) or updates an ingredient; returns its id. */
    suspend fun saveIngredient(e: IngredientEntity): Long = db.withTransaction {
        if (e.id == 0L) {
            dao.insertIngredient(e)
        } else {
            dao.updateIngredient(e)
            dao.refreshLineNames(e.id, e.name, e.unit)
            e.id
        }
    }

    /** Names of the recipes that use the ingredient (for the delete warning). */
    suspend fun recipesUsing(ingredientId: Long): List<String> {
        val ids = dao.recipeIdsUsing(ingredientId)
        if (ids.isEmpty()) return emptyList()
        return dao.getRecipesByIds(ids).map { it.name }.sorted()
    }

    /**
     * Deleting is allowed even when used: affected recipe lines keep their stored
     * name/quantity and become "lipsă" (ingredientId = null).
     */
    suspend fun deleteIngredient(id: Long) = db.withTransaction {
        dao.markLinesMissing(id)
        dao.deleteIngredient(id)
    }

    /** Inserts or updates a recipe and replaces all its lines; returns the recipe id. */
    suspend fun saveRecipe(recipe: RecipeEntity, lines: List<RecipeLineEntity>): Long = db.withTransaction {
        require(recipe.yieldPieces >= 1)
        val id = if (recipe.id == 0L) dao.insertRecipe(recipe) else recipe.id.also { dao.updateRecipe(recipe) }
        dao.deleteLinesOf(id)
        dao.insertLines(lines.mapIndexed { i, l -> l.copy(id = 0, recipeId = id, position = i) })
        id
    }

    suspend fun deleteRecipe(id: Long) = db.withTransaction {
        dao.deleteLinesOf(id)
        dao.deleteRecipe(id)
    }

    suspend fun getIngredients() = dao.getIngredients()
    suspend fun getRecipes() = dao.getRecipes()
}
