package com.omnidot.ui.goals

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnidot.core.AdaptiveMatrixEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.omnidot.core.LockPeriod
import com.omnidot.core.MatrixPreset
import com.omnidot.data.OmniDotPreferences
import com.omnidot.goals.Goal
import com.omnidot.ui.components.PhonePreview
import com.omnidot.ui.components.SectionHeader
import com.omnidot.ui.components.SurfaceCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockscreenSection(prefs: OmniDotPreferences, goals: List<Goal>, status: ApplyStatus, vm: GoalsViewModel) {
    val context = LocalContext.current
    var offset by remember(prefs.verticalOffset) { mutableFloatStateOf(prefs.verticalOffset) }
    var scale by remember(prefs.matrixScale) { mutableFloatStateOf(prefs.matrixScale) }
    val metrics = LocalContext.current.resources.displayMetrics
    val previewHeight = (432f * maxOf(metrics.widthPixels, metrics.heightPixels) / minOf(metrics.widthPixels, metrics.heightPixels)).toInt()
    // Render on a background thread so dragging the sliders stays smooth.
    val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, prefs, goals, offset, scale, previewHeight) {
        value = withContext(Dispatchers.Default) {
            AdaptiveMatrixEngine().render(android.util.Size(432, previewHeight), prefs.copy(verticalOffset = offset, matrixScale = scale), goals).asImageBitmap()
        }
    }
    SectionHeader("Lock screen")
    preview?.let { PhonePreview(it) }
    Spacer(Modifier.height(16.dp))
    SurfaceCard {
        Dropdown("Show", MatrixPreset.entries, prefs.preset, { it.title }, vm::setPreset)
        if (prefs.preset == MatrixPreset.GOALS) Dropdown("Period", LockPeriod.entries, prefs.lockPeriod, { it.label }, vm::setPeriod)
        if (prefs.preset == MatrixPreset.LIFE_IN_WEEKS) {
            OutlinedButton(onClick = {
                val d = prefs.birthDate
                DatePickerDialog(context, { _, y, m, day -> vm.setBirthDate(LocalDate.of(y, m + 1, day)) }, d.year, d.monthValue - 1, d.dayOfMonth)
                    .apply { datePicker.maxDate = System.currentTimeMillis() }.show()
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Birth date · ${prefs.birthDate}") }
        }
        Text("Position", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
        Slider(offset, { offset = it }, valueRange = -.15f..0.15f, onValueChangeFinished = { vm.setOffset(offset) })
        Text("Size", style = MaterialTheme.typography.labelLarge)
        Slider(scale, { scale = it }, valueRange = .6f..1.6f, onValueChangeFinished = { vm.setScale(scale) })
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Update every midnight", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(prefs.automaticUpdates, vm::setAutomaticUpdates)
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = { vm.apply() }, enabled = status !is ApplyStatus.Applying, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(if (status is ApplyStatus.Applying) "Applying…" else "Apply to lock screen")
    }
    when (status) {
        ApplyStatus.Applied -> Text("Lock screen updated. It refreshes after each check-in.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        is ApplyStatus.Error -> Text(status.message, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Dropdown(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = text(selected), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(text(it)) }, onClick = { onSelect(it); expanded = false }) }
        }
    }
}
