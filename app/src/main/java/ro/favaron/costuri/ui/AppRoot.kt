package ro.favaron.costuri.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ro.favaron.costuri.ui.common.LocalSnackbar
import ro.favaron.costuri.ui.common.LocalWideLayout
import ro.favaron.costuri.ui.history.HistoryScreen
import ro.favaron.costuri.ui.ingredients.IngredientsScreen
import ro.favaron.costuri.ui.recipes.RecipesScreen

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val DESTS = listOf(
    Dest("ingrediente", "Ingrediente", Icons.Filled.List),
    Dest("retete", "Rețete", Icons.Filled.Star),
    Dest("vanzari", "Vânzări", Icons.Filled.ShoppingCart),
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar, LocalWideLayout provides wide) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (!wide) NavigationBar {
                    DESTS.forEach { d ->
                        NavigationBarItem(
                            selected = current == d.route,
                            onClick = { go(d.route) },
                            icon = { Icon(d.icon, null) },
                            label = { Text(d.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (wide) NavigationRail {
                    Spacer(Modifier.height(12.dp))
                    DESTS.forEach { d ->
                        NavigationRailItem(
                            selected = current == d.route,
                            onClick = { go(d.route) },
                            icon = { Icon(d.icon, null) },
                            label = { Text(d.label) },
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    NavHost(nav, startDestination = DESTS[0].route) {
                        composable(DESTS[0].route) { IngredientsScreen() }
                        composable(DESTS[1].route) { RecipesScreen() }
                        composable(DESTS[2].route) { HistoryScreen() }
                    }
                }
            }
        }
    }
}
