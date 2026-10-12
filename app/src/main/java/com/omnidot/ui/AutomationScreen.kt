package com.omnidot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omnidot.automation.AutomationAction
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.AutomationTrigger
import com.omnidot.automation.OmniNotificationListenerService
import com.omnidot.core.MatrixPreset
import com.omnidot.ui.theme.DotInactive
import com.omnidot.ui.theme.MutedText
import com.omnidot.ui.theme.OledBlack
import com.omnidot.ui.theme.OmniOrange
import com.omnidot.ui.theme.SurfaceDark

@Composable
fun AutomationScreen(
    rules: List<AutomationRule>,
    testStatus: String?,
    onBack: () -> Unit,
    onSave: (AutomationRule) -> Unit,
    onEnabledChanged: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onTest: (AutomationRule) -> Unit,
    onClearTestStatus: () -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AutomationRule?>(null) }
    var showGuideModal by remember { mutableStateOf(false) }

    Scaffold(containerColor = OledBlack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DotMatrixBrandIcon()
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("OMNIDOT", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
                    Text("Automations", color = MutedText, fontSize = 12.sp)
                }
                IconButton(onClick = { showGuideModal = true }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = "Guide",
                        tint = OmniOrange,
                    )
                }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(onClick = onBack) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = "Wallpaper preview")
                }
            }

            // Live Test Status Banner
            AnimatedVisibility(visible = testStatus != null) {
                testStatus?.let { msg ->
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (msg.startsWith("✕")) Color(0xFF2B0A0A) else Color(0xFF0F2615),
                                RoundedCornerShape(12.dp),
                            )
                            .border(
                                1.dp,
                                if (msg.startsWith("✕")) Color(0xFFE53935) else Color(0xFF2E7D32),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = msg,
                            color = if (msg.startsWith("✕")) Color(0xFFFF8A80) else Color(0xFFA5D6A7),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium,
                        )
                        IconButton(onClick = onClearTestStatus, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MutedText, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (isEditing) {
                // Workflow Editor (Create or Edit)
                androidx.compose.runtime.key(editingRule?.id) {
                    WorkflowEditor(
                        initialRule = editingRule,
                        onCancel = {
                            isEditing = false
                            editingRule = null
                        },
                        onSave = { rule ->
                            onSave(rule)
                            isEditing = false
                            editingRule = null
                        },
                        onTestImmediate = onTest,
                    )
                }
            } else {
                // Workflows List View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ACTIVE WORKFLOWS",
                        color = OmniOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${rules.size} CONFIGURED",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (rules.isEmpty()) {
                    EmptyWorkflowsCard(onCreateClick = {
                        editingRule = null
                        isEditing = true
                    })
                } else {
                    rules.forEach { rule ->
                        WorkflowCard(
                            rule = rule,
                            onEnabledChanged = onEnabledChanged,
                            onEdit = {
                                editingRule = rule
                                isEditing = true
                            },
                            onDelete = onDelete,
                            onTest = onTest,
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            editingRule = null
                            isEditing = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OmniOrange),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("NEW WORKFLOW", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showGuideModal) {
        GuideDialog(onDismiss = { showGuideModal = false })
    }
}

@Composable
private fun WorkflowCard(
    rule: AutomationRule,
    onEnabledChanged: (String, Boolean) -> Unit,
    onEdit: (AutomationRule) -> Unit,
    onDelete: (String) -> Unit,
    onTest: (AutomationRule) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DotInactive, RoundedCornerShape(16.dp)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "${rule.trigger.category.label} • ${rule.action.category.label}",
                        color = MutedText,
                        fontSize = 11.sp,
                    )
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onEnabledChanged(rule.id, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OmniOrange,
                        uncheckedTrackColor = DotInactive,
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Minimalist visual flow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OledBlack, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(triggerIcon(rule.trigger), null, tint = OmniOrange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    rule.trigger.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text("➔", color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                Icon(actionIcon(rule.action), null, tint = OmniOrange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (rule.action == AutomationAction.APPLY_PRESET) rule.preset.title else rule.action.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }

            if (rule.lastRunStatus.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Last: ${rule.lastRunStatus}", color = MutedText, fontSize = 10.sp, maxLines = 1)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onEdit(rule) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("EDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onTest(rule) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = OmniOrange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onDelete(rule.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("DELETE", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun WorkflowEditor(
    initialRule: AutomationRule?,
    onCancel: () -> Unit,
    onSave: (AutomationRule) -> Unit,
    onTestImmediate: (AutomationRule) -> Unit,
) {
    val isEditMode = initialRule != null

    var trigger by remember(initialRule?.id) { mutableStateOf(initialRule?.trigger ?: AutomationTrigger.POWER_CONNECTED) }
    var action by remember(initialRule?.id) { mutableStateOf(initialRule?.action ?: AutomationAction.SHOW_NOTIFICATION) }
    var preset by remember(initialRule?.id) { mutableStateOf(initialRule?.preset ?: MatrixPreset.YEAR_PROGRESS) }
    var url by remember(initialRule?.id) { mutableStateOf(initialRule?.url ?: "") }
    var message by remember(initialRule?.id) { mutableStateOf(initialRule?.message ?: "Progress: {year_percent}% complete") }
    var customPayload by remember(initialRule?.id) { mutableStateOf(initialRule?.customPayload ?: "") }
    var contactName by remember(initialRule?.id) { mutableStateOf(initialRule?.contactName ?: "Pradeep") }
    var contactPhone by remember(initialRule?.id) { mutableStateOf(initialRule?.contactPhone ?: "") }
    var targetMessage by remember(initialRule?.id) { mutableStateOf(initialRule?.targetMessage ?: "Hi, I'm Deepak") }
    var scheduledTime by remember(initialRule?.id) { mutableStateOf(initialRule?.scheduledTime ?: "07:00") }
    var weatherCondition by remember(initialRule?.id) { mutableStateOf(initialRule?.weatherCondition ?: "Rain") }
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OmniOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isEditMode) "Edit workflow" else "Build a workflow",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                    )
                    Text("Choose what happens, then what OmniDot does.", color = MutedText, fontSize = 11.sp)
                }
                TagChip(if (isEditMode) "EDITING" else "NEW")
            }

            val availableActions = AutomationAction.workflowOptions +
                listOfNotNull(initialRule?.action?.takeIf { it !in AutomationAction.workflowOptions })

            Text("START FROM AN EXAMPLE", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        trigger = AutomationTrigger.WHATSAPP_MESSAGE
                        action = AutomationAction.CALL_PHONE
                        contactName = "Pradeep"
                        targetMessage = "Hi, I'm Deepak"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Message → call", fontSize = 11.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        trigger = AutomationTrigger.WEATHER_RAIN
                        action = AutomationAction.SEND_WHATSAPP
                        contactName = "Pradeep"
                        targetMessage = "Bring an umbrella"
                        scheduledTime = "07:00"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Rain → WhatsApp", fontSize = 11.sp, maxLines = 1)
                }
            }

            Spacer(Modifier.height(18.dp))
            WorkflowSectionTitle("1", "WHEN SHOULD IT RUN?")
            Spacer(Modifier.height(8.dp))
            WorkflowChoicePicker(
                selected = trigger,
                options = AutomationTrigger.entries,
                title = { it.label },
                detail = { "${it.category.label} · ${it.subtitle}" },
                icon = { triggerIcon(it) },
                onSelect = { trigger = it },
            )

            if (trigger == AutomationTrigger.WHATSAPP_MESSAGE) {
                Spacer(Modifier.height(12.dp))
                WorkflowTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = "Message from",
                    placeholder = "Contact name, e.g. Pradeep",
                )
                Spacer(Modifier.height(6.dp))
                if (OmniNotificationListenerService.isNotificationAccessGranted(context)) {
                    WorkflowNotice("WhatsApp notification access is enabled.")
                } else {
                    WorkflowNotice("Allow notification access so OmniDot can recognize incoming WhatsApp messages.")
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { OmniNotificationListenerService.openNotificationAccessSettings(context) },
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("ENABLE NOTIFICATION ACCESS") }
                }
            }

            if (trigger == AutomationTrigger.SCHEDULED_TIME || trigger == AutomationTrigger.WEATHER_RAIN) {
                Spacer(Modifier.height(12.dp))
                WorkflowTextField(
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    label = if (trigger == AutomationTrigger.WEATHER_RAIN) "Check at (24-hour time)" else "Run every day at",
                    placeholder = "07:00",
                )
                if (!scheduledTime.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d"))) {
                    Spacer(Modifier.height(6.dp))
                    WorkflowNotice("Enter a valid 24-hour time, for example 07:00.")
                }
            }

            if (trigger == AutomationTrigger.WEATHER_RAIN) {
                Spacer(Modifier.height(8.dp))
                WorkflowTextField(
                    value = weatherCondition,
                    onValueChange = { weatherCondition = it },
                    label = "Weather condition",
                    placeholder = "Rain",
                )
                Spacer(Modifier.height(6.dp))
                WorkflowNotice("Weather checks are not connected yet. A forecast provider is required; this workflow will not run until one is added.")
            } else if (trigger == AutomationTrigger.SCHEDULED_TIME) {
                Spacer(Modifier.height(6.dp))
                WorkflowNotice("Daily time triggers are not connected yet. Saving this workflow will not schedule it.")
            }

            Spacer(Modifier.height(20.dp))
            WorkflowSectionTitle("2", "WHAT SHOULD IT DO?")
            Spacer(Modifier.height(8.dp))
            WorkflowChoicePicker(
                selected = action,
                options = availableActions,
                title = { it.label },
                detail = { "${it.category.label} · ${it.subtitle}" },
                icon = { actionIcon(it) },
                onSelect = { action = it },
            )

            val needsContact = action == AutomationAction.CALL_PHONE || action == AutomationAction.SEND_WHATSAPP
            val needsValidTime = trigger == AutomationTrigger.SCHEDULED_TIME || trigger == AutomationTrigger.WEATHER_RAIN
            if (needsContact) {
                Spacer(Modifier.height(12.dp))
                WorkflowTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = "Contact name",
                    placeholder = "Pradeep",
                )
                Spacer(Modifier.height(8.dp))
                WorkflowTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = "Phone number",
                    placeholder = "+1 555 010 1234",
                )
                Spacer(Modifier.height(6.dp))
                if (contactPhone.isBlank()) {
                    WorkflowNotice("Add the contact's phone number before saving.")
                }
            }

            if (action == AutomationAction.CALL_PHONE || action == AutomationAction.SEND_WHATSAPP ||
                action == AutomationAction.SPEAK_TEXT
            ) {
                Spacer(Modifier.height(8.dp))
                WorkflowTextField(
                    value = targetMessage,
                    onValueChange = { targetMessage = it },
                    label = when (action) {
                        AutomationAction.CALL_PHONE -> "Say aloud on this phone"
                        AutomationAction.SEND_WHATSAPP -> "WhatsApp message"
                        else -> "Text to speak"
                    },
                    placeholder = "Hi, I'm Deepak",
                    minLines = 2,
                )
                if (targetMessage.isBlank()) {
                    Spacer(Modifier.height(6.dp))
                    WorkflowNotice("Enter a message before saving.")
                }
                Spacer(Modifier.height(6.dp))
                when (action) {
                    AutomationAction.CALL_PHONE -> WorkflowNotice("OmniDot opens the dialer and speaks this on your phone. Android does not route app speech into the call.")
                    AutomationAction.SEND_WHATSAPP -> WorkflowNotice("OmniDot opens a prefilled WhatsApp chat. You must tap Send; WhatsApp does not allow silent sending here.")
                    else -> Unit
                }
            }

            if (action == AutomationAction.APPLY_PRESET) {
                Spacer(Modifier.height(12.dp))
                CleanTileGrid(
                    items = MatrixPreset.entries,
                    selected = preset,
                    iconProvider = { Icons.Rounded.Wallpaper },
                    titleProvider = { it.title },
                    subtitleProvider = { it.subtitle },
                    onSelect = { preset = it },
                )
            }

            if (action == AutomationAction.HTTP_REQUEST || action == AutomationAction.HTTP_POST ||
                action == AutomationAction.DISCORD_WEBHOOK || action == AutomationAction.SLACK_WEBHOOK ||
                action == AutomationAction.OPEN_URL
            ) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Webhook URL") },
                    placeholder = {
                        Text(
                            when (action) {
                                AutomationAction.DISCORD_WEBHOOK -> "https://discord.com/api/webhooks/..."
                                AutomationAction.SLACK_WEBHOOK -> "https://hooks.slack.com/services/..."
                                else -> "https://homeassistant.local:8123/api/webhook/..."
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmniOrange,
                        unfocusedBorderColor = DotInactive,
                        focusedLabelColor = OmniOrange,
                    ),
                    singleLine = true,
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Fill sample:", color = MutedText, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedButton(
                        onClick = { url = "https://webhook.site/test" },
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("Webhook.site", fontSize = 10.sp) }
                    OutlinedButton(
                        onClick = { url = "http://homeassistant.local:8123/api/webhook/omnidot" },
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("Home Assistant", fontSize = 10.sp) }
                }

                if (action == AutomationAction.HTTP_POST) {
                    Spacer(Modifier.height(12.dp))
                    Text("Custom JSON Body (Leave empty for default device payload):", color = MutedText, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customPayload,
                        onValueChange = { customPayload = it },
                        label = { Text("Custom JSON Payload") },
                        placeholder = { Text("{\n  \"event\": \"{event}\",\n  \"battery\": \"{battery}%\"\n}") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmniOrange,
                            unfocusedBorderColor = DotInactive,
                        ),
                    )
                }
            }

            if (action == AutomationAction.SHOW_NOTIFICATION || action == AutomationAction.DISCORD_WEBHOOK || action == AutomationAction.SLACK_WEBHOOK) {
                Spacer(Modifier.height(16.dp))
                Text("MESSAGE TEXT", color = OmniOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Notification template") },
                    supportingText = { Text("Variables: {battery}, {date}, {year_percent}, {contact}, {time}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmniOrange,
                        unfocusedBorderColor = DotInactive,
                    ),
                )
            }

            val currentRule = remember(
                initialRule,
                trigger,
                action,
                preset,
                url,
                message,
                customPayload,
                contactName,
                contactPhone,
                targetMessage,
                scheduledTime,
                weatherCondition,
            ) {
                AutomationRule(
                    id = initialRule?.id ?: java.util.UUID.randomUUID().toString(),
                    name = workflowName(trigger, action, contactName, scheduledTime),
                    enabled = initialRule?.enabled ?: true,
                    trigger = trigger,
                    action = action,
                    preset = preset,
                    url = url,
                    message = message,
                    customPayload = customPayload,
                    contactName = contactName,
                    contactPhone = contactPhone,
                    targetMessage = targetMessage,
                    scheduledTime = scheduledTime,
                    weatherCondition = weatherCondition,
                )
            }

            Spacer(Modifier.height(18.dp))
            WorkflowSectionTitle("3", "REVIEW")
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = OledBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, DotInactive, RoundedCornerShape(12.dp)),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(triggerIcon(trigger), null, tint = OmniOrange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(workflowName(trigger, action, contactName, scheduledTime), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("${trigger.label}  →  ${action.label}", color = MutedText, fontSize = 11.sp)
                    }
                }
            }

            OutlinedButton(
                onClick = { onTestImmediate(currentRule) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = OmniOrange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("TEST ACTION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OmniOrange)
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSave(currentRule) },
                    enabled = (!needsContact || contactPhone.isNotBlank()) &&
                        (!needsValidTime || scheduledTime.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d"))) &&
                        (action !in setOf(AutomationAction.CALL_PHONE, AutomationAction.SEND_WHATSAPP, AutomationAction.SPEAK_TEXT) ||
                            targetMessage.isNotBlank()),
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OmniOrange),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        if (isEditMode) "SAVE CHANGES" else "SAVE WORKFLOW",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> WorkflowChoicePicker(
    selected: T,
    options: List<T>,
    title: (T) -> String,
    detail: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
) {
    var expanded by remember(selected) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = OledBlack),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (expanded) OmniOrange else DotInactive, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon(selected), null, tint = OmniOrange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title(selected), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(detail(selected), color = MutedText, fontSize = 10.sp, lineHeight = 14.sp)
                }
                Text(if (expanded) "DONE" else "CHANGE", color = OmniOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (option == selected) OmniOrange.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                onSelect(option)
                                expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon(option), null, tint = if (option == selected) OmniOrange else MutedText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title(option), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(detail(option), color = MutedText, fontSize = 10.sp, lineHeight = 13.sp)
                        }
                        if (option == selected) {
                            Icon(Icons.Rounded.Check, null, tint = OmniOrange, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowSectionTitle(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(22.dp)
                .background(OmniOrange.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = OmniOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(title, color = OmniOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun WorkflowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        singleLine = minLines == 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OmniOrange,
            unfocusedBorderColor = DotInactive,
            focusedLabelColor = OmniOrange,
        ),
    )
}

@Composable
private fun WorkflowNotice(text: String) {
    Text(text, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
}

private fun workflowName(
    trigger: AutomationTrigger,
    action: AutomationAction,
    contactName: String,
    scheduledTime: String,
): String = when (trigger) {
    AutomationTrigger.WHATSAPP_MESSAGE ->
        "When ${contactName.ifBlank { "a contact" }} messages → ${action.label.lowercase()}"
    AutomationTrigger.WEATHER_RAIN ->
        "At $scheduledTime, when rain is forecast → ${action.label.lowercase()}"
    AutomationTrigger.SCHEDULED_TIME ->
        "Every day at $scheduledTime → ${action.label.lowercase()}"
    else -> "${trigger.label} → ${action.label}"
}

@Composable
private fun <T> CleanTileGrid(
    items: List<T>,
    selected: T,
    iconProvider: (T) -> ImageVector,
    titleProvider: (T) -> String,
    subtitleProvider: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    val isSelected = item == selected
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) OmniOrange.copy(alpha = 0.15f) else OledBlack,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) OmniOrange else DotInactive,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelect(item) },
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isSelected) OmniOrange.copy(alpha = 0.22f) else SurfaceDark,
                                            RoundedCornerShape(8.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        iconProvider(item),
                                        contentDescription = null,
                                        tint = if (isSelected) OmniOrange else MutedText,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, null, tint = OmniOrange, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = titleProvider(item),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = if (isSelected) Color.White else Color(0xFFDDDDDD),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = subtitleProvider(item),
                                color = MutedText,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                maxLines = 2,
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkflowsCard(onCreateClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DotInactive, RoundedCornerShape(18.dp)),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DotMatrixBrandIcon(Modifier.size(54.dp))
            Spacer(Modifier.height(14.dp))
            Text("No automations yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Make your lockscreen adapt to charger events, reboots, daily time, or remote webhooks.",
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = OmniOrange),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("CREATE WORKFLOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) OmniOrange else SurfaceDark, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) OmniOrange else DotInactive, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.Black else MutedText,
        )
    }
}

