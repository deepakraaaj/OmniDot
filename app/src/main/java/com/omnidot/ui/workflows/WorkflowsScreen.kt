package com.omnidot.ui.workflows

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.HealthCheck
import com.omnidot.automation.HealthIssue
import com.omnidot.automation.WorkflowRecipes
import com.omnidot.ui.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkflowsScreen(
    rules: List<AutomationRule>,
    padding: PaddingValues,
    snackbar: SnackbarHostState,
    onOpenEditor: (AutomationRule?) -> Unit,
    onEnabledChanged: (String, Boolean) -> Unit,
    onDelete: (AutomationRule) -> Unit,
    onRestore: (AutomationRule) -> Unit,
    onTest: (AutomationRule) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var probe by remember { mutableStateOf(HealthCheck.probe(context)) }
    LifecycleResumeEffect(Unit) { probe = HealthCheck.probe(context); onPauseOrDispose { } }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // After repeated denials Android returns immediately without a dialog; send the user to settings instead.
        if (!granted || !HealthCheck.probe(context).notificationsGranted) {
            context.startActivity(HealthCheck.fixIntent(context, HealthIssue.NOTIFICATIONS_BLOCKED))
        }
        probe = HealthCheck.probe(context)
    }
    val issues = HealthCheck.issues(probe.notificationsGranted, probe.listenerGranted, probe.backgroundAllowed, rules)

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Workflows", style = MaterialTheme.typography.headlineMedium)
                Text("Small automations that run on your phone.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            issues.firstOrNull()?.let { issue ->
                item {
                    HealthBanner(issue, extra = issues.size - 1) {
                        if (issue == HealthIssue.NOTIFICATIONS_BLOCKED && Build.VERSION.SDK_INT >= 33) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(HealthCheck.fixIntent(context, issue))
                        }
                    }
                }
            }
            if (rules.isEmpty()) {
                item { SectionHeader("Start with a recipe") }
                items(WorkflowRecipes.all, key = { it.title }) { recipe ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { onOpenEditor(recipe.build()) },
                        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                            Text(recipe.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    WorkflowCard(
                        rule = rule, nowMillis = now,
                        onEnabledChanged = { onEnabledChanged(rule.id, it) },
                        onEdit = { onOpenEditor(rule) },
                        onTest = { onTest(rule) },
                        onDelete = {
                            onDelete(rule)
                            scope.launch {
                                val result = snackbar.showSnackbar("Workflow deleted", actionLabel = "Undo", withDismissAction = true)
                                if (result == SnackbarResult.ActionPerformed) onRestore(rule)
                            }
                        },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onOpenEditor(null) },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("New workflow") },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun HealthBanner(issue: HealthIssue, extra: Int, onFix: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(issue.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(issue.detail + if (extra > 0) " (+$extra more in Settings)" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(0.dp))
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}
