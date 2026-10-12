package com.omnidot.script

object ScriptTemplates {
    val all: List<Pair<String, String>> = listOf(
        "Alarm after a link" to """
            // When: WhatsApp message. Sets a Clock alarm 20 minutes after Pradeep sends a link.
            if (event.link && event.sender.includes("Pradeep")) {
              const at = omni.alarm.set(20, "Link from " + event.sender);
              omni.notify("Alarm set for " + at, event.link);
              log("Alarm at", at);
            }
        """.trimIndent(),
        "Weekdays only" to """
            const now = omni.time.now();
            const weekend = now.weekday === "SATURDAY" || now.weekday === "SUNDAY";
            if (!weekend) {
              omni.speak("Time to focus. You've got this.", { style: "calm" });
            }
        """.trimIndent(),
        "Post to n8n" to """
            // Replace with your n8n webhook URL.
            const res = omni.http.post("https://your-n8n.example.com/webhook/omnidot", {
              trigger: event.trigger,
              sender: event.sender,
              message: event.message,
              battery: omni.battery(),
            });
            log("n8n replied", res.status);
        """.trimIndent(),
        "Count messages per day" to """
            const today = omni.time.now().date;
            const key = "count-" + today;
            const n = (omni.store.get(key) || 0) + 1;
            omni.store.set(key, n);
            if (n === 10) omni.notify("Busy day", "10 messages today from " + event.sender);
            log("Messages today:", n);
        """.trimIndent(),
        "Morning summary" to """
            const goals = omni.goals.today();
            const open = goals.filter(g => !g.done).map(g => g.title);
            const text = open.length === 0
              ? "Good morning. Everything is done already."
              : "Good morning. Today: " + open.join(", ") + ".";
            omni.bell();
            omni.speak(text, { style: "calm" });
        """.trimIndent(),
    )
}
