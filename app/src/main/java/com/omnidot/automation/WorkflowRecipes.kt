package com.omnidot.automation

data class Recipe(val title: String, val description: String, val build: () -> AutomationRule)

object WorkflowRecipes {
    val all: List<Recipe> = listOf(
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
