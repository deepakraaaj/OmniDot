package com.omnidot.script

/** Everything a workflow script can do on the phone. The Android implementation is [AndroidOmniApi]. */
interface OmniApi {
    fun speak(text: String, style: String)
    fun bell()
    fun notify(title: String, text: String)
    fun vibrate()
    fun toast(text: String)
    /** Sets a Clock app alarm [minutes] from now; returns the time it will ring ("HH:mm"). */
    fun alarmSet(minutes: Int, label: String): String
    fun alarmCalm(minutes: Int, label: String)
    fun now(): Map<String, Any>
    fun goalsToday(): List<Map<String, Any>>
    fun checkIn(title: String): Boolean
    fun httpGet(url: String): Map<String, Any>
    fun httpPost(url: String, json: String): Map<String, Any>
    fun battery(): Int
    fun wifi(): Boolean
    fun clipboard(text: String)
    fun open(url: String)
    fun storeGet(key: String): Any?
    fun storeSet(key: String, value: Any?)
    fun run(name: String): Boolean
}
