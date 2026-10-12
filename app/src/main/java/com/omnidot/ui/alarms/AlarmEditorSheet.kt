package com.omnidot.ui.alarms

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidot.alarm.CalmAlarm
import com.omnidot.alarm.DaysText
import com.omnidot.automation.SoundSynthesizer
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AlarmEditorSheet(initial: CalmAlarm?, onDismiss: () -> Unit, onSave: (CalmAlarm) -> Unit, onDelete: (CalmAlarm) -> Unit) {
    val context = LocalContext.current
    var minute by remember { mutableStateOf(initial?.minuteOfDay ?: (6 * 60 + 30)) }
    var days by remember { mutableStateOf(initial?.days ?: DayOfWeek.entries.take(5).toSet()) }
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var snooze by remember { mutableStateOf(initial?.snoozeMinutes ?: 10) }
    var greeting by remember { mutableStateOf(initial?.speakGreeting ?: true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (initial == null) "New alarm" else "Edit alarm", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                TimePickerDialog(context, { _, h, m -> minute = h * 60 + m }, minute / 60, minute % 60, DateFormat.is24HourFormat(context)).show()
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(formatClock(context, minute), fontSize = 56.sp, fontWeight = FontWeight.Light)
            }
            Text("Repeat · ${DaysText.describe(days)}", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in days,
                        onClick = { days = if (day in days) days - day else days + day },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    )
                }
            }
            OutlinedTextField(label, { label = it.take(40) }, label = { Text("Label (optional)") }, placeholder = { Text("Morning run") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Snooze", style = MaterialTheme.typography.labelLarge)
            val options = listOf(5, 10, 15)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, m ->
                    SegmentedButton(snooze == m, { snooze = m }, SegmentedButtonDefaults.itemShape(i, options.size)) { Text("$m min") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Good-morning voice", style = MaterialTheme.typography.bodyLarge)
                    Text("After \"I'm up\", hear today's goals in a calm voice", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(greeting, { greeting = it })
            }
            TextButton(onClick = { SoundSynthesizer.playSoftBell() }) { Text("Hear the bell") }
            Button(
                onClick = {
                    onSave((initial ?: CalmAlarm(UUID.randomUUID().toString(), minute))
                        .copy(minuteOfDay = minute, days = days, label = label.trim(), snoozeMinutes = snooze, speakGreeting = greeting, enabled = true))
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Save alarm") }
            if (initial != null) TextButton(onClick = { onDelete(initial) }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete alarm", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun formatClock(context: android.content.Context, minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    if (DateFormat.is24HourFormat(context)) return DaysText.formatTime(minuteOfDay)
    val hour12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(hour12, m, if (h < 12) "AM" else "PM")
}
