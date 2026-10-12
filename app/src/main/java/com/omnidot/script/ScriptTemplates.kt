package com.omnidot.script

object ScriptTemplates {
    /** Starter script shown when you pick "Run code": every event field and omni call, ready to edit. */
    val boilerplate: String = """
        // ─── OmniDot script ─────────────────────────────────────────────
        // Runs when this workflow's "When" fires. Edit freely, then tap
        // "Run with sample event" to try it. log(...) prints below.
        //
        // event (what fired this workflow):
        //   event.trigger  e.g. "WHATSAPP_MESSAGE", "POWER_CONNECTED"
        //   event.sender   who sent it (WhatsApp)       event.message  the text
        //   event.link     first link in the message    event.time     "HH:MM:SS"
        //
        // omni (what your phone can do):
        //   omni.speak(text, { style: "calm" | "neutral" | "cheerful" })
        //   omni.bell()  omni.notify(title, text)  omni.vibrate()  omni.toast(text)
        //   omni.alarm.set(minutes, label)   → Clock app alarm, returns "HH:MM"
        //   omni.alarm.calm(minutes, label)  → OmniDot calm alarm
        //   omni.time.now()      → { hour, minute, weekday, date }
        //   omni.goals.today()   → [{ title, tier, done, streak }]
        //   omni.goals.checkIn(title)
        //   omni.http.get(url) / omni.http.post(url, object) → { status, body }
        //   omni.battery()  omni.wifi()  omni.clipboard(text)  omni.open(url)
        //   omni.store.get(key) / omni.store.set(key, value)  (remembered)
        //   omni.run(workflowName)
        // ────────────────────────────────────────────────────────────────

        // 1. Settings: change these to customise.
        const SETTINGS = {
          onlyFrom: "",          // e.g. "Pradeep" (empty = anyone)
          needsLink: false,      // true = only messages with a link
          alarmInMinutes: 0,     // e.g. 20 to set a Clock alarm (0 = off)
          say: "",               // e.g. "New link from {sender}" (empty = silent)
          notify: true,          // show a notification
        };

        // 2. Should this run? Return early to skip.
        if (SETTINGS.onlyFrom && !event.sender.includes(SETTINGS.onlyFrom)) {
          log("Skipped: not from", SETTINGS.onlyFrom);
        } else if (SETTINGS.needsLink && !event.link) {
          log("Skipped: no link");
        } else {
          // 3. Do things.
          const fill = (text) => text.replace("{sender}", event.sender || "someone").replace("{link}", event.link || "");

          if (SETTINGS.alarmInMinutes > 0) {
            const at = omni.alarm.set(SETTINGS.alarmInMinutes, fill("Link from {sender}"));
            log("Alarm set for", at);
          }
          if (SETTINGS.say) omni.speak(fill(SETTINGS.say), { style: "calm" });
          if (SETTINGS.notify) omni.notify("OmniDot", fill(event.message || "Workflow ran"));

          // 4. Your own logic goes here.
          log("Done for", event.trigger);
        }
    """.trimIndent()

    val all: List<Pair<String, String>> = listOf(
        "Starter (everything explained)" to boilerplate,
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
