package ro.favaron.costuri.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ro.favaron.costuri.data.Units
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.HistoryDisplay
import ro.favaron.costuri.domain.Order
import ro.favaron.costuri.domain.OrderEditor
import ro.favaron.costuri.domain.OrderLine
import ro.favaron.costuri.domain.OrderProduct
import ro.favaron.costuri.domain.Validate
import ro.favaron.costuri.ui.common.BoundDecimalField
import ro.favaron.costuri.ui.common.BoundIntField
import ro.favaron.costuri.ui.common.BoundTextField
import ro.favaron.costuri.ui.common.CheckboxRow
import ro.favaron.costuri.ui.common.ConfirmDialog
import ro.favaron.costuri.ui.common.DateDialog
import ro.favaron.costuri.ui.common.LabeledValue
import ro.favaron.costuri.ui.common.NumberTextField
import ro.favaron.costuri.ui.common.Tag
import ro.favaron.costuri.ui.common.profitColor

@Composable
fun OrderCard(
    order: Order,
    expanded: Boolean,
    editing: Boolean,
    editLocked: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    vm: HistoryViewModel,
) {
    var pickDate by remember { mutableStateOf(false) }
    Card(
        colors = if (editing) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            // ---- header ----
            Row(
                Modifier.fillMaxWidth().clickable(enabled = !editing, onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1.4f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(HistoryDisplay.date(order), style = MaterialTheme.typography.titleMedium)
                        if (order.manuallyEdited) Tag("modificat manual", MaterialTheme.colorScheme.tertiaryContainer)
                    }
                    if (order.customer.isNotBlank()) Text(order.customer, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        order.products.joinToString(", ") { "${it.name} × ${it.pieces}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                LabeledValue("Încasări", "${Fmt.money(order.revenue)} RON", Modifier.weight(1f))
                LabeledValue("Cost", "${Fmt.money(order.cost)} RON", Modifier.weight(1f))
                LabeledValue("Profit", "${Fmt.money(order.profit)} RON", Modifier.weight(1f), emphasize = true, valueColor = profitColor(order.profit))
                if (!editing) Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
            }
            if (!expanded) return@Column

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (editing) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.align(Alignment.CenterVertically)) {
                        Icon(Icons.Filled.DateRange, null)
                        Text(" Data: ${Fmt.date(order.date)}")
                    }
                    BoundTextField(order.customer, { v -> vm.edit { OrderEditor.setCustomer(it, v) } }, "Client", Modifier.width(260.dp))
                    BoundTextField(order.note, { v -> vm.edit { OrderEditor.setNote(it, v) } }, "Notă", Modifier.width(360.dp), singleLine = false)
                }
            } else if (order.note.isNotBlank()) {
                Text("Notă: ${order.note}", style = MaterialTheme.typography.bodyMedium)
            }

            order.products.forEach { p ->
                key(p.key) { ProductSection(order, p, editing, vm) }
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (editing) {
                    TextButton(onClick = vm::cancelEdit) { Text("Anulează") }
                    Button(onClick = vm::saveEdit) { Text("Salvează modificările") }
                } else {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, null)
                        Text(" Șterge")
                    }
                    OutlinedButton(onClick = onEdit, enabled = !editLocked) {
                        Icon(Icons.Filled.Edit, null)
                        Text(" Editează")
                    }
                }
            }
        }
    }
    if (pickDate) DateDialog(order.date, { pickDate = false }) { d -> vm.edit { OrderEditor.setDate(it, d) } }
}

@Composable
private fun ProductSection(order: Order, p: OrderProduct, editing: Boolean, vm: HistoryViewModel) {
    var rescaleOpen by remember { mutableStateOf(false) }
    var addLineOpen by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 6.dp)) {
        if (editing) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BoundTextField(p.name, { v -> vm.edit { OrderEditor.setProductName(it, p.key, v) } }, "Produs", Modifier.width(260.dp))
                BoundIntField(p.pieces, { v -> vm.edit { OrderEditor.setPieces(it, p.key, v) } }, "Bucăți vândute", Modifier.width(150.dp))
                BoundDecimalField(
                    p.pricePerPiece, { v -> vm.edit { OrderEditor.setPrice(it, p.key, v) } }, "Preț vânzare / buc",
                    Modifier.width(190.dp), validate = { Validate.money(it) }, suffix = "RON",
                )
                OutlinedButton(onClick = { rescaleOpen = true }, modifier = Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.Filled.Refresh, null)
                    Text(" Rescalează automat")
                }
            }
            Text(
                "Modificarea bucăților schimbă doar încasările; pentru cantități folosește „Rescalează automat”.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "${p.name} — ${HistoryDisplay.pieces(p)} buc × ${HistoryDisplay.price(p)} RON",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            LabeledValue("Încasări produs", "${Fmt.money(p.revenue)} RON")
            LabeledValue("Cost produs", "${Fmt.money(p.cost)} RON")
            LabeledValue("Profit produs", "${Fmt.money(p.profit)} RON", valueColor = profitColor(p.profit))
            LabeledValue("Randament rețetă", "${p.recipeYield} buc")
        }

        if (editing) {
            p.lines.forEach { l -> key(l.key) { EditableLine(p, l, vm) } }
            TextButton(onClick = { addLineOpen = true }) {
                Icon(Icons.Filled.Add, null)
                Text(" Adaugă linie")
            }
        } else {
            LineHeader()
            p.lines.forEach { l -> ReadOnlyLine(l) }
        }
    }

    if (rescaleOpen) {
        RescaleDialog(p, onDismiss = { rescaleOpen = false }) { n -> vm.edit { OrderEditor.rescale(it, p.key, n) } }
    }
    if (addLineOpen) {
        AddLineDialog(onDismiss = { addLineOpen = false }) { name, unit, consumable, qty, price ->
            vm.edit { OrderEditor.addLine(it, p.key, vm.newKey(), name, unit, consumable, qty, price) }
        }
    }
}

