package ro.favaron.costuri.ui.ingredients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.favaron.costuri.data.IngredientEntity
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.unitPrice
import ro.favaron.costuri.ui.AppViewModels
import ro.favaron.costuri.ui.common.ConfirmDialog
import ro.favaron.costuri.ui.common.IngredientDialog
import ro.favaron.costuri.ui.common.ScreenHeader
import ro.favaron.costuri.ui.common.SearchField
import ro.favaron.costuri.ui.common.Tag
import ro.favaron.costuri.ui.common.csvFileName
import ro.favaron.costuri.ui.common.rememberCsvExport

@Composable
fun IngredientsScreen(vm: IngredientsViewModel = viewModel(factory = AppViewModels.Factory)) {
    val list by vm.visible.collectAsState()
    val query by vm.query.collectAsState()
    val asc by vm.ascending.collectAsState()
    val pendingDelete by vm.pendingDelete.collectAsState()
    var editing by remember { mutableStateOf<IngredientEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    val export = rememberCsvExport { vm.csv() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Ingrediente și consumabile") {
            OutlinedButton(onClick = { export(csvFileName("ingrediente")) }) {
                Text("Export CSV")
            }
            Button(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, null)
                Text(" Adaugă")
            }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(query, { vm.query.value = it }, Modifier.weight(1f))
            IconButton(onClick = { vm.ascending.value = !asc }) {
                Icon(if (asc) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, "Sortare")
            }
            Text(if (asc) "A→Z" else "Z→A", style = MaterialTheme.typography.labelLarge)
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (query.isBlank()) "Niciun ingredient. Apasă „Adaugă”." else "Nimic găsit.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(list, key = { it.id }) { ing ->
                    IngredientRow(ing, onEdit = { editing = ing }, onDelete = { vm.askDelete(ing) })
                }
            }
        }
    }

    if (adding || editing != null) {
        IngredientDialog(
            initial = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { vm.save(it); adding = false; editing = null },
        )
    }

    pendingDelete?.let { (ing, recipes) ->
        ConfirmDialog(
            title = "Ștergi „${ing.name}”?",
            text = if (recipes.isEmpty()) "Ingredientul nu este folosit în nicio rețetă."
            else "Atenție: ingredientul este folosit în ${recipes.size} rețete. Liniile respective vor fi marcate „lipsă” " +
                "și rețetele nu vor putea fi vândute până nu înlocuiești ingredientul. Comenzile salvate nu sunt afectate.",
            confirmLabel = "Șterge",
            onConfirm = { vm.confirmDelete() },
            onDismiss = { vm.pendingDelete.value = null },
            body = if (recipes.isEmpty()) null else ({
                Column { recipes.forEach { Text("• $it", fontWeight = FontWeight.SemiBold) } }
            }),
        )
    }
}

@Composable
private fun IngredientRow(ing: IngredientEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ing.name, style = MaterialTheme.typography.titleMedium)
                    if (ing.isConsumable) Tag("Consumabil")
                }
                Text(
                    "Pachet: ${Fmt.qty(ing.packageQuantity)} ${ing.unit} = ${Fmt.money(ing.packagePrice)} RON",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.widthIn(min = 140.dp), horizontalAlignment = Alignment.End) {
                Text("${Fmt.adaptive(ing.unitPrice())} RON", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("per ${ing.unit}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editează") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Șterge") }
        }
    }
}
