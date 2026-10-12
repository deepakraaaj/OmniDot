package com.omnidot.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptRunnerTest {
    private class FakeApi : OmniApi {
        val calls = mutableListOf<String>()
        val store = mutableMapOf<String, Any?>()
        override fun speak(text: String, style: String) { calls += "speak:$style:$text" }
        override fun bell() { calls += "bell" }
        override fun notify(title: String, text: String) { calls += "notify:$title:$text" }
        override fun vibrate() { calls += "vibrate" }
        override fun toast(text: String) { calls += "toast:$text" }
        override fun alarmSet(minutes: Int, label: String): String { calls += "alarm:$minutes:$label"; return "22:50" }
        override fun alarmCalm(minutes: Int, label: String) { calls += "calm:$minutes:$label" }
        override fun now(): Map<String, Any> = mapOf("hour" to 22, "minute" to 30, "weekday" to "MONDAY", "date" to "2026-10-12")
        override fun goalsToday(): List<Map<String, Any>> = listOf(mapOf("title" to "Run", "tier" to "BIG", "done" to false, "streak" to 5))
        override fun checkIn(title: String): Boolean { calls += "checkIn:$title"; return true }
        override fun httpGet(url: String): Map<String, Any> = mapOf("status" to 200, "body" to "ok")
        override fun httpPost(url: String, json: String): Map<String, Any> { calls += "post:$url:$json"; return mapOf("status" to 201, "body" to "") }
        override fun battery(): Int = 77
        override fun wifi(): Boolean = true
        override fun clipboard(text: String) { calls += "clip:$text" }
        override fun open(url: String) { calls += "open:$url" }
        override fun storeGet(key: String): Any? = store[key]
        override fun storeSet(key: String, value: Any?) { store[key] = value }
        override fun run(name: String): Boolean { calls += "run:$name"; return true }
    }

    private val event = mapOf("trigger" to "WHATSAPP_MESSAGE", "sender" to "Pradeep", "message" to "Join https://meet.x/abc", "link" to "https://meet.x/abc", "time" to "22:30")

    @Test
    fun `scripts see the event and call omni`() {
        val api = FakeApi()
        val result = ScriptRunner().run(
            """
            if (event.link && event.sender.includes("Pradeep")) {
              const at = omni.alarm.set(20, "Link from " + event.sender);
              log("alarm at", at);
            }
            omni.speak("hi", { style: "calm" });
            """.trimIndent(),
            event, api,
        )
        assertTrue(result.error, result.ok)
        assertEquals(listOf("alarm:20:Link from Pradeep", "speak:calm:hi"), api.calls)
        assertEquals(listOf("alarm at 22:50"), result.logs)
    }

    @Test
    fun `values from omni come back as plain javascript`() {
        val api = FakeApi()
        val result = ScriptRunner().run(
            """
            const g = omni.goals.today()[0];
            log(g.title, g.streak + 1, omni.time.now().weekday, omni.battery(), omni.wifi());
            omni.http.post("https://n8n/x", { goal: g.title, n: 2 });
            """.trimIndent(),
            event, api,
        )
        assertTrue(result.error, result.ok)
        assertEquals(listOf("Run 6 MONDAY 77 true"), result.logs)
        assertTrue(api.calls.single { it.startsWith("post:") }.contains("\"goal\":\"Run\""))
    }

    @Test
    fun `store remembers values between runs`() {
        val api = FakeApi()
        val code = "const n = (omni.store.get('count') || 0) + 1; omni.store.set('count', n); log(n);"
        ScriptRunner().run(code, event, api)
        assertEquals(listOf("2"), ScriptRunner().run(code, event, api).logs)
    }

    @Test
    fun `errors report the line number`() {
        val result = ScriptRunner().run("log('a');\nnotAFunction();", event, FakeApi())
        assertFalse(result.ok)
        assertTrue(result.error, result.error!!.startsWith("Line 2:"))
        assertEquals(listOf("a"), result.logs)
    }

    @Test
    fun `endless loops are stopped`() {
        val result = ScriptRunner(timeoutMs = 300).run("while (true) {}", event, FakeApi())
        assertFalse(result.ok)
        assertEquals("Script took too long (over 0.3 s)", result.error)
    }

    @Test
    fun `scripts cannot reach java`() {
        val result = ScriptRunner().run("java.lang.System.exit(0)", event, FakeApi())
        assertFalse(result.ok)
    }

    @Test
    fun `every template compiles`() {
        ScriptTemplates.all.forEach { (name, code) ->
            assertTrue(name, ScriptRunner().compileError(code) == null)
        }
    }
}
