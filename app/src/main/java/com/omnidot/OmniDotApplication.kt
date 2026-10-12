package com.omnidot

import android.app.Application
import androidx.core.content.ContextCompat
import com.omnidot.automation.AutomationReceiver

class OmniDotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Android 8+ no longer delivers these implicit broadcasts to manifest receivers;
        // a runtime registration receives them for as long as the app process is alive.
        ContextCompat.registerReceiver(
            this,
            AutomationReceiver(),
            AutomationReceiver.deviceEventFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
