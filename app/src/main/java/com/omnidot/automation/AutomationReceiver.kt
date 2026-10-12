package com.omnidot.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutomationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val trigger = resolveTrigger(context, intent) ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = AutomationRepository(context)
                var rules = repository.current().filter { it.enabled && it.trigger == trigger }
                if (rules.isEmpty() && (trigger == AutomationTrigger.POWER_CONNECTED || trigger == AutomationTrigger.POWER_DISCONNECTED)) {
                    repository.ensureDefaultRules()
                    rules = repository.current().filter { it.enabled && it.trigger == trigger }
                }
                for (rule in rules) {
                    val result = AutomationEngine.execute(context, rule)
                    val statusText = if (result.isSuccess) {
                        result.getOrNull() ?: "Success"
                    } else {
                        "Error: ${result.exceptionOrNull()?.message}"
                    }
                    repository.recordRun(rule.id, statusText)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun resolveTrigger(context: Context, intent: Intent?): AutomationTrigger? {
        val action = intent?.action ?: return null
        return when (action) {
            Intent.ACTION_POWER_CONNECTED -> AutomationTrigger.POWER_CONNECTED
            Intent.ACTION_POWER_DISCONNECTED -> AutomationTrigger.POWER_DISCONNECTED
            Intent.ACTION_BATTERY_LOW -> AutomationTrigger.BATTERY_LOW
            Intent.ACTION_BATTERY_OKAY -> AutomationTrigger.BATTERY_OKAY
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> AutomationTrigger.DEVICE_BOOT
            Intent.ACTION_USER_PRESENT -> AutomationTrigger.SCREEN_UNLOCKED
            Intent.ACTION_HEADSET_PLUG -> AutomationTrigger.HEADSET_PLUG
            "android.net.conn.CONNECTIVITY_CHANGE" -> {
                if (isWifiConnected(context)) AutomationTrigger.WIFI_CONNECTED
                else AutomationTrigger.WIFI_DISCONNECTED
            }
            "com.omnidot.TRIGGER_ACTION" -> {
                val triggerExtra = intent.getStringExtra("trigger")
                if (!triggerExtra.isNullOrBlank()) {
                    AutomationTrigger.fromStored(triggerExtra)
                } else {
                    AutomationTrigger.INBOUND_WEBHOOK
                }
            }
            else -> null
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            networkInfo?.isConnected == true
        }
    }
}
