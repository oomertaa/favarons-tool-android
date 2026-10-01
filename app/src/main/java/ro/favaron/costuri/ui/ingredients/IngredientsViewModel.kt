package ro.favaron.costuri.ui.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.favaron.costuri.data.IngredientEntity
import ro.favaron.costuri.data.LiveRepository
import ro.favaron.costuri.domain.Csv
import ro.favaron.costuri.ui.common.RoCollator
import ro.favaron.costuri.ui.common.matchesSearch

class IngredientsViewModel(private val repo: LiveRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val ascending = MutableStateFlow(true)

    private val all: StateFlow<List<IngredientEntity>> =
        repo.ingredients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visible: StateFlow<List<IngredientEntity>> = combine(all, query, ascending) { list, q, asc ->
        val sorted = list.filter { it.name.matchesSearch(q) }.sortedWith { a, b -> RoCollator.compare(a.name, b.name) }
        if (asc) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Pending delete: ingredient + names of recipes that use it (warning). */
    val pendingDelete = MutableStateFlow<Pair<IngredientEntity, List<String>>?>(null)

    fun save(e: IngredientEntity) {
        viewModelScope.launch { repo.saveIngredient(e) }
    }

    fun askDelete(e: IngredientEntity) {
        viewModelScope.launch { pendingDelete.value = e to repo.recipesUsing(e.id) }
    }

    fun confirmDelete() {
        val p = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { repo.deleteIngredient(p.first.id) }
    }

    fun csv(): String = Csv.ingredients(visible.value)
}
