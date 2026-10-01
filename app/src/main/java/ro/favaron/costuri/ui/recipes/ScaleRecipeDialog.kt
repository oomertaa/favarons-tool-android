package ro.favaron.costuri.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.RecipeScaling
import ro.favaron.costuri.domain.Validate
import ro.favaron.costuri.ui.common.NumberTextField

/** "Scalează": asks for the new number of pieces; quantities follow proportionally. */
@Composable
fun ScaleRecipeDialog(
    recipeName: String,
    currentYield: Int,
    onDismiss: () -> Unit,
    onScale: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(currentYield.toString()) }
    val (n, err) = Validate.wholeNumber(text, 1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scalează „$recipeName”") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Acum rețeta face $currentYield buc.")
                NumberTextField(text, { text = it }, "Număr nou de bucăți", error = err, integer = true, suffix = "buc")
                if (n != null) {
                    Text(
                        "Cantitățile se înmulțesc cu ${Fmt.qty(RecipeScaling.factor(currentYield, n))}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "Ingredientele se scalează exact proporțional, consumabilele se rotunjesc în sus la bucăți întregi, " +
                        "iar cele cu „Cantitate fixă” rămân neschimbate. Costurile se recalculează. " +
                        "Prețul de vânzare pe bucată nu se schimbă. Comenzile salvate nu sunt afectate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = n != null && n != currentYield, onClick = { onScale(n!!); onDismiss() }) { Text("Scalează") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}
