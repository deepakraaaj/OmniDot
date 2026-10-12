package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowRecipesTest {
    @Test
    fun `recipes are saveable and use only available triggers`() {
        assertEquals(6, WorkflowRecipes.all.size)
        assertTrue(WorkflowRecipes.all.any { it.build().action == AutomationAction.SET_PHONE_ALARM && it.build().onlyLinks })
        assertTrue(WorkflowRecipes.all.any { it.build().trigger == AutomationTrigger.ALARM_DISMISSED })
        assertTrue(WorkflowRecipes.all.any { it.build().action == AutomationAction.RING_ALARM })
        WorkflowRecipes.all.forEach { recipe ->
            val rule = recipe.build()
            assertTrue(recipe.title, rule.trigger.available)
            assertTrue(recipe.title, WorkflowText.canSave(rule))
        }
    }

    @Test
    fun `suggestions hide recipes the user already has`() {
        val charging = WorkflowRecipes.all.first().build()
        val remaining = WorkflowRecipes.suggestions(listOf(charging.copy(id = "mine", targetMessage = "Plugged in")))
        assertEquals(WorkflowRecipes.all.size - 1, remaining.size)
        assertEquals(WorkflowRecipes.all, WorkflowRecipes.suggestions(emptyList()))
    }
}
