package ro.favaron.costuri.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ro.favaron.costuri.domain.Calc
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.Order
import ro.favaron.costuri.domain.OrderFactory
import ro.favaron.costuri.domain.OrderProduct
import ro.favaron.costuri.domain.RecipeView
import ro.favaron.costuri.domain.Validate
import ro.favaron.costuri.ui.common.BoundDecimalField
import ro.favaron.costuri.ui.common.DateDialog
import ro.favaron.costuri.ui.common.LabeledValue
import ro.favaron.costuri.ui.common.NumberTextField
import ro.favaron.costuri.ui.common.SearchableDropdown
import ro.favaron.costuri.ui.common.Tag
import ro.favaron.costuri.ui.common.profitColor
import java.math.BigDecimal
import java.time.LocalDate

private data class DraftProduct(
    val key: Long,
    val recipeId: Long,
    val fallback: RecipeView,
    val piecesText: String,
    val priceText: String,
    /** Quantities the user adjusted by hand, by recipe line index. */
    val manual: Map<Int, BigDecimal> = emptyMap(),
)

/** "Comandă nouă": builds a snapshot from the current (valid) recipes. */
@Composable
fun NewOrderDialog(
    recipes: List<RecipeView>,
    onDismiss: () -> Unit,
    onSave: (Order) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var customer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val drafts = remember { mutableStateListOf<DraftProduct>() }
    var nextKey by remember { mutableLongStateOf(1L) }
    var query by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val byId = recipes.associateBy { it.recipe.id }
    // Build the live preview of every product line.
    val built: List<Pair<OrderProduct?, Pair<String?, String?>>> = drafts.map { d ->
        val recipe = byId[d.recipeId] ?: d.fallback
        val (pieces, pErr) = Validate.wholeNumber(d.piecesText, 1)
        val (price, prErr) = Validate.money(d.priceText)
        val product = if (pieces != null && price != null && recipe.canBeOrdered) {
            OrderFactory.buildProduct(recipe, pieces, price, d.manual, key = d.key)
        } else null
        product to (pErr to prErr)
    }
    val products = built.mapNotNull { it.first }
    val valid = drafts.isNotEmpty() && products.size == drafts.size
    val preview = Order(date = date, products = products)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize().imePadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Închide") }
                    Text("Comandă nouă", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        showErrors = true
                        if (valid) {
                            onSave(
                                Order(
                                    date = date, customer = customer.trim(), note = note.trim(),
                                    products = products.map { it.copy(key = 0) },
                                ),
                            )
                        }
                    }) { Text("Salvează comanda") }
                }
                HorizontalDivider()
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.align(Alignment.CenterVertically)) {
                                Icon(Icons.Filled.DateRange, null)
                                Text(" Data: ${Fmt.date(date)}")
                            }
                            OutlinedTextField(customer, { customer = it }, label = { Text("Client (opțional)") }, singleLine = true, modifier = Modifier.width(280.dp))
                            OutlinedTextField(note, { note = it }, label = { Text("Notă (opțional)") }, modifier = Modifier.width(380.dp))
                        }
                    }
                    item {
                        SearchableDropdown(
                            items = recipes,
                            itemLabel = { it.recipe.name },
                            itemEnabled = { it.canBeOrdered },
                            itemSupporting = {
                                if (!it.canBeOrdered) "Ingrediente lipsă — corectează rețeta înainte de vânzare"
                                else "Cost/buc ${Fmt.money(it.costPerPiece)} RON" +
                                    (it.recipe.sellingPrice?.let { p -> " · preț ${Fmt.money(p)} RON" } ?: "")
                            },
                            onSelect = { r ->
                                drafts.add(
                                    DraftProduct(
                                        key = -(nextKey++), recipeId = r.recipe.id, fallback = r, piecesText = "1",
                                        priceText = r.recipe.sellingPrice?.let { Fmt.edit(it) } ?: "",
                                    ),
                                )
                            },
                            label = "Adaugă produs (caută rețetă)",
                            query = query,
                            onQueryChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (showErrors && drafts.isEmpty()) {
                            Text("Adaugă cel puțin un produs.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    itemsIndexed(drafts, key = { _, d -> d.key }) { i, d ->
                        val (product, errs) = built[i]
                        DraftProductCard(
                            draft = d,
                            product = product,
                            piecesError = errs.first,
                            priceError = if (showErrors || d.priceText.isNotEmpty()) errs.second else null,
                            onChange = { drafts[i] = it },
                            onRemove = { drafts.removeAt(i) },
                        )
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                LabeledValue("Încasări", "${Fmt.money(preview.revenue)} RON", emphasize = true)
                                LabeledValue("Cost", "${Fmt.money(preview.cost)} RON", emphasize = true)
                                LabeledValue("Profit", "${Fmt.money(preview.profit)} RON", emphasize = true, valueColor = profitColor(preview.profit))
                            }
                        }
                    }
                }
            }
        }
    }
    if (pickDate) DateDialog(date, { pickDate = false }) { date = it }
}

@Composable
private fun DraftProductCard(
    draft: DraftProduct,
    product: OrderProduct?,
    piecesError: String?,
    priceError: String?,
    onChange: (DraftProduct) -> Unit,
    onRemove: () -> Unit,
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(draft.fallback.recipe.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                NumberTextField(
                    draft.piecesText, { onChange(draft.copy(piecesText = it)) }, "Bucăți vândute",
                    error = piecesError, integer = true, modifier = Modifier.width(150.dp),
                )
                NumberTextField(
                    draft.priceText, { onChange(draft.copy(priceText = it)) }, "Preț vânzare / buc",
                    error = priceError, suffix = "RON", modifier = Modifier.width(190.dp),
                )
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, "Elimină produsul") }
            }
            if (product == null) return@Column
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LabeledValue("Încasări", "${Fmt.money(product.revenue)} RON")
                LabeledValue("Cost", "${Fmt.money(product.cost)} RON")
                LabeledValue("Profit", "${Fmt.money(product.profit)} RON", valueColor = profitColor(product.profit))
                if (draft.manual.isNotEmpty()) {
                    TextButton(onClick = { onChange(draft.copy(manual = emptyMap())) }) {
                        Icon(Icons.Filled.Refresh, null)
                        Text(" Recalculează cantitățile")
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            product.lines.forEachIndexed { idx, l ->
                key(idx) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(l.name)
                            if (l.consumable) Tag(if (l.fixedQuantity) "Consumabil · fix" else "Consumabil")
                        }
                        BoundDecimalField(
                            value = l.quantity,
                            onCommit = { v -> onChange(draft.copy(manual = draft.manual + (idx to v))) },
                            label = "Cantitate",
                            suffix = l.unit,
                            validate = { Validate.nonNegative(it) },
                            modifier = Modifier.width(170.dp),
                        )
                        Column(Modifier.width(130.dp), horizontalAlignment = Alignment.End) {
                            Text("${Fmt.adaptive(l.unitPrice)} RON/${l.unit}", style = MaterialTheme.typography.bodySmall)
                            Text("${Fmt.adaptive(Calc.lineCost(l.quantity, l.unitPrice))} RON", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
