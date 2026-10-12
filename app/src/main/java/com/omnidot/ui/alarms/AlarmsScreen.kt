package com.omnidot.ui.alarms

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omnidot.alarm.AlarmSchedule
import com.omnidot.alarm.CalmAlarm
import com.omnidot.alarm.DaysText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

@Composable
fun AlarmsScreen(padding: PaddingValues, snackbar: SnackbarHostState, vm: AlarmsViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CalmAlarm?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = ZonedDateTime.now() } }

    fun canNotify() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun canFullScreen() = Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    var notifyOk by remember { mutableStateOf(canNotify()) }
    var fullScreenOk by remember { mutableStateOf(canFullScreen()) }
    LifecycleResumeEffect(Unit) { notifyOk = canNotify(); fullScreenOk = canFullScreen(); onPauseOrDispose { } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifyOk = canNotify() }

    val next = alarms.filter { it.enabled }.map { AlarmSchedule.nextTrigger(now, it) }.minOrNull()

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Alarms", style = MaterialTheme.typography.headlineMedium)
                Text(next?.let { AlarmSchedule.ringsIn(now, it) } ?: "Gentle wake-ups that start quietly and greet you with your goals.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (alarms.isNotEmpty() && !notifyOk) item {
                Banner("Alarms need notifications", "Without them the alarm can't show or ring reliably.") {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
            if (alarms.isNotEmpty() && notifyOk && !fullScreenOk) item {
                Banner("Allow full-screen alarms", "So the alarm opens over your lock screen instead of a small notification.") {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
                }
            }
            if (alarms.isEmpty()) item {
                Text("No alarms yet. Add a calm wake-up: a soft bell that slowly rises, then a good-morning with today's goals.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
            }
            items(alarms, key = { it.id }) { alarm ->
                Surface(Modifier.fillMaxWidth().clickable { editing = alarm; editorOpen = true }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatClock(context, alarm.minuteOfDay), fontSize = 36.sp, fontWeight = FontWeight.Light,
                                color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(listOf(alarm.label.ifBlank { null }, DaysText.describe(alarm.days)).filterNotNull().joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(alarm.enabled, { vm.setEnabled(alarm, it) })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = null; editorOpen = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("New alarm") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (editorOpen) AlarmEditorSheet(
        initial = editing,
        onDismiss = { editorOpen = false },
        onSave = { alarm ->
            vm.save(alarm)
            editorOpen = false
            scope.launch { snackbar.showSnackbar(AlarmSchedule.ringsIn(ZonedDateTime.now(), AlarmSchedule.nextTrigger(ZonedDateTime.now(), alarm))) }
        },
        onDelete = { alarm ->
            vm.delete(alarm)
            editorOpen = false
            scope.launch {
                if (snackbar.showSnackbar("Alarm deleted", actionLabel = "Undo", withDismissAction = true) == SnackbarResult.ActionPerformed) vm.save(alarm)
            }
        },
    )
}

@Composable
private fun Banner(title: String, detail: String, onFix: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}
