package com.omnidot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.omnidot.automation.AutomationAction
import com.omnidot.automation.AutomationEngine
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.AutomationTrigger
import com.omnidot.automation.RemoteTriggerGuard
import com.omnidot.core.MatrixPreset
import com.omnidot.data.PreferencesManager
import com.omnidot.ui.AppTab
import com.omnidot.ui.OmniDotApp
import com.omnidot.ui.goals.GoalsScreen
import com.omnidot.ui.settings.SettingsScreen
import com.omnidot.ui.workflows.WorkflowEditor
import com.omnidot.ui.workflows.WorkflowsScreen
import com.omnidot.ui.theme.OmniDotTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
    }

    private val viewModel: OmniDotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent)
        val initialTab = if (intent?.getStringExtra(EXTRA_OPEN_TAB) == "goals") AppTab.GOALS else AppTab.WORKFLOWS
        setContent {
            OmniDotTheme {
                OmniDotApp(
                    initialTab = initialTab,
                    workflows = { padding, snackbar, setHideBar ->
                        val automations by viewModel.automations.collectAsStateWithLifecycle()
                        var editing by remember { mutableStateOf<AutomationRule?>(null) }
                        var editorOpen by remember { mutableStateOf(false) }
                        LaunchedEffect(editorOpen) { setHideBar(editorOpen) }
                        val scope = rememberCoroutineScope()
                        if (editorOpen) {
                            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
                            WorkflowEditor(
                                initial = editing,
                                defaultVoice = prefs.voiceStyle,
                                onClose = { editorOpen = false },
                                onSave = {
                                    viewModel.saveAutomation(it)
                                    editorOpen = false
                                    scope.launch { snackbar.showSnackbar("Workflow saved") }
                                },
                                onTest = { rule -> viewModel.testAutomation(rule) { msg -> scope.launch { snackbar.showSnackbar(msg) } } },
                            )
                        } else {
                            WorkflowsScreen(
                                rules = automations,
                                padding = padding,
                                snackbar = snackbar,
                                onOpenEditor = { editing = it; editorOpen = true },
                                onEnabledChanged = { id, enabled -> viewModel.setAutomationEnabled(id, enabled) },
                                onDelete = { viewModel.deleteAutomation(it.id) },
                                onRestore = { viewModel.restoreAutomation(it) },
                                onTest = { rule -> viewModel.testAutomation(rule) { msg -> scope.launch { snackbar.showSnackbar(msg) } } },
                            )
                        }
                    },
                    goals = { padding, snackbar -> GoalsScreen(padding, snackbar) },
                    settings = { padding, snackbar -> SettingsScreen(padding, snackbar, viewModel) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
        if (intent.hasExtra(EXTRA_OPEN_TAB)) {
            setIntent(intent)
            recreate()
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "omnidot") {
            lifecycleScope.launch {
                val expected = PreferencesManager(this@MainActivity).remoteKey()
                if (!RemoteTriggerGuard.isAuthorized(uri.getQueryParameter("key"), expected)) return@launch
                val actionParam = uri.getQueryParameter("action") ?: uri.host
                val presetParam = uri.getQueryParameter("preset")
                val rule = AutomationRule(
                    name = "Remote Deep Link",
                    trigger = AutomationTrigger.INBOUND_WEBHOOK,
                    action = when (actionParam) {
                        "refresh" -> AutomationAction.REFRESH_CURRENT
                        "cycle" -> AutomationAction.CYCLE_PRESET
                        "preset" -> AutomationAction.APPLY_PRESET
                        "vibrate" -> AutomationAction.VIBRATE
                        else -> AutomationAction.REFRESH_CURRENT
                    },
                    preset = MatrixPreset.fromStored(presetParam?.uppercase()),
                )
                AutomationEngine.execute(this@MainActivity, rule)
            }
        }
    }
}
