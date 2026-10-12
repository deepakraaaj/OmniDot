package com.omnidot.ui.goals

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalDays
import com.omnidot.ui.components.SectionHeader
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun GoalsScreen(padding: PaddingValues, snackbar: SnackbarHostState, vm: GoalsViewModel = viewModel()) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val status by vm.applyStatus.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var editor by remember { mutableStateOf<Goal?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        vm.firstGoalCreated.collect {
            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            snackbar.showSnackbar("We'll ask at 9 pm how it went. Change this in Settings.")
        }
    }
    val active = goals.filter { !it.archived }
    val todays = active.filter { it.activeOn(today) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Goals", style = MaterialTheme.typography.headlineMedium)
                Text("Mark the days you showed up. Your lock screen keeps count.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (todays.isNotEmpty()) {
                item { SectionHeader("Today") }
                items(todays, key = { "today-${it.id}" }) { goal -> TodayRow(goal, today) { vm.toggle(goal.id, today) } }
            }
            item { SectionHeader("All goals") }
            if (active.isEmpty()) {
                item {
                    Text("No goals yet. Add a small habit, a medium goal, or a big dream.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(active, key = { it.id }) { goal -> GoalRow(goal, today) { detailId = goal.id } }
            val archived = goals.filter { it.archived }
            if (archived.isNotEmpty()) {
                item { SectionHeader("Archived") }
                items(archived, key = { "archived-${it.id}" }) { goal ->
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${goal.emoji} ${goal.title}", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vm.restore(goal.copy(archived = false)) }) { Text("Restore") }
                        }
                    }
                }
            }
            item { LockscreenSection(prefs, goals, status, vm) }
        }
        ExtendedFloatingActionButton(
            onClick = { editor = null; editorOpen = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("New goal") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (editorOpen) GoalEditorSheet(editor, onDismiss = { editorOpen = false }) { vm.save(it); editorOpen = false }
    goals.firstOrNull { it.id == detailId }?.let { goal ->
        GoalDetailSheet(
            goal = goal, onDismiss = { detailId = null },
            onToggle = { vm.toggle(goal.id, it) },
            onEdit = { detailId = null; editor = goal; editorOpen = true },
            onArchive = {
                detailId = null
                vm.archive(goal)
                scope.launch {
                    if (snackbar.showSnackbar("Goal archived", actionLabel = "Undo", withDismissAction = true) == SnackbarResult.ActionPerformed) vm.restore(goal)
                }
            },
            onDelete = {
                detailId = null
                vm.delete(goal)
                scope.launch {
                    if (snackbar.showSnackbar("Goal deleted", actionLabel = "Undo", withDismissAction = true) == SnackbarResult.ActionPerformed) vm.restore(goal)
                }
            },
        )
    }
}

@Composable
private fun TodayRow(goal: Goal, today: LocalDate, onToggle: () -> Unit) {
    val done = today in goal.checkIns
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(goal.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierShape(goal.tier, 10.dp); Spacer(Modifier.width(6.dp))
                    Text(goal.tier.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (done) FilledTonalButton(onClick = onToggle) { Icon(Icons.Rounded.Check, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Done") }
            else Button(onClick = onToggle) { Text("Mark done") }
        }
    }
}

@Composable
private fun GoalRow(goal: Goal, today: LocalDate, onClick: () -> Unit) {
    val (done, elapsed) = GoalDays.progress(goal, today)
    val streak = GoalDays.currentStreak(goal, today)
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TierShape(goal.tier, 14.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${goal.emoji} ${goal.title}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (elapsed == 0) "Starts ${goal.startDate}" else "$done of $elapsed days" + if (streak > 0) " · 🔥 $streak in a row" else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
