package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowRecipesTest {
    @Test
    fun `recipes are saveable and use only available triggers`() {
        assertEquals(3, WorkflowRecipes.all.size)
        WorkflowRecipes.all.forEach { recipe ->
            val rule = recipe.build()
            assertTrue(recipe.title, rule.trigger.available)
            assertTrue(recipe.title, WorkflowText.canSave(rule))
        }
    }
}
