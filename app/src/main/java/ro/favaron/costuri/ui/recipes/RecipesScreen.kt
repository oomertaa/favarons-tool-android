package ro.favaron.costuri.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.RecipeView
import ro.favaron.costuri.ui.AppViewModels
import ro.favaron.costuri.ui.common.ConfirmDialog
import ro.favaron.costuri.ui.common.LabeledValue
import ro.favaron.costuri.ui.common.MissingTag
import ro.favaron.costuri.ui.common.ScreenHeader
import ro.favaron.costuri.ui.common.SearchField
import ro.favaron.costuri.ui.common.csvFileName
import ro.favaron.costuri.ui.common.profitColor
import ro.favaron.costuri.ui.common.rememberCsvExport

@Composable
fun RecipesScreen(vm: RecipesViewModel = viewModel(factory = AppViewModels.Factory)) {
    val list by vm.visible.collectAsState()
    val ingredients by vm.ingredients.collectAsState()
    val query by vm.query.collectAsState()
    val asc by vm.ascending.collectAsState()
    // null = closed; RecipeView with id 0 = new recipe
    var editing by remember { mutableStateOf<RecipeView?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RecipeView?>(null) }
    var scaling by remember { mutableStateOf<RecipeView?>(null) }
    val export = rememberCsvExport { vm.csv() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Rețete") {
            OutlinedButton(onClick = { export(csvFileName("retete")) }) {
                Text("Export CSV")
            }
            Button(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, null)
                Text(" Rețetă nouă")
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(query, { vm.query.value = it }, Modifier.weight(1f))
            IconButton(onClick = { vm.ascending.value = !asc }) { Icon(if (asc) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, "Sortare") }
            Text(if (asc) "A→Z" else "Z→A", style = MaterialTheme.typography.labelLarge)
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (query.isBlank()) "Nicio rețetă. Apasă „Rețetă nouă”." else "Nimic găsit.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(list, key = { it.recipe.id }) { r ->
                    RecipeCard(r, onEdit = { editing = r }, onScale = { scaling = r }, onDelete = { deleting = r })
                }
            }
        }
    }

    if (creating || editing != null) {
        RecipeEditorDialog(
            initial = editing,
            ingredients = ingredients,
            onCreateIngredient = vm::createIngredient,
            onDismiss = { creating = false; editing = null },
            onSave = { recipe, lines ->
                vm.save(recipe, lines)
                creating = false
                editing = null
            },
        )
    }

    scaling?.let { r ->
        ScaleRecipeDialog(
            recipeName = r.recipe.name,
            currentYield = r.recipe.yieldPieces,
            onDismiss = { scaling = null },
            onScale = { vm.scale(r.recipe.id, it) },
        )
    }

    deleting?.let { r ->
        ConfirmDialog(
            title = "Ștergi rețeta „${r.recipe.name}”?",
            text = "Comenzile deja salvate nu sunt afectate.",
            confirmLabel = "Șterge",
            onConfirm = { vm.delete(r.recipe.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RecipeCard(r: RecipeView, onEdit: () -> Unit, onScale: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.recipe.name, style = MaterialTheme.typography.titleMedium)
                    if (r.hasMissing) MissingTag()
                }
                if (r.hasMissing) {
                    Text(
                        "Are ingrediente lipsă: nu poate fi vândută până nu sunt înlocuite.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LabeledValue("Randament", "${r.recipe.yieldPieces} buc")
                    LabeledValue("Linii", r.lines.size.toString())
                    LabeledValue("Cost total rețetă", "${Fmt.money(r.batchCost)} RON")
                    LabeledValue("Cost / buc", "${Fmt.money(r.costPerPiece)} RON", emphasize = true)
                    r.recipe.sellingPrice?.let { LabeledValue("Preț vânzare / buc", "${Fmt.money(it)} RON") }
                    r.marginPerPiece?.let { m ->
                        LabeledValue(
                            "Marjă / buc",
                            "${Fmt.money(m)} RON" + (r.marginPercent?.let { " (${Fmt.percent(it)}%)" } ?: ""),
                            valueColor = profitColor(m),
                        )
                    }
                }
            }
            OutlinedButton(onClick = onScale) {
                Icon(Icons.Filled.Refresh, null)
                Text(" Scalează")
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editează") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Șterge") }
        }
    }
}
