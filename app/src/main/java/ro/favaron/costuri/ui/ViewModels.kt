package ro.favaron.costuri.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ro.favaron.costuri.FavaronApp
import ro.favaron.costuri.ui.history.HistoryViewModel
import ro.favaron.costuri.ui.ingredients.IngredientsViewModel
import ro.favaron.costuri.ui.recipes.RecipesViewModel

object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { IngredientsViewModel((this[APPLICATION_KEY] as FavaronApp).liveRepository) }
        initializer { RecipesViewModel((this[APPLICATION_KEY] as FavaronApp).liveRepository) }
        initializer {
            val app = this[APPLICATION_KEY] as FavaronApp
            HistoryViewModel(app.historyRepository, app.liveRepository)
        }
    }
}
