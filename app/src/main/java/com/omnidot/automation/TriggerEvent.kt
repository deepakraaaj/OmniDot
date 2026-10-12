package com.omnidot.automation

/** Details of what fired a workflow, available to its action as {sender}, {message} and {link}. */
data class TriggerEvent(val sender: String = "", val message: String = "", val link: String = "")
