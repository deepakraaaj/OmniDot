package com.omnidot.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnidot.OmniDotViewModel
import com.omnidot.automation.HealthCheck
import com.omnidot.automation.HealthIssue
import com.omnidot.automation.SoundSynthesizer
import com.omnidot.automation.VoiceFeedbackManager
import com.omnidot.automation.VoiceStyle
import com.omnidot.ui.components.SectionHeader
import com.omnidot.ui.components.SurfaceCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(padding: PaddingValues, snackbar: SnackbarHostState, vm: OmniDotViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val key by vm.remoteKey.collectAsStateWithLifecycle()
    var probe by remember { mutableStateOf(HealthCheck.probe(context)) }
    LifecycleResumeEffect(Unit) { probe = HealthCheck.probe(context); onPauseOrDispose { } }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // After repeated denials Android returns immediately without a dialog; send the user to settings instead.
        if (!granted || !HealthCheck.probe(context).notificationsGranted) {
            context.startActivity(HealthCheck.fixIntent(context, HealthIssue.NOTIFICATIONS_BLOCKED))
        }
        probe = HealthCheck.probe(context)
    }

    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionHeader("Voice")
        SurfaceCard {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VoiceStyle.entries.forEachIndexed { i, style ->
                    SegmentedButton(prefs.voiceStyle == style, { vm.setVoiceStyle(style) }, SegmentedButtonDefaults.itemShape(i, VoiceStyle.entries.size)) { Text(style.label) }
                }
            }
            TextButton(onClick = {
                scope.launch {
                    SoundSynthesizer.playSoftBell(); delay(700)
                    VoiceFeedbackManager.speak(context, "Take a breath. You're doing well.", prefs.voiceStyle.pitch, prefs.voiceStyle.rate, prefs.voiceStyle.volume)
                }
            }) { Text("Preview") }
            Text("New speak steps use this style. More voices are coming.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionHeader("Check-in reminder")
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ask me every evening", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(prefs.reminderEnabled, { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 && !probe.notificationsGranted) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    vm.setReminderEnabled(on)
                })
            }
            val h = prefs.reminderMinuteOfDay / 60; val m = prefs.reminderMinuteOfDay % 60
            OutlinedButton(onClick = { TimePickerDialog(context, { _, hh, mm -> vm.setReminderTime(hh * 60 + mm) }, h, m, false).show() },
                enabled = prefs.reminderEnabled, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("At %02d:%02d".format(h, m))
            }
        }

        SectionHeader("Permissions")
        SurfaceCard {
            PermissionRow("Notifications", probe.notificationsGranted) {
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(HealthCheck.fixIntent(context, HealthIssue.NOTIFICATIONS_BLOCKED))
            }
            PermissionRow("Notification access (WhatsApp, alarms)", probe.listenerGranted) { context.startActivity(HealthCheck.fixIntent(context, HealthIssue.LISTENER_MISSING)) }
            PermissionRow("Background usage", probe.backgroundAllowed) { context.startActivity(HealthCheck.fixIntent(context, HealthIssue.BACKGROUND_RESTRICTED)) }
        }

        SectionHeader("Remote trigger")
        SurfaceCard {
            Text("Tasker, ADB and links must include this key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionContainer { Text(key, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp)) }
            Row {
                TextButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("OmniDot key", key))
                    scope.launch { snackbar.showSnackbar("Key copied") }
                }) { Text("Copy") }
                TextButton(onClick = vm::regenerateRemoteKey) { Text("New key") }
            }
            SelectionContainer {
                Text(
                    "adb shell am broadcast -n com.omnidot/.automation.AutomationReceiver -a com.omnidot.TRIGGER_ACTION --es key $key\n\nomnidot://trigger?action=refresh&key=$key",
                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionHeader("About")
        SurfaceCard {
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
            Text("OmniDot $version", style = MaterialTheme.typography.bodyLarge)
            Text("Local-first automations and goals. Apache License 2.0.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(if (granted) "Allowed" else "Not allowed", style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!granted) TextButton(onClick = onFix) { Text("Fix") }
    }
}
