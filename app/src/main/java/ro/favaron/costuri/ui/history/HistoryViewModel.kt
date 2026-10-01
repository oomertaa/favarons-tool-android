package ro.favaron.costuri.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.favaron.costuri.data.HistoryRepository
import ro.favaron.costuri.data.LiveRepository
import ro.favaron.costuri.domain.Csv
import ro.favaron.costuri.domain.Order
import ro.favaron.costuri.domain.RecipeView
import ro.favaron.costuri.domain.Summaries
import ro.favaron.costuri.domain.Summary
import ro.favaron.costuri.domain.buildRecipeViews
import ro.favaron.costuri.ui.common.RoCollator
import java.math.BigDecimal
import java.time.LocalDate

class HistoryViewModel(
    private val history: HistoryRepository,
    live: LiveRepository,
) : ViewModel() {
    val from = MutableStateFlow<LocalDate?>(null)
    val to = MutableStateFlow<LocalDate?>(null)

    /** Orders in the selected date range (all when no range), newest first. */
    val orders: StateFlow<List<Order>> = combine(history.orders, from, to) { all, f, t -> Summaries.filter(all, f, t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Summary computed only from stored history values. */
    val summary: StateFlow<Summary> = orders.map { Summaries.summarize(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Summary(BigDecimal.ZERO, BigDecimal.ZERO, emptyList()))

    /** Live recipes, used ONLY to build new orders. */
    val recipes: StateFlow<List<RecipeView>> = combine(live.recipes, live.ingredients) { r, i ->
        buildRecipeViews(r, i).sortedWith { a, b -> RoCollator.compare(a.recipe.name, b.recipe.name) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Order currently being edited (a working copy of the stored snapshot). */
    val draft = MutableStateFlow<Order?>(null)
    private var tempKey = -1_000_000L

    fun newKey(): Long = tempKey--

    fun startEdit(o: Order) {
        draft.value = o
    }

    fun cancelEdit() {
        draft.value = null
    }

    fun edit(f: (Order) -> Order) = draft.update { it?.let(f) }

    fun saveEdit() {
        val d = draft.value ?: return
        draft.value = null
        viewModelScope.launch { history.save(d) }
    }

    fun create(o: Order) {
        viewModelScope.launch { history.save(o) }
    }

    fun delete(id: Long) {
        if (draft.value?.id == id) draft.value = null
        viewModelScope.launch { history.delete(id) }
    }

    fun csv(): String = Csv.sales(orders.value)
}
