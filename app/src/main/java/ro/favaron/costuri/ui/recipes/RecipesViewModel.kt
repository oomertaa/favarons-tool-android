package ro.favaron.costuri.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.favaron.costuri.data.IngredientEntity
import ro.favaron.costuri.data.LiveRepository
import ro.favaron.costuri.data.RecipeEntity
import ro.favaron.costuri.data.RecipeLineEntity
import ro.favaron.costuri.domain.Csv
import ro.favaron.costuri.domain.RecipeView
import ro.favaron.costuri.domain.buildRecipeViews
import ro.favaron.costuri.ui.common.RoCollator
import ro.favaron.costuri.ui.common.matchesSearch

class RecipesViewModel(private val repo: LiveRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val ascending = MutableStateFlow(true)

    val ingredients: StateFlow<List<IngredientEntity>> = repo.ingredients
        .map { l -> l.sortedWith { a, b -> RoCollator.compare(a.name, b.name) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visible: StateFlow<List<RecipeView>> = combine(repo.recipes, ingredients, query, ascending) { r, i, q, asc ->
        val sorted = buildRecipeViews(r, i).filter { it.recipe.name.matchesSearch(q) }
            .sortedWith { a, b -> RoCollator.compare(a.recipe.name, b.recipe.name) }
        if (asc) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(recipe: RecipeEntity, lines: List<RecipeLineEntity>) {
        viewModelScope.launch { repo.saveRecipe(recipe, lines) }
    }

    fun scale(id: Long, newYield: Int) {
        viewModelScope.launch { repo.scaleRecipe(id, newYield) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteRecipe(id) }
    }

    /** Creates an ingredient from the recipe editor; returns the saved entity (with id). */
    suspend fun createIngredient(e: IngredientEntity): IngredientEntity = e.copy(id = repo.saveIngredient(e))

    fun csv(): String = Csv.recipes(visible.value)
}