@Composable
private fun TagChip(label: String) {
    Box(
        Modifier
            .background(Color(0xFF222222), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, color = OmniOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DotMatrixBrandIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, DotInactive, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { col ->
                        val active = (row == 0 && col <= 1) || (row == 1 && col == 0) || (row == 2 && col <= 2)
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (active) OmniOrange else DotInactive),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideDialog(onDismiss: () -> Unit) {
    var guideTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Triggers", "Webhooks", "Remote / ADB")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDark,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixBrandIcon(Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AUTOMATION GUIDE", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Quick reference for OmniDot workflows", color = MutedText, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = MutedText)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tabs.forEachIndexed { index, title ->
                        CategoryPill(title, guideTab == index) { guideTab = index }
                    }
                }

                Spacer(Modifier.height(18.dp))

                when (guideTab) {
                    0 -> GuideOverviewSection()
                    1 -> GuideTriggersSection()
                    2 -> GuideWebhooksSection()
                    3 -> GuideRemoteAdbSection()
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OmniOrange),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("CLOSE GUIDE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GuideOverviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("OmniDot Automation Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OmniOrange)
        Text(
            "OmniDot lockscreen HUD updates locally on your device without battery drain. You can link hardware events, device boot, or external webhooks directly to wallpaper actions.",
            fontSize = 12.sp,
            color = Color(0xFFDDDDDD),
            lineHeight = 17.sp,
        )
        GuideNote("1. Hardware Events", "Connect/disconnect charger, reach full charge, or low battery.")
        GuideNote("2. System Triggers", "Screen unlock, daily midnight roll-over, device reboot.")
        GuideNote("3. Remote Webhooks", "Send outgoing POST/GET webhooks to Home Assistant, Discord, or n8n.")
        GuideNote("4. Remote Control", "Trigger OmniDot from ADB or Tasker via deep link.")
    }
}

