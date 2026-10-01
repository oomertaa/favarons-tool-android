package ro.favaron.costuri.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.math.BigDecimal

// ---------------------------------------------------------------------------
// LIVE data domain: ingredients (nomenclator) and recipes. Freely editable.
// Nothing in the HISTORY domain references these tables.
// ---------------------------------------------------------------------------

object Units {
    val ALL = listOf("g", "ml", "buc")
}

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** "g", "ml" or "buc". */
    val unit: String,
    val packageQuantity: BigDecimal,
    val packagePrice: BigDecimal,
    val isConsumable: Boolean,
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** The recipe makes this many pieces (>= 1). Line quantities are for the whole batch. */
    val yieldPieces: Int,
    /** Default selling price per piece; optional. */
    val sellingPrice: BigDecimal?,
)

@Entity(
    tableName = "recipe_lines",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("recipeId"), Index("ingredientId")],
)
data class RecipeLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val position: Int,
    /** Null when the ingredient was deleted: the line is "lipsă" (missing). */
    val ingredientId: Long?,
    /** Last known ingredient name/unit, shown for missing lines. */
    val ingredientName: String,
    val unit: String,
    /** Quantity for the whole batch. */
    val quantity: BigDecimal,
    /** "Cantitate fixă": consumable not scaled by pieces. */
    val fixedQuantity: Boolean,
)

data class RecipeWithLines(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val lines: List<RecipeLineEntity>,
)
