package ro.favaron.costuri.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveDao {
    @Query("SELECT * FROM ingredients")
    fun observeIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients")
    suspend fun getIngredients(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredient(id: Long): IngredientEntity?

    @Insert
    suspend fun insertIngredient(e: IngredientEntity): Long

    @Update
    suspend fun updateIngredient(e: IngredientEntity)

    @Query("DELETE FROM ingredients WHERE id = :id")
    suspend fun deleteIngredient(id: Long)

    @Query("UPDATE recipe_lines SET ingredientName = :name, unit = :unit WHERE ingredientId = :id")
    suspend fun refreshLineNames(id: Long, name: String, unit: String)

    @Query("UPDATE recipe_lines SET ingredientId = NULL WHERE ingredientId = :id")
    suspend fun markLinesMissing(id: Long)

    @Query("SELECT DISTINCT recipeId FROM recipe_lines WHERE ingredientId = :ingredientId")
    suspend fun recipeIdsUsing(ingredientId: Long): List<Long>

    @Transaction
    @Query("SELECT * FROM recipes")
    fun observeRecipes(): Flow<List<RecipeWithLines>>

    @Transaction
    @Query("SELECT * FROM recipes")
    suspend fun getRecipes(): List<RecipeWithLines>

    @Query("SELECT * FROM recipes WHERE id IN (:ids)")
    suspend fun getRecipesByIds(ids: List<Long>): List<RecipeEntity>

    @Insert
    suspend fun insertRecipe(e: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(e: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Insert
    suspend fun insertLines(lines: List<RecipeLineEntity>)

    @Query("DELETE FROM recipe_lines WHERE recipeId = :recipeId")
    suspend fun deleteLinesOf(recipeId: Long)
}

/** History DAO: single-table queries only (no joins). */
@Dao
interface HistoryDao {
    @Query("SELECT * FROM orders")
    suspend fun getOrders(): List<OrderEntity>

    @Query("SELECT * FROM order_products")
    suspend fun getProducts(): List<OrderProductEntity>

    @Query("SELECT * FROM order_ingredient_lines")
    suspend fun getLines(): List<OrderIngredientLineEntity>

    @Insert
    suspend fun insertOrder(e: OrderEntity): Long

    @Update
    suspend fun updateOrder(e: OrderEntity)

    @Insert
    suspend fun insertProduct(e: OrderProductEntity): Long

    @Insert
    suspend fun insertLines(lines: List<OrderIngredientLineEntity>)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrder(id: Long)

    @Query("DELETE FROM order_products WHERE orderId = :orderId")
    suspend fun deleteProductsOf(orderId: Long)

    @Query("DELETE FROM order_ingredient_lines WHERE orderId = :orderId")
    suspend fun deleteLinesOf(orderId: Long)
}