@Composable
private fun GuideTriggersSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Trigger Reference", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OmniOrange)
        AutomationTrigger.entries.forEach { t ->
            Card(
                colors = CardDefaults.cardColors(containerColor = OledBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, DotInactive, RoundedCornerShape(12.dp)),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(triggerIcon(t), null, tint = OmniOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(t.guideInfo, color = MutedText, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun GuideWebhooksSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Remote Webhooks", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OmniOrange)
        Text("Available payload & URL variables:", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = MutedText)
        CodeBox(
            """
            {battery}       -> Current battery (e.g. 84)
            {date}          -> ISO date (e.g. 2026-10-12)
            {year_percent}  -> Year progress % (e.g. 78.4)
            {day_of_year}   -> Current day of year (e.g. 285)
            {preset}        -> Active matrix preset title
            {event}         -> Trigger name
            """.trimIndent(),
        )
        Text("Home Assistant Example:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OmniOrange)
        CodeBox("http://homeassistant.local:8123/api/webhook/phone_charged")
        Text("Discord Webhook Example:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OmniOrange)
        CodeBox("https://discord.com/api/webhooks/123456789/abcdef...")
    }
}

@Composable
private fun GuideRemoteAdbSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Remote Control (ADB & Deep Links)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OmniOrange)
        Text("Deep Links (Browser / Shortcuts):", fontSize = 11.sp, color = MutedText)
        CodeBox(
            """
            # Refresh HUD:
            omnidot://trigger?action=refresh

            # Switch preset:
            omnidot://trigger?preset=life_in_weeks

            # Cycle preset:
            omnidot://trigger?action=cycle
            """.trimIndent(),
        )
        Text("ADB Shell Command:", fontSize = 11.sp, color = MutedText)
        CodeBox("adb shell am broadcast -a com.omnidot.TRIGGER_ACTION --es action \"refresh\"")
    }
}

@Composable
private fun GuideNote(title: String, text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OledBlack),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, DotInactive, RoundedCornerShape(10.dp)),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(text, color = MutedText, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun CodeBox(code: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F0F), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Text(
            text = code,
            color = Color(0xFFFFB74D),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

private fun triggerIcon(trigger: AutomationTrigger): ImageVector = when (trigger) {
    AutomationTrigger.WHATSAPP_MESSAGE -> Icons.Rounded.Forum
    AutomationTrigger.WEATHER_RAIN -> Icons.Rounded.CloudUpload
    AutomationTrigger.SCHEDULED_TIME -> Icons.Rounded.Schedule
    AutomationTrigger.POWER_CONNECTED -> Icons.Rounded.Bolt
    AutomationTrigger.POWER_DISCONNECTED -> Icons.Rounded.PowerOff
    AutomationTrigger.BATTERY_LOW -> Icons.Rounded.BatteryAlert
    AutomationTrigger.BATTERY_OKAY -> Icons.Rounded.BatteryFull
    AutomationTrigger.DEVICE_BOOT -> Icons.Rounded.RestartAlt
    AutomationTrigger.SCREEN_UNLOCKED -> Icons.Rounded.LockOpen
    AutomationTrigger.DAILY_MIDNIGHT -> Icons.Rounded.Schedule
    AutomationTrigger.WIFI_CONNECTED -> Icons.Rounded.Wifi
    AutomationTrigger.WIFI_DISCONNECTED -> Icons.Rounded.WifiOff
    AutomationTrigger.HEADSET_PLUG -> Icons.Rounded.Headphones
    AutomationTrigger.INBOUND_WEBHOOK -> Icons.Rounded.Language
}

private fun actionIcon(action: AutomationAction): ImageVector = when (action) {
    AutomationAction.SPEAK_TEXT -> Icons.Rounded.Info
    AutomationAction.SEND_WHATSAPP -> Icons.Rounded.Forum
    AutomationAction.CALL_PHONE -> Icons.Rounded.Bolt
    AutomationAction.APPLY_PRESET -> Icons.Rounded.Wallpaper
    AutomationAction.REFRESH_CURRENT -> Icons.Rounded.Refresh
    AutomationAction.CYCLE_PRESET -> Icons.Rounded.Sync
    AutomationAction.CYCLE_ACCENT -> Icons.Rounded.Palette
    AutomationAction.HTTP_REQUEST -> Icons.Rounded.Language
    AutomationAction.HTTP_POST -> Icons.Rounded.CloudUpload
    AutomationAction.DISCORD_WEBHOOK -> Icons.Rounded.Forum
    AutomationAction.SLACK_WEBHOOK -> Icons.AutoMirrored.Rounded.Send
    AutomationAction.SHOW_NOTIFICATION -> Icons.Rounded.NotificationsActive
    AutomationAction.VIBRATE -> Icons.Rounded.Vibration
    AutomationAction.COPY_STATS -> Icons.Rounded.ContentCopy
    AutomationAction.OPEN_URL -> Icons.Rounded.OpenInBrowser
}
