package ro.favaron.costuri.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.Summary
import ro.favaron.costuri.ui.AppViewModels
import ro.favaron.costuri.ui.common.ConfirmDialog
import ro.favaron.costuri.ui.common.DateDialog
import ro.favaron.costuri.ui.common.LabeledValue
import ro.favaron.costuri.ui.common.ScreenHeader
import ro.favaron.costuri.ui.common.csvFileName
import ro.favaron.costuri.ui.common.profitColor
import ro.favaron.costuri.ui.common.rememberCsvExport
import ro.favaron.costuri.domain.Order
import java.time.LocalDate

@Composable
fun HistoryScreen(vm: HistoryViewModel = viewModel(factory = AppViewModels.Factory)) {
    val orders by vm.orders.collectAsState()
    val summary by vm.summary.collectAsState()
    val recipes by vm.recipes.collectAsState()
    val draft by vm.draft.collectAsState()
    val from by vm.from.collectAsState()
    val to by vm.to.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var showNew by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Order?>(null) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    val export = rememberCsvExport { vm.csv() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Istoric vânzări") {
            OutlinedButton(onClick = { export(csvFileName("vanzari")) }) {
                Text("Export CSV")
            }
            Button(onClick = { showNew = true }) {
                Icon(Icons.Filled.Add, null)
                Text(" Comandă nouă")
            }
        }
        FlowRow(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Perioadă:", modifier = Modifier.align(Alignment.CenterVertically))
            AssistChip(onClick = { pickFrom = true }, label = { Text("De la: " + (from?.let { Fmt.date(it) } ?: "—")) },
                leadingIcon = { Icon(Icons.Filled.DateRange, null) })
            AssistChip(onClick = { pickTo = true }, label = { Text("Până la: " + (to?.let { Fmt.date(it) } ?: "—")) },
                leadingIcon = { Icon(Icons.Filled.DateRange, null) })
            if (from != null || to != null) {
                TextButton(onClick = { vm.from.value = null; vm.to.value = null }) {
                    Icon(Icons.Filled.Clear, null)
                    Text(" Toate")
                }
            }
        }
        Box(Modifier.weight(1f)) {
            if (orders.isEmpty()) {
                Text(
                    "Nicio comandă în perioada selectată.",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(orders, key = { it.id }) { o ->
                        val editing = draft?.id == o.id
                        OrderCard(
                            order = if (editing) draft!! else o,
                            expanded = editing || o.id in expanded,
                            editing = editing,
                            editLocked = draft != null && !editing,
                            onToggle = { expanded = if (o.id in expanded) expanded - o.id else expanded + o.id },
                            onEdit = { vm.startEdit(o) },
                            onDelete = { deleting = o },
                            vm = vm,
                        )
                    }
                }
            }
        }
        SummaryPanel(summary)
    }

    if (showNew) {
        NewOrderDialog(
            recipes = recipes,
            onDismiss = { showNew = false },
            onSave = { vm.create(it); showNew = false },
        )
    }
    deleting?.let { o ->
        ConfirmDialog(
            title = "Ștergi comanda?",
            text = "Comanda din ${Fmt.date(o.date)}" + (if (o.customer.isNotBlank()) " (${o.customer})" else "") +
                " va fi ștearsă definitiv din istoric.",
            confirmLabel = "Șterge",
            onConfirm = { vm.delete(o.id) },
            onDismiss = { deleting = null },
        )
    }
    if (pickFrom) DateDialog(from ?: to ?: LocalDate.now(), { pickFrom = false }) { vm.from.value = it }
    if (pickTo) DateDialog(to ?: from ?: LocalDate.now(), { pickTo = false }) { vm.to.value = it }
}

@Composable
private fun SummaryPanel(s: Summary) {
    var open by rememberSaveable { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sumar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(0.6f))
                LabeledValue("Încasări totale", "${Fmt.money(s.revenue)} RON", Modifier.weight(1f), emphasize = true)
                LabeledValue("Cost ingrediente", "${Fmt.money(s.cost)} RON", Modifier.weight(1f), emphasize = true)
                LabeledValue("Profit", "${Fmt.money(s.profit)} RON", Modifier.weight(1f), emphasize = true, valueColor = profitColor(s.profit))
                IconButton(onClick = { open = !open }) {
                    Icon(if (open) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, "Pe produse")
                }
            }
            if (open) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    SummaryRow("Produs", "Bucăți", "Încasări", "Cost", "Profit", header = true)
                    if (s.products.isEmpty()) Text("—")
                    s.products.forEach { p ->
                        SummaryRow(p.name, p.pieces.toString(), Fmt.money(p.revenue), Fmt.money(p.cost), Fmt.money(p.profit))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(name: String, pieces: String, revenue: String, cost: String, profit: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val w = if (header) FontWeight.SemiBold else null
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(name, Modifier.weight(2f), style = style, fontWeight = w)
        Text(pieces, Modifier.weight(0.8f), style = style, fontWeight = w, textAlign = TextAlign.End)
        Text(revenue, Modifier.weight(1f), style = style, fontWeight = w, textAlign = TextAlign.End)
        Text(cost, Modifier.weight(1f), style = style, fontWeight = w, textAlign = TextAlign.End)
        Text(profit, Modifier.weight(1f), style = style, fontWeight = w, textAlign = TextAlign.End)
    }
}
