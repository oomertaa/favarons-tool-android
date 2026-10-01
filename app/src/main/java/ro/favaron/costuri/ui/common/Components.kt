package ro.favaron.costuri.ui.common

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.favaron.costuri.domain.Fmt
import ro.favaron.costuri.domain.Num
import java.math.BigDecimal
import java.text.Collator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

val LocalSnackbar = compositionLocalOf { SnackbarHostState() }
val LocalWideLayout = compositionLocalOf { true }

val RoCollator: Collator = Collator.getInstance(Locale.forLanguageTag("ro-RO")).apply { strength = Collator.PRIMARY }

/** Diacritics- and case-insensitive "contains" for search fields. */
fun String.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return norm(this).contains(norm(query.trim()))
}

@Composable
fun ScreenHeader(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Caută după nume") {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, "Șterge") }
        },
        placeholder = { Text(placeholder) },
    )
}

/** Small colored label ("Consumabil", "lipsă", "modificat manual"...). */
@Composable
fun Tag(text: String, container: Color = MaterialTheme.colorScheme.secondaryContainer, content: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun MissingTag() = Tag("lipsă", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false, valueColor: Color = Color.Unspecified) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else null,
            color = valueColor,
        )
    }
}

@Composable
fun profitColor(v: BigDecimal): Color =
    if (v.signum() < 0) MaterialTheme.colorScheme.error else Color.Unspecified

/** Plain text input for numbers (decimal keyboard; comma or dot accepted). */
@Composable
fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    integer: Boolean = false,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
    )
}

/**
 * Inline editor bound to a stored value: commits only valid input, and
 * re-syncs its text when the value is changed elsewhere (e.g. a recalculated
 * line cost or a rescale).
 */
@Composable
fun BoundDecimalField(
    value: BigDecimal,
    onCommit: (BigDecimal) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    validate: (String) -> Pair<BigDecimal?, String?>,
    suffix: String? = null,
) {
    var text by remember { mutableStateOf(Fmt.edit(value)) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(value) {
        val parsed = Num.parse(text)
        if (parsed == null || parsed.compareTo(value) != 0) {
            text = Fmt.edit(value)
            error = null
        }
    }
    NumberTextField(
        value = text,
        onValueChange = { t ->
            text = t
            val (v, e) = validate(t)
            error = e
            if (v != null && v.compareTo(value) != 0) onCommit(v)
        },
        label = label,
        modifier = modifier,
        error = error,
        suffix = suffix,
    )
}

@Composable
fun BoundIntField(value: Int, onCommit: (Int) -> Unit, label: String, modifier: Modifier = Modifier, min: Int = 1) {
    var text by remember { mutableStateOf(value.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(value) {
        if (Num.parseInt(text) != value) {
            text = value.toString()
            error = null
        }
    }
    NumberTextField(
        value = text,
        onValueChange = { t ->
            text = t
            val (v, e) = ro.favaron.costuri.domain.Validate.wholeNumber(t, min)
            error = e
            if (v != null && v != value) onCommit(v)
        },
        label = label,
        modifier = modifier,
        error = error,
        integer = true,
    )
}

@Composable
fun BoundTextField(value: String, onCommit: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onCommit,
        label = { Text(label, maxLines = 1) },
        modifier = modifier,
        singleLine = singleLine,
    )
}

/** Checkbox whose whole row (box + label) is one touch target. */
@Composable
fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    small: Boolean = false,
) {
    Row(
        modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
        Column {
            Text(label, style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    body: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text)
                body?.invoke()
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    ) { DatePicker(state) }
}

/**
 * Searchable dropdown: type to filter, pick an item. [trailing] can hold an
 * extra action (e.g. "Adaugă ingredient"); [onQueryChange] exposes the text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableDropdown(
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    itemEnabled: (T) -> Boolean = { true },
    itemSupporting: ((T) -> String?)? = null,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = items.filter { itemLabel(it).matchesSearch(query) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange(it); expanded = true },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("Nimic găsit") }, onClick = {}, enabled = false)
            }
            filtered.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(itemLabel(item))
                            itemSupporting?.invoke(item)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    enabled = itemEnabled(item),
                    onClick = {
                        onSelect(item)
                        onQueryChange("")
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Returns a launcher that asks the user where to save a CSV (Storage Access
 * Framework, no permission needed) and writes [content] there.
 */
@Composable
fun rememberCsvExport(content: () -> String): (fileName: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var pending by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val data = pending
        pending = null
        if (uri != null && data != null) {
            scope.launch {
                val ok = writeText(context, uri, data)
                snackbar.showSnackbar(if (ok) "Fișierul CSV a fost salvat" else "Eroare la salvarea fișierului")
            }
        }
    }
    return { name ->
        pending = content()
        launcher.launch(name)
    }
}

private suspend fun writeText(context: Context, uri: android.net.Uri, data: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(data.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
    }

fun csvFileName(prefix: String): String = "${prefix}_${LocalDate.now()}.csv"
