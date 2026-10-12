package com.omnidot.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidot.ApplyStatus
import com.omnidot.core.AdaptiveMatrixEngine
import com.omnidot.core.MatrixPreset
import com.omnidot.data.OmniDotPreferences
import com.omnidot.ui.theme.DotInactive
import com.omnidot.ui.theme.MutedText
import com.omnidot.ui.theme.OledBlack
import com.omnidot.ui.theme.OmniOrange
import com.omnidot.ui.theme.SurfaceDark
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun PreviewScreen(
    preferences: OmniDotPreferences,
    applyStatus: ApplyStatus,
    onPresetSelected: (MatrixPreset) -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenAutomations: () -> Unit,
    onApply: () -> Unit,
) {
    Scaffold(containerColor = OledBlack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Text("OMNIDOT", fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("Your time. Compiled locally.", color = MutedText, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            PhonePreview(preferences)
            Spacer(Modifier.height(26.dp))
            Text("MATRIX", style = MaterialTheme.typography.labelSmall, color = OmniOrange, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MatrixPreset.entries.forEach { preset ->
                    PresetChip(preset, preset == preferences.preset) { onPresetSelected(preset) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenCustomize, modifier = Modifier.weight(1f)) { Text("CUSTOMIZE") }
                OutlinedButton(onClick = onOpenAutomations, modifier = Modifier.weight(1f)) { Text("AUTOMATE") }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onApply,
                enabled = applyStatus !is ApplyStatus.Applying,
                colors = ButtonDefaults.buttonColors(containerColor = OmniOrange),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (applyStatus is ApplyStatus.Applying) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (applyStatus is ApplyStatus.Applying) "COMPILING…" else "APPLY TO LOCKSCREEN", fontWeight = FontWeight.Bold)
            }
            when (applyStatus) {
                ApplyStatus.Applied -> StatusText("Lockscreen updated. Next refresh is scheduled.", false)
                is ApplyStatus.Error -> StatusText(applyStatus.message, true)
                else -> Unit
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CustomizeScreen(
    preferences: OmniDotPreferences,
    applyStatus: ApplyStatus,
    onBack: () -> Unit,
    onBirthDateChanged: (LocalDate) -> Unit,
    onAutomaticUpdatesChanged: (Boolean) -> Unit,
    onVerticalOffsetChanged: (Float) -> Unit,
    onMatrixScaleChanged: (Float) -> Unit,
    onAccentColorChanged: (Int) -> Unit,
    onApply: () -> Unit,
) {
    Scaffold(containerColor = OledBlack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("← BACK") }
                Spacer(Modifier.weight(1f))
                Text("CUSTOMIZE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(18.dp))
            PhonePreview(preferences)
            Spacer(Modifier.height(18.dp))
            LayoutControls(preferences, onVerticalOffsetChanged, onMatrixScaleChanged)
            Spacer(Modifier.height(12.dp))
            ColorControls(preferences, onAccentColorChanged)
            Spacer(Modifier.height(12.dp))
            PresetConfiguration(preferences, onBirthDateChanged)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Daily refresh", fontWeight = FontWeight.SemiBold)
                    Text("Update automatically after midnight", color = MutedText, fontSize = 12.sp)
                }
                Switch(checked = preferences.automaticUpdates, onCheckedChange = onAutomaticUpdatesChanged)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onApply,
                enabled = applyStatus !is ApplyStatus.Applying,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmniOrange),
            ) {
                Text(if (applyStatus is ApplyStatus.Applying) "APPLYING…" else "APPLY TO LOCKSCREEN", fontWeight = FontWeight.Bold)
            }
            when (applyStatus) {
                ApplyStatus.Applied -> StatusText("Lockscreen updated.", false)
                is ApplyStatus.Error -> StatusText(applyStatus.message, true)
                else -> Unit
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LayoutControls(
    preferences: OmniDotPreferences,
    onOffsetChanged: (Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
) {
    var localOffset by remember { mutableFloatStateOf(preferences.verticalOffset) }
    var localScale by remember { mutableFloatStateOf(preferences.matrixScale) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark).padding(16.dp),
    ) {
        Text("Layout", fontWeight = FontWeight.SemiBold)
        Text("Position · 1% per tap", color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    localOffset = (localOffset - .01f).coerceIn(-.15f, .15f)
                    onOffsetChanged(localOffset)
                },
                modifier = Modifier.weight(1f),
            ) { Text("↑  UP") }
            OutlinedButton(
                onClick = { localOffset = 0f; onOffsetChanged(0f) },
                modifier = Modifier.weight(1f),
            ) { Text("RESET") }
            OutlinedButton(
                onClick = {
                    localOffset = (localOffset + .01f).coerceIn(-.15f, .15f)
                    onOffsetChanged(localOffset)
                },
                modifier = Modifier.weight(1f),
            ) { Text("DOWN  ↓") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Zoom · ${(localScale * 100).roundToInt()}%", color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    localScale = (localScale - .05f).coerceIn(.6f, 1.6f)
                    onScaleChanged(localScale)
                },
                modifier = Modifier.weight(1f),
            ) { Text("−  OUT") }
            OutlinedButton(
                onClick = { localScale = 1f; onScaleChanged(1f) },
                modifier = Modifier.weight(1f),
            ) { Text("RESET") }
            OutlinedButton(
                onClick = {
                    localScale = (localScale + .05f).coerceIn(.6f, 1.6f)
                    onScaleChanged(localScale)
                },
                modifier = Modifier.weight(1f),
            ) { Text("IN  +") }
        }
        if (localScale > 1f) {
            Spacer(Modifier.height(10.dp))
            Text(
                "⚠ Zoom exceeds the fitted screen area and may crop the matrix.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ColorControls(preferences: OmniDotPreferences, onColorChanged: (Int) -> Unit) {
    val colors = listOf(
        0xFFE65100.toInt(),
        0xFF00C853.toInt(),
        0xFF2979FF.toInt(),
        0xFFAA00FF.toInt(),
        0xFFFF1744.toInt(),
        0xFFFFD600.toInt(),
        0xFFFFFFFF.toInt(),
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark).padding(16.dp),
    ) {
        Text("Color", fontWeight = FontWeight.SemiBold)
        Text("Choose your active-dot color", color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            colors.forEach { argb ->
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(argb))
                        .border(
                            width = if (preferences.accentColor == argb) 3.dp else 1.dp,
                            color = if (preferences.accentColor == argb) androidx.compose.ui.graphics.Color.White else DotInactive,
                            shape = CircleShape,
                        )
                        .clickable { onColorChanged(argb) },
                )
            }
        }
    }
}

@Composable
private fun PhonePreview(preferences: OmniDotPreferences) {
    val bitmap = remember(preferences) {
        AdaptiveMatrixEngine().render(android.util.Size(432, 900), preferences).asImageBitmap()
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.width(216.dp).height(450.dp).clip(RoundedCornerShape(30.dp))
                .border(2.dp, DotInactive, RoundedCornerShape(30.dp)).background(SurfaceDark),
        ) {
            Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Text(
                "10:09",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

@Composable
private fun PresetChip(preset: MatrixPreset, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(156.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) OmniOrange.copy(alpha = .18f) else SurfaceDark)
            .border(1.dp, if (selected) OmniOrange else DotInactive, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        Text(preset.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(preset.subtitle, color = MutedText, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun PresetConfiguration(
    preferences: OmniDotPreferences,
    onBirthDateChanged: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    when (preferences.preset) {
        MatrixPreset.LIFE_IN_WEEKS -> OutlinedButton(onClick = {
            val value = preferences.birthDate
            DatePickerDialog(context, { _, year, month, day ->
                onBirthDateChanged(LocalDate.of(year, month + 1, day))
            }, value.year, value.monthValue - 1, value.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("BIRTH DATE · ${preferences.birthDate}")
        }
        else -> Unit
    }
}

@Composable
private fun StatusText(message: String, error: Boolean) {
    Text(
        message,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 10.dp),
    )
}
