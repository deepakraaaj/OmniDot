package com.omnidot.ui.goals

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnidot.goals.EmojiInput
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalTier
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorSheet(initial: Goal?, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var tier by remember { mutableStateOf(initial?.tier ?: GoalTier.SMALL) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: tier.defaultEmoji) }
    var start by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var end by remember { mutableStateOf(initial?.endDate) }

    fun pick(current: LocalDate, min: LocalDate?, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(context, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) }, current.year, current.monthValue - 1, current.dayOfMonth).apply {
            min?.let { datePicker.minDate = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
        }.show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (initial == null) "New goal" else "Edit goal", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(emoji, { emoji = EmojiInput.sanitize(it) }, label = { Text("Icon") }, singleLine = true, modifier = Modifier.width(88.dp))
                OutlinedTextField(title, { title = it.take(60) }, label = { Text("Goal") }, placeholder = { Text("Run 5 km") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Text("Size", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalTier.entries.forEach { t ->
                    val selected = tier == t
                    Surface(
                        modifier = Modifier.weight(1f).clickable {
                            if (emoji == tier.defaultEmoji) emoji = t.defaultEmoji
                            tier = t
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) t.color().copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (selected) BorderStroke(2.dp, t.color()) else null,
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t.defaultEmoji, style = MaterialTheme.typography.headlineSmall)
                            TierShape(t, 14.dp, Modifier.padding(vertical = 6.dp))
                            Text(t.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { pick(start, null) { start = it; if (end?.isBefore(it) == true) end = it } }, modifier = Modifier.fillMaxWidth()) {
                Text("Starts ${start.format(dateFormat)}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pick(end ?: start.plusDays(29), start) { end = it } }, modifier = Modifier.weight(1f)) {
                    Text(end?.let { "Ends ${it.format(dateFormat)}" } ?: "No end date")
                }
                if (end != null) TextButton(onClick = { end = null }) { Text("Clear") }
            }
            Button(
                onClick = {
                    onSave((initial ?: Goal(UUID.randomUUID().toString(), "", "", tier, start, null, emptySet()))
                        .copy(title = title.trim(), emoji = emoji.ifBlank { tier.defaultEmoji }, tier = tier, startDate = start, endDate = end))
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Save goal") }
        }
    }
}
