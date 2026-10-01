package ro.favaron.costuri.ui.recipes

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ro.favaron.costuri.data.IngredientEntity
import ro.favaron.costuri.data.RecipeEntity
import ro.favaron.costuri.data.RecipeLineEntity
import ro.favaron.costuri.domain.Calc
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.RecipeScaling
import ro.favaron.costuri.domain.RecipeView
import ro.favaron.costuri.domain.Validate
import ro.favaron.costuri.domain.unitPrice
import ro.favaron.costuri.ui.common.CheckboxRow
import ro.favaron.costuri.ui.common.IngredientDialog
import ro.favaron.costuri.ui.common.LabeledValue
import ro.favaron.costuri.ui.common.MissingTag
import ro.favaron.costuri.ui.common.NumberTextField
import ro.favaron.costuri.ui.common.SearchableDropdown
import ro.favaron.costuri.ui.common.Tag
import ro.favaron.costuri.ui.common.profitColor
import java.math.BigDecimal

private data class EditLine(
    val key: Long,
    val ingredientId: Long?,
    val storedName: String,
    val storedUnit: String,
    val qtyText: String,
    val fixed: Boolean,
)

/** Full-screen recipe editor. Costs are computed live from the current ingredient prices. */
@Composable
fun RecipeEditorDialog(
    initial: RecipeView?,
    ingredients: List<IngredientEntity>,
    onCreateIngredient: suspend (IngredientEntity) -> IngredientEntity,
    onDismiss: () -> Unit,
    onSave: (RecipeEntity, List<RecipeLineEntity>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.recipe?.name ?: "") }
    var yieldText by remember { mutableStateOf(initial?.recipe?.yieldPieces?.toString() ?: "1") }
    var priceText by remember { mutableStateOf(initial?.recipe?.sellingPrice?.let { Fmt.edit(it) } ?: "") }
    var nextKey by remember { mutableLongStateOf(1L) }
    val lines = remember {
        mutableStateListOf<EditLine>().apply {
            initial?.lines?.forEach { lv ->
                add(
                    EditLine(
                        key = lv.line.id, ingredientId = lv.ingredient?.id, storedName = lv.line.ingredientName,
                        storedUnit = lv.line.unit, qtyText = Fmt.edit(lv.line.quantity), fixed = lv.line.fixedQuantity,
                    ),
                )
            }
        }
    }
    var showErrors by remember { mutableStateOf(false) }
    var addQuery by remember { mutableStateOf("") }
    var newIngredientName by remember { mutableStateOf<String?>(null) }
    var scaleOpen by remember { mutableStateOf(false) }
    var scaleError by remember { mutableStateOf<String?>(null) }

    val byId = ingredients.associateBy { it.id }
    val nameError = if (name.isBlank()) "Obligatoriu" else null
    val (yieldPieces, yieldError) = Validate.wholeNumber(yieldText, 1)
    val (price, priceError) = Validate.money(priceText, required = false)
    val lineQty = lines.map { Validate.positiveQuantity(it.qtyText) }
    val lineCosts = lines.mapIndexed { i, l ->
        val ing = l.ingredientId?.let { byId[it] }
        val q = lineQty[i].first
        if (ing != null && q != null) Calc.lineCost(q, ing.unitPrice()) else null
    }
    val batchCost = Calc.sum(lineCosts.filterNotNull())
    val costPerPiece = yieldPieces?.let { Calc.costPerPiece(batchCost, it) }
    val missingCount = lines.count { it.ingredientId == null || byId[it.ingredientId] == null }
    val valid = nameError == null && yieldPieces != null && priceError == null && lineQty.all { it.first != null }

    fun addLineFor(ing: IngredientEntity) {
        lines.add(EditLine(-(nextKey++), ing.id, ing.name, ing.unit, "", false))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize().imePadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Închide") }
                    Text(
                        if (initial == null) "Rețetă nouă" else "Editează rețeta",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        showErrors = true
                        if (valid) {
                            val recipe = RecipeEntity(
                                id = initial?.recipe?.id ?: 0, name = name.trim(),
                                yieldPieces = yieldPieces!!, sellingPrice = price,
                            )
                            val entities = lines.mapIndexed { i, l ->
                                val ing = l.ingredientId?.let { byId[it] }
                                RecipeLineEntity(
                                    recipeId = recipe.id, position = i, ingredientId = ing?.id,
                                    ingredientName = ing?.name ?: l.storedName, unit = ing?.unit ?: l.storedUnit,
                                    quantity = lineQty[i].first!!,
                                    fixedQuantity = if (ing != null) ing.isConsumable && l.fixed else l.fixed,
                                )
                            }
                            onSave(recipe, entities)
                        }
                    }) { Text("Salvează") }
                }
                HorizontalDivider()
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = name, onValueChange = { name = it }, label = { Text("Nume produs") },
                                singleLine = true, isError = showErrors && nameError != null,
                                supportingText = { if (showErrors && nameError != null) Text(nameError) },
                                modifier = Modifier.width(320.dp),
                            )
                            NumberTextField(
                                value = yieldText, onValueChange = { yieldText = it },
                                label = "Randament (bucăți)", integer = true, suffix = "buc",
                                error = yieldError, modifier = Modifier.width(200.dp),
                            )
                            OutlinedButton(
                                onClick = {
                                    scaleError = when {
                                        yieldPieces == null -> "Corectează întâi randamentul."
                                        lineQty.any { it.first == null } -> "Corectează întâi cantitățile liniilor."
                                        else -> null
                                    }
                                    if (scaleError == null) scaleOpen = true
                                },
                                modifier = Modifier.align(Alignment.CenterVertically),
                            ) {
                                Icon(Icons.Filled.Refresh, null)
                                Text(" Scalează")
                            }
                            NumberTextField(
                                value = priceText, onValueChange = { priceText = it },
                                label = "Preț vânzare / buc (opțional)", suffix = "RON",
                                error = priceError, modifier = Modifier.width(260.dp),
                            )
                        }
                        Text(
                            "Cantitățile de mai jos sunt pentru toată rețeta (toate bucățile). " +
                                "Modificarea directă a randamentului doar corectează numărul de bucăți; " +
                                "pentru a înmulți și cantitățile folosește „Scalează”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        scaleError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    item {
                        Text("Ingrediente", style = MaterialTheme.typography.titleMedium)
                        if (missingCount > 0) {
                            Text(
                                "$missingCount linii au ingrediente lipsă (șterse). Înlocuiește-le sau elimină-le " +
                                    "pentru a putea vinde rețeta.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    itemsIndexed(lines, key = { _, l -> l.key }) { i, l ->
                        val ing = l.ingredientId?.let { byId[it] }
                        LineEditor(
                            line = l,
                            ingredient = ing,
                            ingredients = ingredients,
                            qtyError = if (showErrors || l.qtyText.isNotEmpty()) lineQty[i].second else null,
                            cost = lineCosts[i],
                            onChange = { lines[i] = it },
                            onDelete = { lines.removeAt(i) },
                        )
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SearchableDropdown(
                                    items = ingredients,
                                    itemLabel = { it.name },
                                    itemSupporting = {
                                        "${it.unit} · ${Fmt.adaptive(it.unitPrice())} RON/${it.unit}" +
                                            if (it.isConsumable) " · consumabil" else ""
                                    },
                                    onSelect = { addLineFor(it) },
                                    label = "Adaugă linie: caută ingredient",
                                    query = addQuery,
                                    onQueryChange = { addQuery = it },
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedButton(onClick = { newIngredientName = addQuery }) {
                                    Icon(Icons.Filled.Add, null)
                                    Text(" Adaugă ingredient")
                                }
                            }
                        }
                    }
                    item {
                        Card {
                            FlowRow(
                                Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LabeledValue("Cost total rețetă", "${Fmt.money(batchCost)} RON", emphasize = true)
                                costPerPiece?.let { LabeledValue("Cost / buc", "${Fmt.money(it)} RON", emphasize = true) }
                                if (price != null && costPerPiece != null) {
                                    val margin = price.subtract(costPerPiece)
                                    val pct = Calc.marginPercent(margin, price)
                                    LabeledValue(
                                        "Marjă / buc",
                                        "${Fmt.money(margin)} RON" + (pct?.let { " (${Fmt.percent(it)}%)" } ?: ""),
                                        emphasize = true,
                                        valueColor = profitColor(margin),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (scaleOpen && yieldPieces != null) {
        ScaleRecipeDialog(
            recipeName = name.ifBlank { "rețeta" },
            currentYield = yieldPieces,
            onDismiss = { scaleOpen = false },
            onScale = { newYield ->
                lines.indices.forEach { i ->
                    val l = lines[i]
                    val ing = l.ingredientId?.let { byId[it] }
                    val consumable = ing?.isConsumable ?: false
                    val fixed = if (ing != null) consumable && l.fixed else l.fixed
                    val q = RecipeScaling.scaleQuantity(lineQty[i].first!!, yieldPieces, newYield, consumable, fixed)
                    lines[i] = l.copy(qtyText = Fmt.edit(q))
                }
                yieldText = newYield.toString()
            },
        )
    }

    newIngredientName?.let { initialName ->
        IngredientDialog(
            initial = null,
            initialName = initialName,
            onDismiss = { newIngredientName = null },
            onSave = { e ->
                newIngredientName = null
                scope.launch {
                    val saved = onCreateIngredient(e)
                    addLineFor(saved)
                    addQuery = ""
                }
            },
        )
    }
}

@Composable
private fun LineEditor(
    line: EditLine,
    ingredient: IngredientEntity?,
    ingredients: List<IngredientEntity>,
    qtyError: String?,
    cost: BigDecimal?,
    onChange: (EditLine) -> Unit,
    onDelete: () -> Unit,
) {
    var replaceQuery by remember { mutableStateOf("") }
    val missing = ingredient == null
    Card(
        colors = if (missing) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ingredient?.name ?: line.storedName, style = MaterialTheme.typography.titleSmall)
                        if (missing) MissingTag()
                        if (ingredient?.isConsumable == true) Tag("Consumabil")
                    }
                    ingredient?.let {
                        Text(
                            "${Fmt.adaptive(it.unitPrice())} RON/${it.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                NumberTextField(
                    value = line.qtyText,
                    onValueChange = { onChange(line.copy(qtyText = it)) },
                    label = "Cantitate",
                    suffix = ingredient?.unit ?: line.storedUnit,
                    error = qtyError,
                    modifier = Modifier.width(170.dp),
                )
                if (ingredient?.isConsumable == true) {
                    CheckboxRow(line.fixed, { onChange(line.copy(fixed = it)) }, "Cantitate fixă", small = true)
                }
                Column(Modifier.width(110.dp), horizontalAlignment = Alignment.End) {
                    Text("Cost", style = MaterialTheme.typography.labelSmall)
                    Text(cost?.let { "${Fmt.adaptive(it)} RON" } ?: "—", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Elimină linia") }
            }
            if (missing) {
                SearchableDropdown(
                    items = ingredients,
                    itemLabel = { it.name },
                    itemSupporting = { "${it.unit} · ${Fmt.adaptive(it.unitPrice())} RON/${it.unit}" },
                    onSelect = { onChange(line.copy(ingredientId = it.id, storedName = it.name, storedUnit = it.unit)) },
                    label = "Înlocuiește cu un ingredient existent",
                    query = replaceQuery,
                    onQueryChange = { replaceQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
