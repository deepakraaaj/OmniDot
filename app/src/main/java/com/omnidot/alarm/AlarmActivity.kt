package com.omnidot.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnidot.goals.GoalDays
import com.omnidot.goals.GoalRepository
import com.omnidot.ui.goals.TierShape
import com.omnidot.ui.theme.OmniDotTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Full-screen wake-up shown over the lock screen while a calm alarm rings. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        enableEdgeToEdge()
        val ring = AlarmRingService.Ring.from(intent)
        setContent {
            OmniDotTheme(darkTheme = true) {
                val ringing by AlarmRingService.ringing.collectAsStateWithLifecycle()
                LaunchedEffect(ringing) { if (ringing == null) { delay(300); finish() } }
                val goals by GoalRepository(this).goals.collectAsStateWithLifecycle(emptyList())
                val now by produceState(LocalTime.now()) { while (true) { value = LocalTime.now(); delay(1_000) } }
                val today = LocalDate.now()
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Spacer(Modifier.heightIn(min = 24.dp))
                        Text(now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)), fontSize = 72.sp, fontWeight = FontWeight.Light)
                        Text(ring?.label?.ifBlank { null } ?: "Good morning", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val todays = goals.filter { it.activeOn(today) }.sortedByDescending { it.tier.rank }
                        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            todays.forEach { goal ->
                                val streak = GoalDays.currentStreak(goal, today)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TierShape(goal.tier, 12.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("${goal.emoji} ${goal.title}", style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (streak > 0) Text("🔥 $streak", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f, fill = false).heightIn(min = 32.dp))
                        Row(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { AlarmRingService.send(this@AlarmActivity, AlarmRingService.ACTION_SNOOZE) },
                                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                            ) { Text("Snooze ${ring?.snoozeMinutes ?: 10} min", style = MaterialTheme.typography.titleMedium) }
                            Button(
                                onClick = { AlarmRingService.send(this@AlarmActivity, AlarmRingService.ACTION_DISMISS) },
                                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                            ) { Text("I'm up", style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                }
            }
        }
    }
}
