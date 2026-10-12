package com.omnidot.script

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omnidot.alarm.AlarmRingService
import com.omnidot.alarm.AlarmScheduler
import com.omnidot.automation.AutomationEngine
import com.omnidot.automation.AutomationRepository
import com.omnidot.automation.PhoneAlarms
import com.omnidot.automation.SoundSynthesizer
import com.omnidot.automation.VoiceFeedbackManager
import com.omnidot.automation.VoiceStyle
import com.omnidot.goals.GoalDays
import com.omnidot.goals.GoalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

private val Context.scriptStore by preferencesDataStore(name = "omnidot_script_store")

/** The `omni` API backed by the phone. Calls run on the script's background thread. */
class AndroidOmniApi(private val context: Context, private val ruleId: String, private val depth: Int = 0) : OmniApi {
    private val storeKey = stringPreferencesKey("store_json")

    override fun speak(text: String, style: String) {
        val voice = VoiceStyle.entries.firstOrNull { it.name.equals(style, true) } ?: VoiceStyle.CALM
        runBlocking { VoiceFeedbackManager.speak(context, text, voice.pitch, voice.rate, voice.volume, waitForCompletion = true) }
    }

    override fun bell() = SoundSynthesizer.playSoftBell()
    override fun notify(title: String, text: String) = AutomationEngine.showNotification(context, title, text)
    override fun vibrate() = AutomationEngine.triggerVibration(context)
    override fun toast(text: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    }

    override fun alarmSet(minutes: Int, label: String): String = PhoneAlarms.set(context, ruleId, minutes.coerceIn(1, 1440), label).first
    override fun alarmCalm(minutes: Int, label: String) =
        AlarmScheduler.snooze(context, AlarmRingService.Ring(ruleId, label, 10, true), ZonedDateTime.now().plusMinutes(minutes.coerceIn(1, 1440).toLong()))

    override fun now(): Map<String, Any> {
        val t = LocalTime.now()
        val d = LocalDate.now()
        return mapOf("hour" to t.hour, "minute" to t.minute, "weekday" to d.dayOfWeek.name, "date" to d.toString())
    }

    override fun goalsToday(): List<Map<String, Any>> = runBlocking {
        val today = LocalDate.now()
        GoalRepository(context).current().filter { it.activeOn(today) }.map {
            mapOf("title" to it.title, "tier" to it.tier.name, "done" to (today in it.checkIns), "streak" to GoalDays.currentStreak(it, today))
        }
    }

    override fun checkIn(title: String): Boolean = runBlocking {
        val repo = GoalRepository(context)
        val goal = repo.current().firstOrNull { !it.archived && it.title.equals(title.trim(), true) } ?: return@runBlocking false
        repo.setCheckIn(goal.id, LocalDate.now(), done = true)
        true
    }

    override fun httpGet(url: String): Map<String, Any> = http(url, "GET", null)
    override fun httpPost(url: String, json: String): Map<String, Any> = http(url, "POST", json)

    private fun http(url: String, method: String, body: String?): Map<String, Any> {
        require(url.startsWith("http://") || url.startsWith("https://")) { "URL must start with http:// or https://" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "OmniDot-Android/1.0")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(body) }
            }
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText().take(64_000) }.orEmpty()
            mapOf("status" to status, "body" to text)
        } finally {
            connection.disconnect()
        }
    }

    override fun battery(): Int = AutomationEngine.getBatteryLevel(context)

    override fun wifi(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    override fun clipboard(text: String) {
        Handler(Looper.getMainLooper()).post {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("OmniDot", text))
        }
    }

    override fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun storeGet(key: String): Any? = runBlocking {
        val raw = context.scriptStore.data.first()[storeKey] ?: return@runBlocking null
        JSONObject(raw).opt(key)?.takeUnless { it == JSONObject.NULL }
    }

    override fun storeSet(key: String, value: Any?) {
        runBlocking {
            context.scriptStore.edit { prefs ->
                val obj = prefs[storeKey]?.let { JSONObject(it) } ?: JSONObject()
                if (value == null) obj.remove(key) else obj.put(key, value)
                prefs[storeKey] = obj.toString()
            }
        }
    }

    override fun run(name: String): Boolean = runBlocking {
        if (depth >= 3) return@runBlocking false // stop workflows calling each other forever
        val repo = AutomationRepository(context)
        val rule = repo.current().firstOrNull { it.enabled && it.name.contains(name.trim(), ignoreCase = true) } ?: return@runBlocking false
        val result = AutomationEngine.execute(context, rule, depth = depth + 1)
        repo.recordRun(rule.id, result.getOrElse { "Error: ${it.message}" })
        result.isSuccess
    }
}
