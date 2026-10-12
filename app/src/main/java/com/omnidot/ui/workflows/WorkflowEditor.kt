package com.omnidot.ui.workflows

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnidot.automation.*
import com.omnidot.core.MatrixPreset
import com.omnidot.alarm.DaysText
import com.omnidot.ui.MaxContentWidth
import com.omnidot.alarm.formatClock
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WorkflowEditor(
    initial: AutomationRule?,
    isEdit: Boolean,
    defaultVoice: VoiceStyle,
    onClose: () -> Unit,
    onSave: (AutomationRule) -> Unit,
    onTest: (AutomationRule) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rule by remember {
        mutableStateOf(initial ?: AutomationRule(
            name = "", trigger = AutomationTrigger.POWER_CONNECTED, action = AutomationAction.SPEAK_TEXT,
            speechPitch = defaultVoice.pitch, speechRate = defaultVoice.rate,
        ))
    }
    var sheet by remember { mutableStateOf<String?>(null) } // "trigger" | "action"
    var contactNote by remember { mutableStateOf<String?>(null) }
    var listenerGranted by remember { mutableStateOf(OmniNotificationListenerService.isNotificationAccessGranted(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        listenerGranted = OmniNotificationListenerService.isNotificationAccessGranted(context); onPauseOrDispose { }
    }
    BackHandler(onBack = onClose)

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: run { contactNote = "No contact was selected."; return@rememberLauncherForActivityResult }
        contactNote = try {
            context.contentResolver.query(uri, arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER,
            ), null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use "Could not read that contact. Please choose again."
                val name = c.getString(0).orEmpty()
                val number = c.getString(1).orEmpty()
                if (number.isBlank()) "That contact has no phone number." else { rule = rule.copy(contactName = name, contactPhone = number); null }
            } ?: "Could not read that contact. Please choose again."
        } catch (_: SecurityException) { "Android did not grant access to that contact." }
        catch (_: IllegalArgumentException) { "That contact is no longer available." }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit workflow" else "New workflow") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = MaxContentWidth).navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onTest(rule) }, enabled = WorkflowText.canSave(rule), modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Test")
                    }
                    Button(
                        onClick = { onSave(rule.copy(name = WorkflowText.name(rule))) },
                        enabled = WorkflowText.canSave(rule),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) { Text("Save") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().wrapContentWidth().widthIn(max = MaxContentWidth).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("When", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            ChoiceRow(triggerIcon(rule.trigger), WorkflowText.whenText(rule), rule.trigger.subtitle) { sheet = "trigger" }
            if (rule.trigger.needsAppAlive) Note("Works while OmniDot is running. Background reliability comes in the next update.")
            if (rule.trigger == AutomationTrigger.SCHEDULED_TIME) {
                val minute = DaysText.parseTime(rule.scheduledTime) ?: (7 * 60)
                OutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(context, { _, h, m -> rule = rule.copy(scheduledTime = DaysText.formatTime(h * 60 + m)) },
                            minute / 60, minute % 60, android.text.format.DateFormat.is24HourFormat(context)).show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("At ${formatClock(context, minute)}") }
                Text("Repeat · ${if (rule.days.isEmpty()) "Every day" else DaysText.describe(rule.days)}",
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    java.time.DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = rule.days.isEmpty() || day in rule.days,
                            onClick = {
                                val current = if (rule.days.isEmpty()) java.time.DayOfWeek.entries.toSet() else rule.days
                                val next = if (day in current) current - day else current + day
                                rule = rule.copy(days = if (next.size == 7) emptySet() else next)
                            },
                            label = { Text(day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())) },
                        )
                    }
                }
            }
            if (rule.trigger == AutomationTrigger.WHATSAPP_MESSAGE) {
                Field(rule.contactName, { rule = rule.copy(contactName = it) }, "Message from", "Contact name (leave empty for anyone)")
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Only messages with a link", style = MaterialTheme.typography.bodyLarge)
                        Text("For meeting links, videos, articles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = rule.onlyLinks, onCheckedChange = { rule = rule.copy(onlyLinks = it) })
                }
            }

            Text("Do", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            ChoiceRow(actionIcon(rule.action), WorkflowText.doText(rule), rule.action.subtitle) { sheet = "action" }
            ActionFields(rule, onChange = { rule = it }, contactNote = contactNote, onPickContact = {
                contactNote = null
                contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            }, onPreview = {
                scope.launch {
                    if (rule.playChime) { SoundSynthesizer.playSoftBell(); kotlinx.coroutines.delay(700) }
                    VoiceFeedbackManager.speak(context, rule.targetMessage.ifBlank { "This is how I sound" }, rule.speechPitch, rule.speechRate, defaultVoice.volume)
                }
            })
            Spacer(Modifier.height(24.dp))
        }
    }

    when (sheet) {
        "trigger" -> ChoiceSheet(
            title = "When should it run?",
            items = AutomationTrigger.pickerOptions + listOfNotNull(initial?.trigger?.takeIf { !it.available }),
            categoryOf = { it.category.label }, label = { it.label }, subtitle = { it.subtitle }, icon = { triggerIcon(it) },
            locked = { it in HealthCheck.listenerTriggers && !listenerGranted },
            onUnlock = { OmniNotificationListenerService.openNotificationAccessSettings(context) },
            onSelect = { rule = rule.copy(trigger = it); sheet = null },
            onDismiss = { sheet = null },
        )
        "action" -> ChoiceSheet(
            title = "What should it do?",
            items = AutomationAction.workflowOptions + listOfNotNull(initial?.action?.takeIf { it !in AutomationAction.workflowOptions }),
            categoryOf = { it.category.label }, label = { it.label }, subtitle = { it.subtitle }, icon = { actionIcon(it) },
            locked = { false }, onUnlock = {},
            onSelect = { picked ->
                rule = rule.copy(action = picked, code = if (picked == AutomationAction.RUN_CODE && rule.code.isBlank()) com.omnidot.script.ScriptTemplates.boilerplate else rule.code)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
    }
}

@Composable
private fun ChoiceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, placeholder: String, minLines: Int = 1) {
    OutlinedTextField(value, onChange, label = { Text(label) }, placeholder = { Text(placeholder) },
        minLines = minLines, singleLine = minLines == 1, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ActionFields(
    rule: AutomationRule,
    onChange: (AutomationRule) -> Unit,
    contactNote: String?,
    onPickContact: () -> Unit,
    onPreview: () -> Unit,
) {
    when (rule.action) {
        AutomationAction.SPEAK_TEXT -> {
            Field(rule.targetMessage, { onChange(rule.copy(targetMessage = it)) }, "Words to speak", "Charging started", minLines = 2)
            Note("Tip: {greeting} speaks a good morning with today's goals.")
            Text("Voice", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VoiceStyle.entries.forEachIndexed { i, style ->
                    SegmentedButton(
                        selected = rule.speechPitch == style.pitch && rule.speechRate == style.rate,
                        onClick = { onChange(rule.copy(speechPitch = style.pitch, speechRate = style.rate)) },
                        shape = SegmentedButtonDefaults.itemShape(i, VoiceStyle.entries.size),
                    ) { Text(style.label) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Soft bell first", style = MaterialTheme.typography.bodyLarge)
                    Text("A gentle tone before speaking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = rule.playChime, onCheckedChange = { onChange(rule.copy(playChime = it)) })
            }
            TextButton(onClick = onPreview, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Preview voice")
            }
        }
        AutomationAction.RUN_CODE -> CodeEditor(code = rule.code, ruleId = rule.id) { onChange(rule.copy(code = it)) }
        AutomationAction.SET_PHONE_ALARM -> {
            Text("Ring after", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            val options = listOf(5, 10, 20, 30, 60)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, m ->
                    SegmentedButton(rule.alarmInMinutes == m, { onChange(rule.copy(alarmInMinutes = m)) }, SegmentedButtonDefaults.itemShape(i, options.size)) {
                        Text(if (m < 60) "$m min" else "1 h", maxLines = 1)
                    }
                }
            }
            Field(rule.alarmLabel, { onChange(rule.copy(alarmLabel = it.take(60))) }, "Alarm label", "Link from {sender}")
            Note("Sets a one-time alarm in your Clock app. Variables: {sender} {link} {message}")
        }
        AutomationAction.RING_ALARM -> {
            Field(rule.alarmLabel, { onChange(rule.copy(alarmLabel = it.take(40))) }, "Label (optional)", "Morning run")
            Text("Snooze", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            val options = listOf(5, 10, 15)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, m ->
                    SegmentedButton(rule.snoozeMinutes == m, { onChange(rule.copy(snoozeMinutes = m)) }, SegmentedButtonDefaults.itemShape(i, options.size)) { Text("$m min") }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Good-morning voice", style = MaterialTheme.typography.bodyLarge)
                    Text("After \"I'm up\", hear today's goals in a calm voice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = rule.speakGreeting, onCheckedChange = { onChange(rule.copy(speakGreeting = it)) })
            }
            TextButton(onClick = { SoundSynthesizer.playSoftBell() }) { Text("Hear the bell") }
            Note(if (rule.trigger == AutomationTrigger.SCHEDULED_TIME) "Starts at 5% volume and rises over 90 seconds. Rings even on silent, until you tap Snooze or I'm up."
                 else "Tip: choose \"At a time\" above to use this as your wake-up alarm.")
        }
        AutomationAction.SEND_WHATSAPP, AutomationAction.CALL_PHONE -> {
            Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (rule.contactPhone.isBlank()) "No contact chosen" else "${rule.contactName} · ${rule.contactPhone}", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onPickContact) { Text(if (rule.contactPhone.isBlank()) "Choose contact" else "Change contact") }
                    contactNote?.let { Note(it) }
                }
            }
            Field(rule.targetMessage, { onChange(rule.copy(targetMessage = it)) },
                if (rule.action == AutomationAction.CALL_PHONE) "Say aloud on this phone" else "WhatsApp message", "Running 5 minutes late", minLines = 2)
            Note(if (rule.action == AutomationAction.CALL_PHONE) "Opens the dialer and speaks this on your phone. Android doesn't send app speech into calls."
                 else "Opens WhatsApp with your message ready. You tap Send.")
        }
        AutomationAction.HTTP_REQUEST, AutomationAction.HTTP_POST, AutomationAction.DISCORD_WEBHOOK,
        AutomationAction.SLACK_WEBHOOK, AutomationAction.OPEN_URL -> {
            Field(rule.url, { onChange(rule.copy(url = it.trim())) }, "URL", when (rule.action) {
                AutomationAction.DISCORD_WEBHOOK -> "https://discord.com/api/webhooks/…"
                AutomationAction.SLACK_WEBHOOK -> "https://hooks.slack.com/services/…"
                else -> "https://example.com/webhook"
            })
            if (rule.action == AutomationAction.HTTP_POST) {
                Field(rule.customPayload, { onChange(rule.copy(customPayload = it)) }, "JSON body (optional)", "{ \"battery\": \"{battery}\" }", minLines = 3)
            }
            if (rule.action == AutomationAction.DISCORD_WEBHOOK || rule.action == AutomationAction.SLACK_WEBHOOK) {
                Field(rule.message, { onChange(rule.copy(message = it)) }, "Message", "Battery {battery}%")
            }
            Note("Variables: {battery} {date} {year_percent} {time} {trigger}")
        }
        AutomationAction.SHOW_NOTIFICATION -> {
            Field(rule.message, { onChange(rule.copy(message = it)) }, "Notification text", "Battery is at {battery}%")
            Note("Variables: {battery} {date} {year_percent} {time} {trigger}")
        }
        AutomationAction.APPLY_PRESET -> {
            Text("Lock screen view", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            MatrixPreset.entries.forEach { p ->
                Row(Modifier.fillMaxWidth().clickable { onChange(rule.copy(preset = p)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = rule.preset == p, onClick = { onChange(rule.copy(preset = p)) })
                    Text(p.title)
                }
            }
        }
        else -> Unit
    }
}
