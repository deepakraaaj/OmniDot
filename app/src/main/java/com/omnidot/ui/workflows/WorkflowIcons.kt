package com.omnidot.ui.workflows

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.omnidot.automation.AutomationAction
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.AutomationTrigger

internal fun triggerIcon(trigger: AutomationTrigger): ImageVector = when (trigger) {
    AutomationTrigger.WHATSAPP_MESSAGE -> Icons.Rounded.Forum
    AutomationTrigger.WEATHER_RAIN -> Icons.Rounded.CloudUpload
    AutomationTrigger.SCHEDULED_TIME -> Icons.Rounded.Schedule
    AutomationTrigger.PHONE_ALARM_RINGS -> Icons.Rounded.Alarm
    AutomationTrigger.ALARM_DISMISSED -> Icons.Rounded.WbSunny
    AutomationTrigger.POWER_CONNECTED -> Icons.Rounded.Power
    AutomationTrigger.POWER_DISCONNECTED -> Icons.Rounded.PowerOff
    AutomationTrigger.BATTERY_LOW -> Icons.Rounded.BatteryAlert
    AutomationTrigger.BATTERY_OKAY -> Icons.Rounded.BatteryFull
    AutomationTrigger.DEVICE_BOOT -> Icons.Rounded.RestartAlt
    AutomationTrigger.SCREEN_UNLOCKED -> Icons.Rounded.LockOpen
    AutomationTrigger.DAILY_MIDNIGHT -> Icons.Rounded.Schedule
    AutomationTrigger.WIFI_CONNECTED -> Icons.Rounded.Wifi
    AutomationTrigger.WIFI_DISCONNECTED -> Icons.Rounded.WifiOff
    AutomationTrigger.HEADSET_PLUG -> Icons.Rounded.Headphones
    AutomationTrigger.INBOUND_WEBHOOK -> Icons.Rounded.Language
}

internal fun actionIcon(action: AutomationAction): ImageVector = when (action) {
    AutomationAction.SET_PHONE_ALARM -> Icons.Rounded.AlarmAdd
    AutomationAction.RING_ALARM -> Icons.Rounded.Alarm
    AutomationAction.SPEAK_TEXT -> Icons.Rounded.Info
    AutomationAction.SEND_WHATSAPP -> Icons.Rounded.Forum
    AutomationAction.CALL_PHONE -> Icons.Rounded.Call
    AutomationAction.APPLY_PRESET -> Icons.Rounded.Wallpaper
    AutomationAction.REFRESH_CURRENT -> Icons.Rounded.Refresh
    AutomationAction.CYCLE_PRESET -> Icons.Rounded.Sync
    AutomationAction.CYCLE_ACCENT -> Icons.Rounded.Palette
    AutomationAction.HTTP_REQUEST -> Icons.Rounded.Language
    AutomationAction.HTTP_POST -> Icons.Rounded.CloudUpload
    AutomationAction.DISCORD_WEBHOOK -> Icons.Rounded.Forum
    AutomationAction.SLACK_WEBHOOK -> Icons.AutoMirrored.Rounded.Send
    AutomationAction.SHOW_NOTIFICATION -> Icons.Rounded.NotificationsActive
    AutomationAction.VIBRATE -> Icons.Rounded.Vibration
    AutomationAction.COPY_STATS -> Icons.Rounded.ContentCopy
    AutomationAction.OPEN_URL -> Icons.Rounded.OpenInBrowser
}
