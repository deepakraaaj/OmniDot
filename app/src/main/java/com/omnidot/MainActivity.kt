package com.omnidot

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.omnidot.automation.AutomationEngine
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.AutomationAction
import com.omnidot.automation.AutomationTrigger
import com.omnidot.core.MatrixPreset
import com.omnidot.ui.PreviewScreen
import com.omnidot.ui.AutomationScreen
import com.omnidot.ui.CustomizeScreen
import com.omnidot.ui.theme.OmniDotTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: OmniDotViewModel by viewModels()

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent)

        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val status by viewModel.applyStatus.collectAsStateWithLifecycle()
            val automations by viewModel.automations.collectAsStateWithLifecycle()
            val testStatus by viewModel.testStatus.collectAsStateWithLifecycle()
            var page by remember { mutableStateOf(0) }

            // React to wallpaper apply status with toast
            LaunchedEffect(status) {
                when (status) {
                    ApplyStatus.Applied -> showToast("✓ Lockscreen wallpaper updated!")
                    is ApplyStatus.Error -> showToast("✕ Error: ${(status as ApplyStatus.Error).message}")
                    else -> Unit
                }
            }

            OmniDotTheme {
                if (page == 0) AutomationScreen(
                    rules = automations,
                    testStatus = testStatus,
                    onBack = { page = 1 },
                    onSave = { rule ->
                        viewModel.saveAutomation(rule)
                        showToast("✓ Workflow saved: ${rule.name}")
                    },
                    onEnabledChanged = { id, enabled ->
                        viewModel.setAutomationEnabled(id, enabled)
                        showToast(if (enabled) "Workflow enabled" else "Workflow paused")
                    },
                    onDelete = { id ->
                        viewModel.deleteAutomation(id)
                        showToast("Workflow deleted")
                    },
                    onTest = { rule ->
                        viewModel.testAutomation(rule) { result ->
                            showToast(result)
                        }
                    },
                    onClearTestStatus = viewModel::clearTestStatus,
                ) else if (page == 2) CustomizeScreen(
                    preferences = preferences,
                    applyStatus = status,
                    onBack = { page = 1 },
                    onBirthDateChanged = { date ->
                        viewModel.setBirthDate(date)
                        showToast("Birth date set: $date")
                    },
                    onAutomaticUpdatesChanged = { enabled ->
                        viewModel.setAutomaticUpdates(enabled)
                        showToast(if (enabled) "Midnight refresh enabled" else "Midnight refresh disabled")
                    },
                    onVerticalOffsetChanged = viewModel::setVerticalOffset,
                    onMatrixScaleChanged = viewModel::setMatrixScale,
                    onAccentColorChanged = { color ->
                        viewModel.setAccentColor(color)
                        showToast("Accent color updated")
                    },
                    onApply = {
                        viewModel.applyWallpaper()
                        showToast("Compiling lockscreen HUD...")
                    },
                ) else PreviewScreen(
                    preferences = preferences,
                    applyStatus = status,
                    onPresetSelected = { preset ->
                        viewModel.selectPreset(preset)
                        showToast("Selected: ${preset.title}")
                    },
                    onOpenCustomize = { page = 2 },
                    onOpenAutomations = { page = 0 },
                    onApply = {
                        viewModel.applyWallpaper()
                        showToast("Compiling lockscreen HUD...")
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "omnidot") {
            lifecycleScope.launch {
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
                val result = AutomationEngine.execute(this@MainActivity, rule)
                showToast("OmniDot Remote: ${result.getOrNull() ?: result.exceptionOrNull()?.message}")
            }
        }
    }
}
