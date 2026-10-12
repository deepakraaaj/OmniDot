package com.omnidot.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.omnidot.data.PreferencesManager
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
                if (intent?.action == ACTION_REMOTE_TRIGGER &&
                    !RemoteTriggerGuard.isAuthorized(intent.getStringExtra("key"), PreferencesManager(context).remoteKey())
                ) return@launch
                val repository = AutomationRepository(context)
                val rules = repository.current().filter { it.enabled && it.trigger == trigger && it.trigger.available }
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
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> AutomationTrigger.DEVICE_BOOT
            ACTION_REMOTE_TRIGGER -> {
                val triggerExtra = intent.getStringExtra("trigger")
                if (!triggerExtra.isNullOrBlank()) AutomationTrigger.fromStored(triggerExtra) else AutomationTrigger.INBOUND_WEBHOOK
            }
            else -> {
                val wifiNow = action == CONNECTIVITY_CHANGE && isWifiConnected(context)
                val trigger = DeviceEvents.resolve(
                    action = action,
                    initialSticky = isInitialStickyBroadcast,
                    headsetState = intent.getIntExtra("state", -1),
                    wifiNow = wifiNow,
                    lastWifi = lastWifi,
                )
                if (action == CONNECTIVITY_CHANGE) lastWifi = wifiNow
                trigger
            }
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

    companion object {
        const val ACTION_REMOTE_TRIGGER = "com.omnidot.TRIGGER_ACTION"
        private const val CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"

        /** Last observed Wi-Fi state in this process; the sticky replay at registration seeds it. */
        @Volatile
        private var lastWifi: Boolean? = null

        fun deviceEventFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_HEADSET_PLUG)
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
    }
}