private val COLS = listOf(2.2f, 0.7f, 0.9f, 1f, 1.1f, 1.1f, 1.1f)

@Composable
private fun LineHeader() {
    val labels = listOf("Ingredient", "Unitate", "Consumabil", "Cantitate", "Preț unitar", "Cost linie", "")
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        labels.forEachIndexed { i, s ->
            Text(
                s, Modifier.weight(COLS[i]), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                textAlign = if (i in 3..5) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun ReadOnlyLine(l: OrderLine) {
    val d = HistoryDisplay.line(l)
    val cells = listOf(
        d.name,
        d.unit,
        d.consumable,
        d.quantity,
        d.unitPrice,
        d.lineCost,
        listOfNotNull("fixă".takeIf { l.fixedQuantity }, "cost manual".takeIf { l.costManual }).joinToString(", "),
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        cells.forEachIndexed { i, s ->
            Text(
                s, Modifier.weight(COLS[i]).padding(start = if (i == 6) 12.dp else 0.dp),
                style = if (i == 6) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                textAlign = if (i in 3..5) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun EditableLine(p: OrderProduct, l: OrderLine, vm: HistoryViewModel) {
    FlowRow(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BoundTextField(l.name, { v -> vm.edit { OrderEditor.setLineName(it, p.key, l.key, v) } }, "Ingredient", Modifier.width(220.dp))
        BoundTextField(l.unit, { v -> vm.edit { OrderEditor.setLineUnit(it, p.key, l.key, v) } }, "Unitate", Modifier.width(90.dp))
        BoundDecimalField(
            l.quantity, { v -> vm.edit { OrderEditor.setQuantity(it, p.key, l.key, v) } }, "Cantitate",
            Modifier.width(140.dp), validate = { Validate.nonNegative(it) },
        )
        BoundDecimalField(
            l.unitPrice, { v -> vm.edit { OrderEditor.setUnitPrice(it, p.key, l.key, v) } }, "Preț unitar",
            Modifier.width(150.dp), validate = { Validate.nonNegative(it) },
        )
        BoundDecimalField(
            l.lineCost, { v -> vm.edit { OrderEditor.setLineCost(it, p.key, l.key, v) } },
            if (l.costManual) "Cost linie (manual)" else "Cost linie",
            Modifier.width(170.dp), validate = { Validate.nonNegative(it) },
        )
        Column(Modifier.align(Alignment.CenterVertically)) {
            Text(if (l.consumable) "Consumabil" else "Ingredient", style = MaterialTheme.typography.labelSmall)
            Text("Afișat: ${Fmt.adaptive(l.lineCost)}", style = MaterialTheme.typography.labelSmall)
        }
        if (l.consumable) {
            CheckboxRow(
                l.fixedQuantity, { f -> vm.edit { OrderEditor.setFixed(it, p.key, l.key, f) } }, "Cantitate fixă",
                Modifier.align(Alignment.CenterVertically), small = true,
            )
        }
        IconButton(onClick = { vm.edit { OrderEditor.deleteLine(it, p.key, l.key) } }, modifier = Modifier.align(Alignment.CenterVertically)) {
            Icon(Icons.Filled.Delete, "Șterge linia")
        }
    }
}

@Composable
private fun RescaleDialog(p: OrderProduct, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(p.pieces.toString()) }
    var confirm by remember { mutableStateOf<Int?>(null) }
    val (n, err) = Validate.wholeNumber(text, 1)
    if (confirm == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Rescalează „${p.name}”") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Număr nou de bucăți:")
                    NumberTextField(text, { text = it }, "Bucăți", error = err, integer = true)
                }
            },
            confirmButton = { TextButton(enabled = n != null, onClick = { confirm = n }) { Text("Continuă") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
        )
    } else {
        ConfirmDialog(
            title = "Confirmi rescalarea?",
            text = "Cantitățile liniilor fără „Cantitate fixă” vor fi recalculate pentru ${confirm} bucăți din cantitatea " +
                "de bază per bucată salvată în comandă. Modificările manuale ale cantităților și costurilor acestor linii " +
                "vor fi suprascrise. Costurile folosesc prețurile unitare salvate în comandă.",
            confirmLabel = "Rescalează",
            onConfirm = { onConfirm(confirm!!) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun AddLineDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean, java.math.BigDecimal, java.math.BigDecimal) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var consumable by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val (qty, qErr) = Validate.nonNegative(qtyText)
    val (price, pErr) = Validate.nonNegative(priceText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Linie nouă (text liber)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nume") }, singleLine = true,
                    isError = showErrors && name.isBlank())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(unit, { unit = it }, label = { Text("Unitate") }, singleLine = true, modifier = Modifier.width(100.dp))
                    Units.ALL.forEach { u -> TextButton(onClick = { unit = u }) { Text(u) } }
                }
                CheckboxRow(consumable, { consumable = it }, "Consumabil")
                NumberTextField(qtyText, { qtyText = it }, "Cantitate", error = if (showErrors || qtyText.isNotEmpty()) qErr else null)
                NumberTextField(priceText, { priceText = it }, "Preț unitar (RON / unitate)", error = if (showErrors || priceText.isNotEmpty()) pErr else null)
                Spacer(Modifier.width(1.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showErrors = true
                if (name.isNotBlank() && unit.isNotBlank() && qty != null && price != null) {
                    onAdd(name.trim(), unit.trim(), consumable, qty, price)
                    onDismiss()
                }
            }) { Text("Adaugă") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}
