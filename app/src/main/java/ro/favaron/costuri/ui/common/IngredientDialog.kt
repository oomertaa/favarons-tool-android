package ro.favaron.costuri.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ro.favaron.costuri.data.IngredientEntity
import ro.favaron.costuri.data.Units
import ro.favaron.costuri.domain.Calc
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.Validate

/**
 * Add/edit dialog for an ingredient or consumable. The SAME component is used
 * on the Ingredients page and from the recipe editor ("Adaugă ingredient").
 */
@Composable
fun IngredientDialog(
    initial: IngredientEntity?,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (IngredientEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: initialName) }
    var unit by remember { mutableStateOf(initial?.unit ?: "g") }
    var qtyText by remember { mutableStateOf(initial?.packageQuantity?.let { Fmt.edit(it) } ?: "") }
    var priceText by remember { mutableStateOf(initial?.packagePrice?.let { Fmt.edit(it) } ?: "") }
    var consumable by remember { mutableStateOf(initial?.isConsumable ?: false) }
    var showErrors by remember { mutableStateOf(false) }

    val nameError = if (name.isBlank()) "Obligatoriu" else null
    val (qty, qtyError) = Validate.positiveQuantity(qtyText)
    val (price, priceError) = Validate.money(priceText)
    val valid = nameError == null && qty != null && price != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Ingredient nou" else "Editează ingredient") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nume") },
                    singleLine = true,
                    isError = showErrors && nameError != null,
                    supportingText = if (showErrors && nameError != null) ({ Text(nameError) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Unitate de măsură", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Units.ALL.forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                    }
                }
                NumberTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = "Cantitate totală pachet",
                    suffix = unit,
                    error = if (showErrors || qtyText.isNotEmpty()) qtyError else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                NumberTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = "Preț total pachet",
                    suffix = "RON",
                    error = if (showErrors || priceText.isNotEmpty()) priceError else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                CheckboxRow(
                    checked = consumable,
                    onCheckedChange = { consumable = it },
                    label = "Consumabil",
                    supporting = "hârtie de copt, cutii, panglici, pahare…",
                )
                if (qty != null && price != null) {
                    Text(
                        "Preț per $unit: ${Fmt.adaptive(Calc.unitPrice(price, qty))} RON",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showErrors = true
                if (valid) {
                    onSave(
                        IngredientEntity(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            unit = unit,
                            packageQuantity = qty!!,
                            packagePrice = price!!,
                            isConsumable = consumable,
                        ),
                    )
                }
            }) { Text("Salvează") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}
