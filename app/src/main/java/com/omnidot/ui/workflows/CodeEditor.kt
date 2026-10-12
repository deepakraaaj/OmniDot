package com.omnidot.ui.workflows

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidot.script.AndroidOmniApi
import com.omnidot.script.ScriptResult
import com.omnidot.script.ScriptRunner
import com.omnidot.script.ScriptTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Symbols = listOf("{", "}", "(", ")", "[", "]", ";", "\"", "'", "=", "=>", ".", "+", "!", "&&", "||")

/** A phone-friendly JavaScript editor with line numbers, a symbol row, templates and a test run. */
@Composable
fun CodeEditor(code: String, ruleId: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf(TextFieldValue(code)) }
    var templatesOpen by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ScriptResult?>(null) }
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)

    fun update(v: TextFieldValue) {
        value = v
        if (v.text != code) onChange(v.text)
    }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Your code", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { templatesOpen = true }) { Text("Templates") }
                DropdownMenu(expanded = templatesOpen, onDismissRequest = { templatesOpen = false }) {
                    ScriptTemplates.all.forEach { (name, template) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            templatesOpen = false
                            update(TextFieldValue(template, TextRange(template.length)))
                        })
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 220.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp),
        ) {
            val lines = value.text.count { it == '\n' } + 1
            Text(
                (1..lines).joinToString("\n"),
                style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f), textAlign = TextAlign.End),
                modifier = Modifier.width(28.dp),
            )
            Box(Modifier.padding(start = 10.dp).weight(1f).horizontalScroll(rememberScrollState())) {
                BasicTextField(
                    value = value,
                    onValueChange = ::update,
                    textStyle = style,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.width(900.dp),
                    decorationBox = { inner ->
                        if (value.text.isEmpty()) Text("// event.sender, event.message, event.link…\nomni.speak(\"Hello\");", style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        inner()
                    },
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbols.forEach { symbol ->
                AssistChip(onClick = {
                    val start = value.selection.start
                    val end = value.selection.end
                    val text = value.text.replaceRange(start, end, symbol)
                    update(TextFieldValue(text, TextRange(start + symbol.length)))
                }, label = { Text(symbol, fontFamily = FontFamily.Monospace) })
            }
        }
        OutlinedButton(
            onClick = {
                running = true
                scope.launch {
                    val sample = mapOf(
                        "trigger" to "TEST", "sender" to "Pradeep",
                        "message" to "Join https://meet.google.com/abc-defg-hij", "link" to "https://meet.google.com/abc-defg-hij",
                        "time" to java.time.LocalTime.now().withNano(0).toString(),
                    )
                    result = withContext(Dispatchers.IO) { ScriptRunner().run(value.text, sample, AndroidOmniApi(context, ruleId)) }
                    running = false
                }
            },
            enabled = !running && value.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Text(if (running) "Running…" else "Run with sample event", modifier = Modifier.padding(start = 6.dp))
        }
        result?.let { r ->
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(if (r.ok) "Log" else "Log · failed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                r.logs.takeLast(50).forEach { Text(it, style = style) }
                if (r.ok && r.logs.isEmpty()) Text("Script finished", style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                r.error?.let { Text(it, style = style.copy(color = MaterialTheme.colorScheme.error)) }
            }
        }
        Text(
            "Sample event: sender \"Pradeep\" with a meeting link. Use event.sender, event.message, event.link, event.trigger and omni.* (speak, bell, notify, alarm, goals, http, store, run…).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
