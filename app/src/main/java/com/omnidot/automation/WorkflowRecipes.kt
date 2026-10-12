package com.omnidot.automation

data class Recipe(val title: String, val description: String, val build: () -> AutomationRule)

object WorkflowRecipes {
    val all: List<Recipe> = listOf(
        Recipe("Alarm for a shared link", "When a contact sends a link on WhatsApp, set a Clock alarm 20 minutes later") {
            AutomationRule(name = "", trigger = AutomationTrigger.WHATSAPP_MESSAGE, action = AutomationAction.SET_PHONE_ALARM, onlyLinks = true, alarmInMinutes = 20)
        },
        Recipe("Good morning after your alarm", "When you turn off your alarm: a soft bell, then today's goals in a calm voice") {
            AutomationRule(name = "", trigger = AutomationTrigger.ALARM_DISMISSED, action = AutomationAction.SPEAK_TEXT, targetMessage = "{greeting}")
        },
        Recipe("Calm wake-up", "Weekdays at 6:30: a soft rising bell, then a good-morning with today's goals") {
            AutomationRule(
                name = "", trigger = AutomationTrigger.SCHEDULED_TIME, action = AutomationAction.RING_ALARM, scheduledTime = "06:30",
                days = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY),
            )
        },
        Recipe("Calm charging chime", "A soft bell and a gentle voice when you plug in") {
            AutomationRule(name = "", trigger = AutomationTrigger.POWER_CONNECTED, action = AutomationAction.SPEAK_TEXT, targetMessage = "Charging started")
        },
        Recipe("Battery low reminder", "A notification when battery drops below 20%") {
            AutomationRule(name = "", trigger = AutomationTrigger.BATTERY_LOW, action = AutomationAction.SHOW_NOTIFICATION, message = "Battery is at {battery}%. Time to charge.")
        },
        Recipe("Music time", "A calm hello when headphones connect") {
            AutomationRule(name = "", trigger = AutomationTrigger.HEADSET_PLUG, action = AutomationAction.SPEAK_TEXT, targetMessage = "Enjoy your music")
        },
    ).map { recipe -> recipe.copy(build = { recipe.build().let { it.copy(name = WorkflowText.name(it)) } }) }

    /** Recipes whose trigger and action the user doesn't already have a workflow for. */
    fun suggestions(rules: List<AutomationRule>): List<Recipe> = all.filter { recipe ->
        val r = recipe.build()
        rules.none { it.trigger == r.trigger && it.action == r.action }
    }
}
