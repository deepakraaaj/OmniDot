# OmniDot UI/UX Overhaul & Goals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clean the repo, redesign OmniDot into a calm three-tab app (Workflows · Goals · Settings), and add tiered Goals with daily check-ins whose biggest daily win is drawn on the lockscreen.

**Architecture:** Pure-Kotlin domain logic (goal day states, grids, streaks, codecs, sentences, validation, scheduling math) lives in small files with JVM unit tests. Android glue (DataStore repositories, WorkManager workers, receivers, wallpaper apply) stays thin. Compose UI is rebuilt per tab under `ui/workflows`, `ui/goals`, `ui/settings`, sharing a new calm theme.

**Tech Stack:** Kotlin 2.0.21, AGP 8.12, Jetpack Compose (BOM 2024.12.01, Material 3), DataStore Preferences, WorkManager 2.10, JUnit 4, org.json.

**Spec:** `docs/superpowers/specs/2026-10-12-ui-ux-goals-design.md`

## Global Constraints

- Branch `ui-ux-overhaul`; minSdk 26, targetSdk/compileSdk 35; JDK 17 via Gradle toolchain.
- Build/test command for every task: `./gradlew testDebugUnitTest assembleDebug` (must pass with the machine's default JDK after Task 1).
- Colours: dark bg `#121417`, dark surface `#1B1E22`; light bg `#F6F5F2`, light surface `#FFFFFF`; accent teal `#5FB3A6`; tiers Small `#7BC47F`, Medium `#6FA8DC`, Big `#E0B65A`.
- Tier shapes on the lockscreen: Small ● circle, Medium ■ rounded square, Big ◆ diamond.
- Copy: sentence case everywhere; no ALL-CAPS letter-spaced headings; no toasts for trivial actions; deletes offer Undo via Snackbar.
- Hidden triggers (no backend): `SCHEDULED_TIME`, `WEATHER_RAIN`, `DAILY_MIDNIGHT`.
- Wallpaper only re-renders automatically if the user pressed **Apply to lock screen** at least once (`wallpaperEnabled`).
- Calm voice defaults: rate 0.9, pitch 1.0, volume 0.8.
- Commit after every task with message ending in `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. A goal whose dates are edited after check-ins exist (start moved later / end moved earlier) must not show check-ins outside its range as achieved — test in Task 3.
2. A notification "Done" tap for a goal that was deleted or archived since the reminder fired must be a silent no-op — test in Task 4.
3. Reminder time already passed today, and DST transitions, must schedule the next occurrence correctly — test in Task 7.
4. Corrupted or partially-old goal JSON must drop only the bad entries, not wipe all goals — test in Task 4.
5. Month grids for months starting on Sunday (6 rows) and February in leap years must align to Monday-first columns — test in Task 3.

---

## File Structure

```
app/src/main/java/com/omnidot/
  OmniDotApplication.kt                 NEW  registers runtime device-event receiver
  MainActivity.kt                       MOD  thin host: theme + OmniDotApp, deep link w/ key
  OmniDotViewModel.kt                   MOD  workflows + settings state
  core/
    MatrixPreset.kt                     MOD  GOALS, YEAR_PROGRESS, LIFE_IN_WEEKS (+ legacy mapping)
    LockPeriod.kt                       NEW  THIS_MONTH, LAST_12_WEEKS, THIS_YEAR
    AdaptiveMatrixEngine.kt             MOD  goals rendering, shared geometry
    MatrixSyncWorker.kt                 MOD  uses WallpaperRefresher
    WallpaperRefresher.kt               NEW  apply / refreshIfEnabled with goals
    BootReceiver.kt                     MOD  also reschedules check-in reminder
  data/PreferencesManager.kt            MOD  new prefs, remote key, drop sprintStart
  goals/
    Goal.kt                             NEW  Goal, GoalTier, activeOn, toggle
    GoalDays.kt                         NEW  DayState, dayState(), streak, progress
    GoalsMatrix.kt                      NEW  period grids + header
    GoalCodec.kt                        NEW  JSON encode/decode (lenient)
    GoalListOps.kt                      NEW  pure list mutations
    GoalRepository.kt                   NEW  DataStore wrapper
    reminder/ReminderSchedule.kt        NEW  delay math + pending goals
    reminder/CheckInReminderWorker.kt   NEW  WorkManager daily reminder
    reminder/CheckInNotifier.kt         NEW  builds notifications
    reminder/CheckInReceiver.kt         NEW  handles Done / Not today
  automation/
    AutomationRule.kt                   MOD  available flag, calm defaults, no personal defaults
    AutomationRepository.kt             MOD  seed defaults once, decode defaults
    AutomationReceiver.kt               MOD  remote key check
    AutomationEngine.kt                 MOD  bell, volume, WallpaperRefresher
    RemoteTriggerGuard.kt               NEW  key generation + check
    VoiceStyle.kt                       NEW  CALM / NEUTRAL / CHEERFUL
    VoiceFeedbackManager.kt             MOD  volume, queue, timeout
    BellTone.kt                         NEW  pure soft-bell PCM generator
    SoundSynthesizer.kt                 MOD  playSoftBell()
    WorkflowText.kt                     NEW  sentences, last-run text, canSave
    WorkflowRecipes.kt                  NEW  empty-state recipes
    HealthCheck.kt                      NEW  pure health issues + Android probe
  ui/
    OmniDotApp.kt                       NEW  scaffold, bottom nav, snackbar
    theme/Color.kt, Theme.kt            MOD  calm light/dark tokens
    components/Common.kt                NEW  SectionHeader, SettingsCard, PhonePreview
    workflows/WorkflowsScreen.kt        NEW  list, health banner, recipes
    workflows/WorkflowCard.kt           NEW
    workflows/WorkflowIcons.kt          NEW  trigger/action icons
    workflows/WorkflowEditor.kt         NEW  full-screen editor
    workflows/ChoiceSheet.kt            NEW  searchable bottom-sheet picker
    goals/GoalsViewModel.kt             NEW
    goals/GoalsScreen.kt                NEW  today card + list
    goals/GoalVisuals.kt                NEW  tier colour/icon/shape
    goals/GoalEditorSheet.kt            NEW
    goals/GoalDetailSheet.kt            NEW  calendar toggle
    goals/LockscreenSection.kt          NEW  preview, view/period, sliders, apply
    settings/SettingsScreen.kt          NEW
  ui/AutomationScreen.kt, ui/PreviewScreen.kt   DELETE (Task 17)
app/src/test/java/com/omnidot/...       tests per task
```

---

### Task 1: Repo hygiene and JDK toolchain

**Files:**
- Delete: 14 root `*.png` files
- Modify: `settings.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`

**Interfaces:** Produces: `testImplementation(libs.json)` available for later JSON tests.

- [ ] **Step 1: Confirm the default-JDK build fails**

Run: `./gradlew testDebugUnitTest -q`
Expected: FAIL with `does not provide the required capabilities: [JAVA_COMPILER]`

- [ ] **Step 2: Delete root screenshots**

```bash
git rm -q edit_header.png edit_mode.png final_screen.png guide_adb.png guide_modal.png guide_webhooks.png test_edit_1.png test_edit_2.png toast_switch.png toast_test.png toast_toggle.png updated_ui.png
git status --short   # expect no other *.png left in repo root: ls *.png → none
```

- [ ] **Step 3: Add foojay resolver to `settings.gradle.kts`** (insert after `pluginManagement { … }` block)

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
```

- [ ] **Step 4: Pin toolchain in `app/build.gradle.kts`** — replace `kotlinOptions { jvmTarget = "17" }` with:

```kotlin
kotlin { jvmToolchain(17) }
```

and add the test JSON dependency below `testImplementation(libs.junit)`:

```kotlin
testImplementation(libs.json)
```

- [ ] **Step 5: Add catalog entry in `gradle/libs.versions.toml`**

Under `[versions]`: `json = "20240303"`; under `[libraries]`:

```toml
json = { module = "org.json:json", version.ref = "json" }
```

- [ ] **Step 6: Verify with the default JDK**

Run: `./gradlew testDebugUnitTest assembleDebug -q`
Expected: BUILD SUCCESSFUL (no output with -q), exit 0.

- [ ] **Step 7: Commit**

```bash
git add -A settings.gradle.kts app/build.gradle.kts gradle/libs.versions.toml
git commit -m "chore: remove stray screenshots and pin JDK 17 toolchain"
```

---

### Task 2: Presets, lock period and preferences

**Files:**
- Modify: `core/MatrixPreset.kt`, `data/PreferencesManager.kt`, `core/AdaptiveMatrixEngine.kt` (stateFor only), `OmniDotViewModel.kt` (remove sprint), `ui/PreviewScreen.kt` (remove sprint button)
- Create: `core/LockPeriod.kt`, `automation/VoiceStyle.kt`
- Test: `test/.../core/MatrixPresetTest.kt`, `test/.../core/AdaptiveMatrixEngineTest.kt`

**Interfaces:**
- Produces: `enum class MatrixPreset { GOALS, YEAR_PROGRESS, LIFE_IN_WEEKS }` with `title`, `subtitle`, `columns`, `fromStored(String?)`; `enum class LockPeriod(val label: String) { THIS_MONTH, LAST_12_WEEKS, THIS_YEAR }` with `fromStored`; `enum class VoiceStyle(val label: String, val pitch: Float, val rate: Float, val volume: Float)` with `fromStored`.
- Produces `OmniDotPreferences` fields: `preset`, `birthDate`, `accentColor`, `automaticUpdates`, `verticalOffset`, `matrixScale`, `lockPeriod: LockPeriod`, `wallpaperEnabled: Boolean`, `reminderEnabled: Boolean`, `reminderConfigured: Boolean`, `reminderMinuteOfDay: Int`, `voiceStyle: VoiceStyle`.
- Produces `PreferencesManager` setters: `setPreset`, `setBirthDate`, `setAutomaticUpdates`, `setVerticalOffset`, `setMatrixScale`, `setAccentColor`, `setLockPeriod(LockPeriod)`, `setWallpaperEnabled(Boolean)`, `setReminder(enabled: Boolean)` (also sets configured=true), `setReminderMinuteOfDay(Int)`, `setVoiceStyle(VoiceStyle)`, `suspend fun remoteKey(): String` (Task 9 fills generation; here returns stored or "").

- [ ] **Step 1: Write failing tests** — `app/src/test/java/com/omnidot/core/MatrixPresetTest.kt`

```kotlin
package com.omnidot.core

import com.omnidot.automation.VoiceStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixPresetTest {
    @Test
    fun `removed presets migrate to year progress`() {
        assertEquals(MatrixPreset.YEAR_PROGRESS, MatrixPreset.fromStored("SPRINT_100"))
        assertEquals(MatrixPreset.YEAR_PROGRESS, MatrixPreset.fromStored("CONTRIBUTION_HEATMAP"))
    }

    @Test
    fun `unknown or missing preset defaults to goals`() {
        assertEquals(MatrixPreset.GOALS, MatrixPreset.fromStored(null))
        assertEquals(MatrixPreset.GOALS, MatrixPreset.fromStored("NOPE"))
        assertEquals(MatrixPreset.LIFE_IN_WEEKS, MatrixPreset.fromStored("LIFE_IN_WEEKS"))
    }

    @Test
    fun `lock period and voice style fall back safely`() {
        assertEquals(LockPeriod.THIS_MONTH, LockPeriod.fromStored(null))
        assertEquals(LockPeriod.THIS_YEAR, LockPeriod.fromStored("THIS_YEAR"))
        assertEquals(VoiceStyle.CALM, VoiceStyle.fromStored("bogus"))
    }

    @Test
    fun `calm voice matches spec values`() {
        assertEquals(0.9f, VoiceStyle.CALM.rate)
        assertEquals(1.0f, VoiceStyle.CALM.pitch)
        assertEquals(0.8f, VoiceStyle.CALM.volume)
    }
}
```

In `AdaptiveMatrixEngineTest.kt` delete the two tests `sprint is zero before start…` and `contribution matrix is exactly 52 by 7`.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew testDebugUnitTest -q`
Expected: compilation FAIL — `Unresolved reference: LockPeriod`, `VoiceStyle`, `GOALS`.

- [ ] **Step 3: Implement**

`core/MatrixPreset.kt`:

```kotlin
package com.omnidot.core

enum class MatrixPreset(
    val title: String,
    val subtitle: String,
    val columns: Int,
) {
    GOALS("Goals", "Your biggest win each day", 7),
    YEAR_PROGRESS("Year in dots", "Every day of the current year", 21),
    LIFE_IN_WEEKS("Life in weeks", "4,160 weeks across 80 years", 52);

    companion object {
        private val legacyToYear = setOf("SPRINT_100", "CONTRIBUTION_HEATMAP")

        fun fromStored(value: String?): MatrixPreset = when {
            value in legacyToYear -> YEAR_PROGRESS
            else -> entries.firstOrNull { it.name == value } ?: GOALS
        }
    }
}
```

`core/LockPeriod.kt`:

```kotlin
package com.omnidot.core

enum class LockPeriod(val label: String) {
    THIS_MONTH("This month"),
    LAST_12_WEEKS("Last 12 weeks"),
    THIS_YEAR("This year");

    companion object {
        fun fromStored(value: String?): LockPeriod =
            entries.firstOrNull { it.name == value } ?: THIS_MONTH
    }
}
```

`automation/VoiceStyle.kt`:

```kotlin
package com.omnidot.automation

enum class VoiceStyle(val label: String, val pitch: Float, val rate: Float, val volume: Float) {
    CALM("Calm", pitch = 1.0f, rate = 0.9f, volume = 0.8f),
    NEUTRAL("Neutral", pitch = 1.0f, rate = 1.0f, volume = 0.9f),
    CHEERFUL("Cheerful", pitch = 1.15f, rate = 1.05f, volume = 1.0f);

    companion object {
        fun fromStored(value: String?): VoiceStyle =
            entries.firstOrNull { it.name == value } ?: CALM
    }
}
```

`data/PreferencesManager.kt` — replace the data class and Keys, keep the DataStore delegate:

```kotlin
data class OmniDotPreferences(
    val preset: MatrixPreset = MatrixPreset.GOALS,
    val birthDate: LocalDate = LocalDate.of(1995, 1, 1),
    val accentColor: Int = 0xFF5FB3A6.toInt(),
    val automaticUpdates: Boolean = true,
    val verticalOffset: Float = 0f,
    val matrixScale: Float = 1f,
    val lockPeriod: LockPeriod = LockPeriod.THIS_MONTH,
    val wallpaperEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderConfigured: Boolean = false,
    val reminderMinuteOfDay: Int = 21 * 60,
    val voiceStyle: VoiceStyle = VoiceStyle.CALM,
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val preset = stringPreferencesKey("matrix_preset")
        val birthDate = longPreferencesKey("birth_date_epoch_day")
        val accent = intPreferencesKey("accent_argb")
        val automaticUpdates = booleanPreferencesKey("automatic_updates")
        val verticalOffset = floatPreferencesKey("vertical_offset")
        val matrixScale = floatPreferencesKey("matrix_scale")
        val lockPeriod = stringPreferencesKey("lock_period")
        val wallpaperEnabled = booleanPreferencesKey("wallpaper_enabled")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderMinute = intPreferencesKey("reminder_minute_of_day")
        val voiceStyle = stringPreferencesKey("voice_style")
        val remoteKey = stringPreferencesKey("remote_trigger_key")
    }

    val preferences: Flow<OmniDotPreferences> = context.omniDotDataStore.data.map { values ->
        val defaults = OmniDotPreferences()
        OmniDotPreferences(
            preset = MatrixPreset.fromStored(values[Keys.preset]),
            birthDate = values[Keys.birthDate]?.let(LocalDate::ofEpochDay) ?: defaults.birthDate,
            accentColor = values[Keys.accent] ?: defaults.accentColor,
            automaticUpdates = values[Keys.automaticUpdates] ?: true,
            verticalOffset = (values[Keys.verticalOffset] ?: 0f).coerceIn(-0.15f, 0.15f),
            matrixScale = (values[Keys.matrixScale] ?: 1f).coerceIn(0.6f, 1.6f),
            lockPeriod = LockPeriod.fromStored(values[Keys.lockPeriod]),
            wallpaperEnabled = values[Keys.wallpaperEnabled] ?: false,
            reminderEnabled = values[Keys.reminderEnabled] ?: false,
            reminderConfigured = values[Keys.reminderEnabled] != null,
            reminderMinuteOfDay = (values[Keys.reminderMinute] ?: defaults.reminderMinuteOfDay).coerceIn(0, 1439),
            voiceStyle = VoiceStyle.fromStored(values[Keys.voiceStyle]),
        )
    }

    suspend fun current(): OmniDotPreferences = preferences.first()

    suspend fun setPreset(preset: MatrixPreset) = edit { it[Keys.preset] = preset.name }
    suspend fun setBirthDate(date: LocalDate) = edit { it[Keys.birthDate] = date.toEpochDay() }
    suspend fun setAutomaticUpdates(enabled: Boolean) = edit { it[Keys.automaticUpdates] = enabled }
    suspend fun setVerticalOffset(offset: Float) = edit { it[Keys.verticalOffset] = offset.coerceIn(-0.15f, 0.15f) }
    suspend fun setMatrixScale(scale: Float) = edit { it[Keys.matrixScale] = scale.coerceIn(0.6f, 1.6f) }
    suspend fun setAccentColor(color: Int) = edit { it[Keys.accent] = color }
    suspend fun setLockPeriod(period: LockPeriod) = edit { it[Keys.lockPeriod] = period.name }
    suspend fun setWallpaperEnabled(enabled: Boolean) = edit { it[Keys.wallpaperEnabled] = enabled }
    suspend fun setReminder(enabled: Boolean) = edit { it[Keys.reminderEnabled] = enabled }
    suspend fun setReminderMinuteOfDay(minute: Int) = edit { it[Keys.reminderMinute] = minute.coerceIn(0, 1439) }
    suspend fun setVoiceStyle(style: VoiceStyle) = edit { it[Keys.voiceStyle] = style.name }

    suspend fun remoteKey(): String = context.omniDotDataStore.data.first()[Keys.remoteKey].orEmpty()

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.omniDotDataStore.edit(block)
    }
}
```

Add imports `com.omnidot.core.LockPeriod`, `com.omnidot.automation.VoiceStyle`; remove `sprintStart` usages.

`core/AdaptiveMatrixEngine.kt` `stateFor` — delete the `SPRINT_100` and `CONTRIBUTION_HEATMAP` branches, delete the `deterministicIntensity` function and its use in `render` (the `if (!isCurrent && settings.preset == MatrixPreset.CONTRIBUTION_HEATMAP …)` block becomes `paint.alpha = 255`, and the column/row choice becomes the non-heatmap branch only). Add the branch:

```kotlin
MatrixPreset.GOALS -> throw IllegalArgumentException("Goals use GoalsMatrix, not stateFor")
```

and at the top of `render`, temporarily (Task 5 replaces it) map GOALS to year so nothing crashes:

```kotlin
val settings = if (settings.preset == MatrixPreset.GOALS) settings.copy(preset = MatrixPreset.YEAR_PROGRESS) else settings
```

(rename the parameter to `requested` and shadow as `settings`).

`OmniDotViewModel.kt`: delete `restartSprint()`. `ui/PreviewScreen.kt`: remove the `onRestartSprint` parameter and the `MatrixPreset.SPRINT_100 ->` branch in `PresetConfiguration`; `MainActivity.kt`: remove the `onRestartSprint = …` argument.

- [ ] **Step 4: Run tests**

Run: `./gradlew testDebugUnitTest assembleDebug -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "refactor: replace sprint and heatmap presets with goals view and new prefs"
```

---

### Task 3: Goal model, day states, grids

**Files:**
- Create: `goals/Goal.kt`, `goals/GoalDays.kt`, `goals/GoalsMatrix.kt`
- Test: `test/.../goals/GoalDaysTest.kt`, `test/.../goals/GoalsMatrixTest.kt`

**Interfaces:**
- Produces:
  - `enum class GoalTier(val rank: Int, val label: String, val defaultEmoji: String) { SMALL(1,"Small","🌱"), MEDIUM(2,"Medium","🌊"), BIG(3,"Big","⛰️") }` + `fromStored`
  - `data class Goal(id, title, emoji, tier, startDate, endDate: LocalDate?, checkIns: Set<LocalDate>, archived = false)` with `fun activeOn(date: LocalDate): Boolean`, `fun canCheckIn(date: LocalDate, today: LocalDate): Boolean`, `fun toggle(date: LocalDate, today: LocalDate): Goal`, `fun withCheckIn(date: LocalDate, done: Boolean, today: LocalDate): Goal`
  - `sealed interface DayState { data class Achieved(val tier: GoalTier); data object Missed; data object NoGoal; data object TodayPending; data object Future }`
  - `object GoalDays { fun dayState(date, goals, today): DayState; fun currentStreak(goal, today): Int; fun progress(goal, today): Pair<Int, Int> }`
  - `data class GoalsMatrixState(val cells: List<LocalDate?>, val columns: Int, val rows: Int, val title: String, val detail: String)`; `object GoalsMatrix { fun build(goals: List<Goal>, period: LockPeriod, today: LocalDate): GoalsMatrixState }`

- [ ] **Step 1: Write failing tests** — `app/src/test/java/com/omnidot/goals/GoalDaysTest.kt`

```kotlin
package com.omnidot.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalDaysTest {
    private val today = LocalDate.of(2026, 10, 12)
    private fun goal(
        tier: GoalTier,
        start: LocalDate = LocalDate.of(2026, 10, 1),
        end: LocalDate? = null,
        checks: Set<LocalDate> = emptySet(),
        archived: Boolean = false,
    ) = Goal("id-$tier-$start", "g", tier.defaultEmoji, tier, start, end, checks, archived)

    @Test
    fun `achieved shows the highest tier checked that day`() {
        val d = LocalDate.of(2026, 10, 5)
        val goals = listOf(goal(GoalTier.SMALL, checks = setOf(d)), goal(GoalTier.BIG, checks = setOf(d)), goal(GoalTier.MEDIUM))
        assertEquals(DayState.Achieved(GoalTier.BIG), GoalDays.dayState(d, goals, today))
    }

    @Test
    fun `missed today-pending no-goal and future states`() {
        val goals = listOf(goal(GoalTier.SMALL))
        assertEquals(DayState.Missed, GoalDays.dayState(LocalDate.of(2026, 10, 3), goals, today))
        assertEquals(DayState.TodayPending, GoalDays.dayState(today, goals, today))
        assertEquals(DayState.NoGoal, GoalDays.dayState(LocalDate.of(2026, 9, 30), goals, today))
        assertEquals(DayState.Future, GoalDays.dayState(today.plusDays(1), goals, today))
        assertEquals(DayState.NoGoal, GoalDays.dayState(today, emptyList(), today))
    }

    @Test
    fun `archived goals and check-ins outside the date range never count`() {
        val outside = LocalDate.of(2026, 9, 28)
        val shifted = goal(GoalTier.BIG, start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 10), checks = setOf(outside, LocalDate.of(2026, 10, 11)))
        assertEquals(DayState.NoGoal, GoalDays.dayState(outside, listOf(shifted), today))
        assertEquals(DayState.NoGoal, GoalDays.dayState(LocalDate.of(2026, 10, 11), listOf(shifted), today))
        val archived = goal(GoalTier.BIG, checks = setOf(LocalDate.of(2026, 10, 5)), archived = true)
        assertEquals(DayState.NoGoal, GoalDays.dayState(LocalDate.of(2026, 10, 5), listOf(archived), today))
    }

    @Test
    fun `toggle only works inside range and not in the future`() {
        val g = goal(GoalTier.SMALL)
        assertTrue(g.toggle(today, today).checkIns.contains(today))
        assertFalse(g.toggle(today, today).toggle(today, today).checkIns.contains(today))
        assertEquals(g, g.toggle(today.plusDays(1), today))
        assertEquals(g, g.toggle(LocalDate.of(2026, 9, 1), today))
        assertTrue(g.withCheckIn(today, true, today).withCheckIn(today, true, today).checkIns == setOf(today))
    }

    @Test
    fun `streak counts back from today or yesterday`() {
        val checks = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(2, GoalDays.currentStreak(goal(GoalTier.SMALL, checks = checks), today))
        assertEquals(3, GoalDays.currentStreak(goal(GoalTier.SMALL, checks = checks + today), today))
        assertEquals(0, GoalDays.currentStreak(goal(GoalTier.SMALL, checks = setOf(today.minusDays(3))), today))
    }

    @Test
    fun `progress counts check-ins over elapsed active days`() {
        val g = goal(GoalTier.SMALL, start = LocalDate.of(2026, 10, 3), checks = setOf(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 1)))
        assertEquals(2 to 10, GoalDays.progress(g, today))
        val notStarted = goal(GoalTier.SMALL, start = today.plusDays(3))
        assertEquals(0 to 0, GoalDays.progress(notStarted, today))
    }
}
```

`app/src/test/java/com/omnidot/goals/GoalsMatrixTest.kt`

```kotlin
package com.omnidot.goals

import com.omnidot.core.LockPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GoalsMatrixTest {
    @Test
    fun `month starting on sunday needs six monday-first rows`() {
        // 2026-03-01 is a Sunday, March has 31 days
        val m = GoalsMatrix.build(emptyList(), LockPeriod.THIS_MONTH, LocalDate.of(2026, 3, 15))
        assertEquals(7, m.columns)
        assertEquals(6, m.rows)
        assertEquals(6 + 31, m.cells.size)
        (0 until 6).forEach { assertNull(m.cells[it]) }
        assertEquals(LocalDate.of(2026, 3, 1), m.cells[6])
        assertEquals("March", m.title)
    }

    @Test
    fun `leap february starting on thursday`() {
        // 2024-02-01 is a Thursday, 29 days
        val m = GoalsMatrix.build(emptyList(), LockPeriod.THIS_MONTH, LocalDate.of(2024, 2, 10))
        assertEquals(3 + 29, m.cells.size)
        assertEquals(5, m.rows)
        assertEquals(LocalDate.of(2024, 2, 29), m.cells.last())
    }

    @Test
    fun `last 12 weeks ends on the sunday of this week`() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday
        val m = GoalsMatrix.build(emptyList(), LockPeriod.LAST_12_WEEKS, today)
        assertEquals(84, m.cells.size)
        assertEquals(12, m.rows)
        assertEquals(LocalDate.of(2026, 10, 18), m.cells.last())
        assertEquals(LocalDate.of(2026, 7, 27), m.cells.first())
        assertEquals("Last 12 weeks", m.title)
    }

    @Test
    fun `this year uses 21 columns`() {
        val m = GoalsMatrix.build(emptyList(), LockPeriod.THIS_YEAR, LocalDate.of(2024, 6, 1))
        assertEquals(366, m.cells.size)
        assertEquals(21, m.columns)
        assertEquals(18, m.rows)
        assertEquals("2024", m.title)
    }

    @Test
    fun `detail counts achieved days so far and prompts when no goals`() {
        val today = LocalDate.of(2026, 10, 12)
        assertEquals("Add a goal to start", GoalsMatrix.build(emptyList(), LockPeriod.THIS_MONTH, today).detail)
        val g = Goal("a", "Run", "🌱", GoalTier.SMALL, LocalDate.of(2026, 10, 1), null, setOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 12)))
        assertEquals("2 of 12 days showed up", GoalsMatrix.build(listOf(g), LockPeriod.THIS_MONTH, today).detail)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew testDebugUnitTest -q` → FAIL: unresolved `Goal`, `GoalDays`, `GoalsMatrix`.

- [ ] **Step 3: Implement**

`goals/Goal.kt`:

```kotlin
package com.omnidot.goals

import java.time.LocalDate

enum class GoalTier(val rank: Int, val label: String, val defaultEmoji: String) {
    SMALL(1, "Small", "🌱"),
    MEDIUM(2, "Medium", "🌊"),
    BIG(3, "Big", "⛰️");

    companion object {
        fun fromStored(value: String?): GoalTier = entries.firstOrNull { it.name == value } ?: SMALL
    }
}

data class Goal(
    val id: String,
    val title: String,
    val emoji: String,
    val tier: GoalTier,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val checkIns: Set<LocalDate>,
    val archived: Boolean = false,
) {
    fun activeOn(date: LocalDate): Boolean =
        !archived && !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))

    fun canCheckIn(date: LocalDate, today: LocalDate): Boolean = activeOn(date) && !date.isAfter(today)

    fun toggle(date: LocalDate, today: LocalDate): Goal =
        withCheckIn(date, date !in checkIns, today)

    fun withCheckIn(date: LocalDate, done: Boolean, today: LocalDate): Goal {
        if (!canCheckIn(date, today)) return this
        return copy(checkIns = if (done) checkIns + date else checkIns - date)
    }
}
```

`goals/GoalDays.kt`:

```kotlin
package com.omnidot.goals

import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface DayState {
    data class Achieved(val tier: GoalTier) : DayState
    data object Missed : DayState
    data object NoGoal : DayState
    data object TodayPending : DayState
    data object Future : DayState
}

object GoalDays {
    fun dayState(date: LocalDate, goals: List<Goal>, today: LocalDate): DayState {
        if (date.isAfter(today)) return DayState.Future
        val active = goals.filter { it.activeOn(date) }
        if (active.isEmpty()) return DayState.NoGoal
        val best = active.filter { date in it.checkIns }.maxByOrNull { it.tier.rank }
        return when {
            best != null -> DayState.Achieved(best.tier)
            date == today -> DayState.TodayPending
            else -> DayState.Missed
        }
    }

    fun currentStreak(goal: Goal, today: LocalDate): Int {
        var day = if (today in goal.checkIns) today else today.minusDays(1)
        var count = 0
        while (day in goal.checkIns && goal.activeOn(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** (achieved days, elapsed active days) up to and including today. */
    fun progress(goal: Goal, today: LocalDate): Pair<Int, Int> {
        if (today.isBefore(goal.startDate)) return 0 to 0
        val last = goal.endDate?.takeIf { it.isBefore(today) } ?: today
        val elapsed = ChronoUnit.DAYS.between(goal.startDate, last).toInt() + 1
        val achieved = goal.checkIns.count { !it.isBefore(goal.startDate) && !it.isAfter(last) }
        return achieved to elapsed
    }
}
```

`goals/GoalsMatrix.kt`:

```kotlin
package com.omnidot.goals

import com.omnidot.core.LockPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil

data class GoalsMatrixState(
    val cells: List<LocalDate?>,
    val columns: Int,
    val rows: Int,
    val title: String,
    val detail: String,
)

object GoalsMatrix {
    fun build(goals: List<Goal>, period: LockPeriod, today: LocalDate): GoalsMatrixState {
        val (cells, columns, title) = when (period) {
            LockPeriod.THIS_MONTH -> {
                val first = today.withDayOfMonth(1)
                val lead = first.dayOfWeek.value - 1
                val days = (0 until today.lengthOfMonth()).map { first.plusDays(it.toLong()) }
                Triple(List<LocalDate?>(lead) { null } + days, 7,
                    today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
            }
            LockPeriod.LAST_12_WEEKS -> {
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val start = end.minusDays(83)
                Triple((0 until 84).map { start.plusDays(it.toLong()) }, 7, period.label)
            }
            LockPeriod.THIS_YEAR -> {
                val first = today.withDayOfYear(1)
                Triple((0 until today.lengthOfYear()).map { first.plusDays(it.toLong()) }, 21, today.year.toString())
            }
        }
        val rows = ceil(cells.size / columns.toFloat()).toInt()
        val visibleGoals = goals.filterNot { it.archived }
        val detail = if (visibleGoals.isEmpty()) {
            "Add a goal to start"
        } else {
            val elapsed = cells.filterNotNull().filter { !it.isAfter(today) }
            val achieved = elapsed.count { GoalDays.dayState(it, visibleGoals, today) is DayState.Achieved }
            "$achieved of ${elapsed.size} days showed up"
        }
        return GoalsMatrixState(cells, columns, rows, title, detail)
    }
}
```

- [ ] **Step 4: Run tests** — `./gradlew testDebugUnitTest -q` → PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/omnidot/goals app/src/test/java/com/omnidot/goals
git commit -m "feat(goals): add tiered goal model, day states and period grids"
```

---

### Task 4: Goal persistence

**Files:**
- Create: `goals/GoalCodec.kt`, `goals/GoalListOps.kt`, `goals/GoalRepository.kt`
- Test: `test/.../goals/GoalCodecTest.kt`, `test/.../goals/GoalListOpsTest.kt`

**Interfaces:**
- Consumes: `Goal`, `GoalTier` (Task 3).
- Produces: `object GoalCodec { fun encode(goals: List<Goal>): String; fun decode(raw: String): List<Goal> }`; `object GoalListOps { fun upsert(list, goal): List<Goal>; fun remove(list, id): List<Goal>; fun setCheckIn(list, id, date, done, today): List<Goal>; fun toggle(list, id, date, today): List<Goal> }`; `class GoalRepository(context) { val goals: Flow<List<Goal>>; suspend fun current(); suspend fun save(goal); suspend fun delete(id); suspend fun toggle(id, date, today = LocalDate.now()); suspend fun setCheckIn(id, date, done, today = LocalDate.now()) }`.

- [ ] **Step 1: Write failing tests**

`GoalCodecTest.kt`:

```kotlin
package com.omnidot.goals

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoalCodecTest {
    private val goal = Goal("g1", "Run 5 km", "🏃", GoalTier.BIG, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
        setOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5)), archived = false)

    @Test
    fun `round trip preserves every field`() {
        val open = goal.copy(id = "g2", endDate = null, archived = true, checkIns = emptySet())
        assertEquals(listOf(goal, open), GoalCodec.decode(GoalCodec.encode(listOf(goal, open))))
    }

    @Test
    fun `blank or garbage input yields empty list`() {
        assertEquals(emptyList<Goal>(), GoalCodec.decode(""))
        assertEquals(emptyList<Goal>(), GoalCodec.decode("not json"))
    }

    @Test
    fun `bad entries are dropped while good ones survive`() {
        val raw = """[
          {"id":"bad","title":"x","tier":"BIG","start":"not-a-date"},
          {"id":"","title":"no id","start":"2026-10-01"},
          {"id":"ok","title":"Read","tier":"WHAT","start":"2026-10-01","checkIns":["2026-10-02","oops"]}
        ]"""
        val decoded = GoalCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("ok", decoded[0].id)
        assertEquals(GoalTier.SMALL, decoded[0].tier)
        assertEquals("🌱", decoded[0].emoji)
        assertEquals(setOf(LocalDate.of(2026, 10, 2)), decoded[0].checkIns)
    }
}
```

`GoalListOpsTest.kt`:

```kotlin
package com.omnidot.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalListOpsTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val a = Goal("a", "A", "🌱", GoalTier.SMALL, LocalDate.of(2026, 10, 1), null, emptySet())
    private val b = a.copy(id = "b", title = "B")

    @Test
    fun `upsert replaces in place and appends new`() {
        val updated = GoalListOps.upsert(listOf(a, b), a.copy(title = "A2"))
        assertEquals(listOf("A2", "B"), updated.map { it.title })
        assertEquals(3, GoalListOps.upsert(updated, a.copy(id = "c")).size)
    }

    @Test
    fun `check-in for a missing or archived goal is a no-op`() {
        val list = listOf(a, b.copy(archived = true))
        assertEquals(list, GoalListOps.setCheckIn(list, "gone", today, true, today))
        assertEquals(list, GoalListOps.setCheckIn(list, "b", today, true, today))
    }

    @Test
    fun `set check-in and toggle update only the target goal`() {
        val done = GoalListOps.setCheckIn(listOf(a, b), "a", today, true, today)
        assertTrue(today in done[0].checkIns)
        assertTrue(done[1].checkIns.isEmpty())
        assertTrue(GoalListOps.toggle(done, "a", today, today)[0].checkIns.isEmpty())
        assertEquals(listOf(b), GoalListOps.remove(done, "a"))
    }
}
```

- [ ] **Step 2: Run** — `./gradlew testDebugUnitTest -q` → FAIL: unresolved `GoalCodec`, `GoalListOps`.

- [ ] **Step 3: Implement**

`goals/GoalCodec.kt`:

```kotlin
package com.omnidot.goals

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object GoalCodec {
    fun encode(goals: List<Goal>): String = JSONArray().apply {
        goals.forEach { g ->
            put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("emoji", g.emoji)
                put("tier", g.tier.name)
                put("start", g.startDate.toString())
                g.endDate?.let { put("end", it.toString()) }
                put("checkIns", JSONArray(g.checkIns.sorted().map { it.toString() }))
                put("archived", g.archived)
            })
        }
    }.toString()

    fun decode(raw: String): List<Goal> {
        if (raw.isBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching { decodeOne(array.getJSONObject(index)) }.getOrNull()
        }
    }

    private fun decodeOne(item: JSONObject): Goal? {
        val id = item.optString("id")
        if (id.isBlank()) return null
        val tier = GoalTier.fromStored(item.optString("tier"))
        val checks = item.optJSONArray("checkIns")
        return Goal(
            id = id,
            title = item.optString("title").ifBlank { "Goal" },
            emoji = item.optString("emoji").ifBlank { tier.defaultEmoji },
            tier = tier,
            startDate = LocalDate.parse(item.getString("start")),
            endDate = item.optString("end").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            checkIns = if (checks == null) emptySet() else (0 until checks.length())
                .mapNotNull { runCatching { LocalDate.parse(checks.getString(it)) }.getOrNull() }
                .toSet(),
            archived = item.optBoolean("archived", false),
        )
    }
}
```

`goals/GoalListOps.kt`:

```kotlin
package com.omnidot.goals

import java.time.LocalDate

object GoalListOps {
    fun upsert(list: List<Goal>, goal: Goal): List<Goal> =
        if (list.any { it.id == goal.id }) list.map { if (it.id == goal.id) goal else it } else list + goal

    fun remove(list: List<Goal>, id: String): List<Goal> = list.filterNot { it.id == id }

    fun setCheckIn(list: List<Goal>, id: String, date: LocalDate, done: Boolean, today: LocalDate): List<Goal> =
        list.map { if (it.id == id) it.withCheckIn(date, done, today) else it }

    fun toggle(list: List<Goal>, id: String, date: LocalDate, today: LocalDate): List<Goal> =
        list.map { if (it.id == id) it.toggle(date, today) else it }
}
```

`goals/GoalRepository.kt`:

```kotlin
package com.omnidot.goals

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.goalDataStore by preferencesDataStore(name = "omnidot_goals")

class GoalRepository(private val context: Context) {
    private val key = stringPreferencesKey("goals_json")

    val goals: Flow<List<Goal>> = context.goalDataStore.data.map { GoalCodec.decode(it[key].orEmpty()) }

    suspend fun current(): List<Goal> = goals.first()
    suspend fun save(goal: Goal) = update { GoalListOps.upsert(it, goal) }
    suspend fun delete(id: String) = update { GoalListOps.remove(it, id) }
    suspend fun toggle(id: String, date: LocalDate, today: LocalDate = LocalDate.now()) =
        update { GoalListOps.toggle(it, id, date, today) }
    suspend fun setCheckIn(id: String, date: LocalDate, done: Boolean, today: LocalDate = LocalDate.now()) =
        update { GoalListOps.setCheckIn(it, id, date, done, today) }

    private suspend fun update(transform: (List<Goal>) -> List<Goal>) {
        context.goalDataStore.edit { it[key] = GoalCodec.encode(transform(GoalCodec.decode(it[key].orEmpty()))) }
    }
}
```

- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/omnidot/goals app/src/test/java/com/omnidot/goals
git commit -m "feat(goals): persist goals with lenient JSON codec"
```

---

### Task 5: Goals lockscreen rendering

**Files:**
- Modify: `core/AdaptiveMatrixEngine.kt`
- Test: `test/.../core/AdaptiveMatrixEngineTest.kt` (geometry test)

**Interfaces:**
- Consumes: `GoalsMatrix.build`, `GoalDays.dayState`, `DayState`, `GoalTier`.
- Produces: `fun render(size: Size, settings: OmniDotPreferences, goals: List<Goal> = emptyList(), today: LocalDate = LocalDate.now()): Bitmap`; `suspend fun applyToLockscreen(context: Context, settings: OmniDotPreferences, goals: List<Goal>)`; `internal fun gridGeometry(width: Int, height: Int, settings: OmniDotPreferences, columns: Int, rows: Int): GridGeometry` with `data class GridGeometry(val left: Float, val top: Float, val cell: Float, val gap: Float, val adjustedTop: Float)`; `companion` constants `TIER_SMALL = 0xFF7BC47F.toInt()`, `TIER_MEDIUM = 0xFF6FA8DC.toInt()`, `TIER_BIG = 0xFFE0B65A.toInt()`, `ACCENT = 0xFF5FB3A6.toInt()`.

- [ ] **Step 1: Write failing test** (append to `AdaptiveMatrixEngineTest`)

```kotlin
@Test
fun `grid geometry centres the grid and fits inside the screen`() {
    val g = engine.gridGeometry(1080, 2400, OmniDotPreferences(), columns = 7, rows = 6)
    val gridWidth = 7 * g.cell + 6 * g.gap
    assertEquals(1080f, g.left * 2 + gridWidth, 0.5f)
    assertTrue(g.top > 2400 * 0.3f)
    assertTrue(g.top + 6 * g.cell + 5 * g.gap < 2400f)
}
```

(add `import org.junit.Assert.assertTrue`)

- [ ] **Step 2: Run** — FAIL: unresolved `gridGeometry`.

- [ ] **Step 3: Implement** — restructure `AdaptiveMatrixEngine`:

1. Extract the existing layout math (margin, aspect-ratio fractions, `adjustedTop`, `fittedCell`, scale, centring) into:

```kotlin
data class GridGeometry(val left: Float, val top: Float, val cell: Float, val gap: Float, val adjustedTop: Float)

internal fun gridGeometry(width: Int, height: Int, settings: OmniDotPreferences, columns: Int, rows: Int): GridGeometry {
    val margin = width * 0.075f
    val aspectRatio = height.toFloat() / width
    val topFraction = when { aspectRatio >= 2.15f -> .42f; aspectRatio >= 1.95f -> .40f; else -> .38f }
    val bottomFraction = if (aspectRatio >= 2.15f) .18f else .16f
    val adjustedTop = (topFraction + settings.verticalOffset).coerceIn(.18f, .62f)
    val top = height * adjustedTop
    val bottom = height * (1f - bottomFraction + settings.verticalOffset)
    val availableWidth = width - margin * 2
    val availableHeight = bottom - top
    val fitted = min(
        availableWidth / (columns + (columns - 1) * GAP_RATIO),
        availableHeight / (rows + (rows - 1) * GAP_RATIO),
    )
    val cell = fitted * settings.matrixScale
    val gap = cell * GAP_RATIO
    val gridWidth = columns * cell + (columns - 1) * gap
    val gridHeight = rows * cell + (rows - 1) * gap
    return GridGeometry((width - gridWidth) / 2f, top + (availableHeight - gridHeight) / 2f, cell, gap, adjustedTop)
}
```

Keep the existing explanatory comment about OEM lockscreen bounds above it. Make the Year/Life path in `render` use it.

2. New `render` signature and dispatch:

```kotlin
fun render(size: Size, settings: OmniDotPreferences, goals: List<Goal> = emptyList(), today: LocalDate = LocalDate.now()): Bitmap {
    require(size.width > 0 && size.height > 0) { "Bitmap dimensions must be positive" }
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(BACKGROUND)
    if (settings.preset == MatrixPreset.GOALS) drawGoals(canvas, size, settings, goals, today)
    else drawTimeMatrix(canvas, size, settings, today)   // the existing Year/Life body, unchanged apart from gridGeometry
    return bitmap
}
```

Remove the Task 2 temporary GOALS→YEAR mapping.

3. `drawGoals`:

```kotlin
private fun drawGoals(canvas: Canvas, size: Size, settings: OmniDotPreferences, goals: List<Goal>, today: LocalDate) {
    val matrix = GoalsMatrix.build(goals, settings.lockPeriod, today)
    val g = gridGeometry(size.width, size.height, settings, matrix.columns, matrix.rows)
    val title = titlePaint(size).apply { textSize = max(28f, size.width * .06f); letterSpacing = .02f }
    val detail = detailPaint(size)
    canvas.drawText(matrix.title, g.left, size.height * (g.adjustedTop - .055f), title)
    canvas.drawText(matrix.detail, g.left, size.height * (g.adjustedTop - .03f), detail)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1.5f, g.cell * .12f) }
    matrix.cells.forEachIndexed { index, date ->
        if (date == null) return@forEachIndexed
        val x = g.left + (index % matrix.columns) * (g.cell + g.gap)
        val y = g.top + (index / matrix.columns) * (g.cell + g.gap)
        val rect = RectF(x, y, x + g.cell, y + g.cell)
        when (val state = GoalDays.dayState(date, goals, today)) {
            is DayState.Achieved -> drawTierShape(canvas, rect, state.tier, fill)
            DayState.Missed -> { ring.color = MISSED_RING; canvas.drawOval(rect.inset(ring.strokeWidth), ring) }
            DayState.TodayPending -> { ring.color = ACCENT; canvas.drawOval(rect.inset(ring.strokeWidth), ring) }
            DayState.NoGoal, DayState.Future -> { fill.color = INACTIVE; canvas.drawOval(rect.inset(g.cell * .18f), fill) }
        }
    }
}

private fun drawTierShape(canvas: Canvas, rect: RectF, tier: GoalTier, paint: Paint) {
    when (tier) {
        GoalTier.SMALL -> { paint.color = TIER_SMALL; canvas.drawOval(rect, paint) }
        GoalTier.MEDIUM -> { paint.color = TIER_MEDIUM; val r = rect.width() * .24f; canvas.drawRoundRect(rect, r, r, paint) }
        GoalTier.BIG -> {
            paint.color = TIER_BIG
            val path = android.graphics.Path().apply {
                moveTo(rect.centerX(), rect.top); lineTo(rect.right, rect.centerY())
                lineTo(rect.centerX(), rect.bottom); lineTo(rect.left, rect.centerY()); close()
            }
            canvas.drawPath(path, paint)
        }
    }
}

private fun RectF.inset(by: Float) = RectF(left + by, top + by, right - by, bottom - by)
```

Extract the existing `titlePaint`/`detailPaint` builders into `private fun titlePaint(size: Size)` / `detailPaint(size: Size)` and reuse them in the Year/Life path.

4. Constants in `companion object` (keep `GAP_RATIO`, `BACKGROUND`, `INACTIVE`, `MUTED_TEXT`; replace `CURRENT_POSITION` with `ACCENT`):

```kotlin
const val ACCENT = 0xFF5FB3A6.toInt()
const val TIER_SMALL = 0xFF7BC47F.toInt()
const val TIER_MEDIUM = 0xFF6FA8DC.toInt()
const val TIER_BIG = 0xFFE0B65A.toInt()
private const val MISSED_RING = 0xFF3A3F45.toInt()
```

`BACKGROUND` becomes `0xFF121417.toInt()`.

5. `applyToLockscreen(context, settings, goals: List<Goal>)` passes goals to `render`.

Callers that still call the two-arg `applyToLockscreen` (`OmniDotViewModel.applyWallpaper`, `MatrixSyncWorker`, `AutomationEngine`) — add `emptyList()` temporarily; Task 6 replaces them.

- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "feat(lockscreen): render goals grid with tier shapes and day states"
```

---

### Task 6: Wallpaper refresh pipeline

**Files:**
- Create: `core/WallpaperRefresher.kt`
- Modify: `core/MatrixSyncWorker.kt`, `OmniDotViewModel.kt`, `automation/AutomationEngine.kt`

**Interfaces:**
- Consumes: `PreferencesManager`, `GoalRepository`, `AdaptiveMatrixEngine.applyToLockscreen(context, settings, goals)`.
- Produces: `object WallpaperRefresher { suspend fun apply(context: Context, settings: OmniDotPreferences? = null); suspend fun refreshIfEnabled(context: Context): Boolean }` — `apply` renders with current goals, sets `wallpaperEnabled = true`; `refreshIfEnabled` returns false without rendering when `wallpaperEnabled` is false.

- [ ] **Step 1: Implement** (Android glue; covered by device check in Task 17)

```kotlin
package com.omnidot.core

import android.content.Context
import com.omnidot.data.OmniDotPreferences
import com.omnidot.data.PreferencesManager
import com.omnidot.goals.GoalRepository

object WallpaperRefresher {
    suspend fun apply(context: Context, settings: OmniDotPreferences? = null) {
        val preferences = PreferencesManager(context)
        val resolved = settings ?: preferences.current()
        AdaptiveMatrixEngine().applyToLockscreen(context, resolved, GoalRepository(context).current())
        preferences.setWallpaperEnabled(true)
    }

    suspend fun refreshIfEnabled(context: Context): Boolean {
        val settings = PreferencesManager(context).current()
        if (!settings.wallpaperEnabled) return false
        apply(context, settings)
        return true
    }
}
```

`MatrixSyncWorker.doWork`: replace `AdaptiveMatrixEngine().applyToLockscreen(applicationContext, settings)` with `WallpaperRefresher.refreshIfEnabled(applicationContext)`.

`OmniDotViewModel.applyWallpaper`: replace the engine call with `WallpaperRefresher.apply(getApplication(), preferences.value)`.

`AutomationEngine`: in `APPLY_PRESET`, `REFRESH_CURRENT`, `CYCLE_PRESET`, `CYCLE_ACCENT` replace `AdaptiveMatrixEngine().applyToLockscreen(context, X)` with `WallpaperRefresher.apply(context, X)`. In `CYCLE_PRESET` the cycle naturally covers GOALS → YEAR → LIFE because it iterates `MatrixPreset.entries`. Update the `CYCLE_PRESET` subtitle in `AutomationRule.kt` to `"Rotate: Goals → Year → Life"` and `CYCLE_ACCENT`'s colour list's first entry to teal `0xFF5FB3A6.toInt()` (replacing Amber).

- [ ] **Step 2: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 3: Commit**

```bash
git add -A app/src
git commit -m "feat(lockscreen): central wallpaper refresher gated by wallpaperEnabled"
```

---

### Task 7: Evening check-in reminder

**Files:**
- Create: `goals/reminder/ReminderSchedule.kt`, `goals/reminder/CheckInNotifier.kt`, `goals/reminder/CheckInReminderWorker.kt`, `goals/reminder/CheckInReceiver.kt`
- Modify: `AndroidManifest.xml`, `core/BootReceiver.kt`
- Test: `test/.../goals/ReminderScheduleTest.kt`

**Interfaces:**
- Consumes: `GoalRepository.setCheckIn`, `WallpaperRefresher.refreshIfEnabled`, `PreferencesManager` reminder prefs.
- Produces: `object ReminderSchedule { fun delayUntilNext(now: ZonedDateTime, minuteOfDay: Int): Duration; fun pendingToday(goals: List<Goal>, today: LocalDate): List<Goal> }`; `object CheckInReminderWorker.Companion { fun schedule(context: Context, minuteOfDay: Int, now: ZonedDateTime = ZonedDateTime.now()); fun cancel(context: Context) }`; `CheckInReceiver` actions `com.omnidot.CHECKIN_DONE` / `com.omnidot.CHECKIN_SKIP` with extras `goal_id`, `epoch_day`; `MainActivity` extra `open_tab` = `"goals"`.

- [ ] **Step 1: Write failing test** — `ReminderScheduleTest.kt`

```kotlin
package com.omnidot.goals

import com.omnidot.goals.reminder.ReminderSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `later today schedules today`() {
        val now = ZonedDateTime.of(2026, 10, 12, 18, 0, 0, 0, zone)
        assertEquals(Duration.ofHours(3), ReminderSchedule.delayUntilNext(now, 21 * 60))
    }

    @Test
    fun `passed or exact time schedules tomorrow`() {
        val now = ZonedDateTime.of(2026, 10, 12, 21, 0, 0, 0, zone)
        assertEquals(Duration.ofHours(24), ReminderSchedule.delayUntilNext(now, 21 * 60))
        val late = ZonedDateTime.of(2026, 10, 12, 22, 30, 0, 0, zone)
        assertEquals(Duration.ofMinutes(22 * 60 + 30), ReminderSchedule.delayUntilNext(late, 21 * 60))
    }

    @Test
    fun `dst spring forward day is 23 hours long`() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 21, 0, 0, 0, ny) // DST starts 2026-03-08
        assertEquals(Duration.ofHours(23), ReminderSchedule.delayUntilNext(now, 21 * 60))
    }

    @Test
    fun `pending lists active unchecked goals only`() {
        val today = LocalDate.of(2026, 10, 12)
        val base = Goal("a", "A", "🌱", GoalTier.SMALL, LocalDate.of(2026, 10, 1), null, emptySet())
        val goals = listOf(
            base,
            base.copy(id = "done", checkIns = setOf(today)),
            base.copy(id = "future", startDate = today.plusDays(1)),
            base.copy(id = "arch", archived = true),
        )
        assertEquals(listOf("a"), ReminderSchedule.pendingToday(goals, today).map { it.id })
    }
}
```

- [ ] **Step 2: Run** — FAIL: unresolved `ReminderSchedule`.

- [ ] **Step 3: Implement**

`ReminderSchedule.kt`:

```kotlin
package com.omnidot.goals.reminder

import com.omnidot.goals.Goal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderSchedule {
    fun delayUntilNext(now: ZonedDateTime, minuteOfDay: Int): Duration {
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        var next = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!next.isAfter(now)) next = now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
        return Duration.between(now, next)
    }

    fun pendingToday(goals: List<Goal>, today: LocalDate): List<Goal> =
        goals.filter { it.activeOn(today) && today !in it.checkIns }
}
```

`CheckInNotifier.kt`:

```kotlin
package com.omnidot.goals.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.omnidot.MainActivity
import com.omnidot.R
import com.omnidot.goals.Goal
import java.time.LocalDate

object CheckInNotifier {
    const val CHANNEL_ID = "goal_checkins"
    const val NOTIFICATION_ID = 4201

    fun show(context: Context, pending: List<Goal>, today: LocalDate) {
        if (pending.isEmpty()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Goal check-ins", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Evening reminder to mark today's goals" },
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_TAB, "goals")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(open)
            .setAutoCancel(true)
        if (pending.size == 1) {
            val goal = pending.single()
            builder.setContentTitle("${goal.emoji} Did you achieve it today?")
                .setContentText(goal.title)
                .addAction(0, "Done", action(context, CheckInReceiver.ACTION_DONE, goal.id, today, 1))
                .addAction(0, "Not today", action(context, CheckInReceiver.ACTION_SKIP, goal.id, today, 2))
        } else {
            builder.setContentTitle("${pending.size} goals to check in")
                .setContentText(pending.joinToString(" · ") { it.title })
        }
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun action(context: Context, action: String, goalId: String, day: LocalDate, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, CheckInReceiver::class.java).setAction(action)
                .putExtra(CheckInReceiver.EXTRA_GOAL_ID, goalId)
                .putExtra(CheckInReceiver.EXTRA_EPOCH_DAY, day.toEpochDay()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
```

`CheckInReminderWorker.kt`:

```kotlin
package com.omnidot.goals.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.omnidot.data.PreferencesManager
import com.omnidot.goals.GoalRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class CheckInReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings = PreferencesManager(applicationContext).current()
        if (!settings.reminderEnabled) return Result.success()
        schedule(applicationContext, settings.reminderMinuteOfDay)
        val today = LocalDate.now()
        CheckInNotifier.show(applicationContext, ReminderSchedule.pendingToday(GoalRepository(applicationContext).current(), today), today)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "omnidot_checkin_reminder"

        fun schedule(context: Context, minuteOfDay: Int, now: ZonedDateTime = ZonedDateTime.now()) {
            val delay = ReminderSchedule.delayUntilNext(now, minuteOfDay)
            val request = OneTimeWorkRequestBuilder<CheckInReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }
}
```

`CheckInReceiver.kt`:

```kotlin
package com.omnidot.goals.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omnidot.core.WallpaperRefresher
import com.omnidot.goals.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        context.getSystemService(NotificationManager::class.java).cancel(CheckInNotifier.NOTIFICATION_ID)
        if (action != ACTION_DONE) return
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val day = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_EPOCH_DAY, LocalDate.now().toEpochDay()))
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                GoalRepository(context).setCheckIn(goalId, day, done = true)
                runCatching { WallpaperRefresher.refreshIfEnabled(context) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.omnidot.CHECKIN_DONE"
        const val ACTION_SKIP = "com.omnidot.CHECKIN_SKIP"
        const val EXTRA_GOAL_ID = "goal_id"
        const val EXTRA_EPOCH_DAY = "epoch_day"
    }
}
```

`MainActivity.kt` — add `companion object { const val EXTRA_OPEN_TAB = "open_tab" }` (consumed in Task 12).

Manifest — inside `<application>`:

```xml
<receiver android:name=".goals.reminder.CheckInReceiver" android:exported="false" />
```

`BootReceiver.onReceive` — after `MatrixSyncWorker.scheduleNext(context)`, reschedule reminder:

```kotlin
val pending = goAsync()
CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
    try {
        val settings = PreferencesManager(context).current()
        if (settings.reminderEnabled) CheckInReminderWorker.schedule(context, settings.reminderMinuteOfDay)
    } finally { pending.finish() }
}
```

(inside the same `when` branch; add imports.)

- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "feat(goals): evening check-in reminder with Done / Not today actions"
```

---

### Task 8: Automation honesty and calm defaults

**Files:**
- Create: `OmniDotApplication.kt`
- Modify: `automation/AutomationRule.kt`, `automation/AutomationRepository.kt`, `automation/AutomationReceiver.kt`, `OmniDotViewModel.kt`, `AndroidManifest.xml`
- Test: `test/.../automation/AutomationRuleTest.kt`

**Interfaces:**
- Produces: `AutomationTrigger.available: Boolean`, `AutomationTrigger.needsAppAlive: Boolean`, `AutomationTrigger.pickerOptions: List<AutomationTrigger>`; `AutomationRule.createDefaultChargerConnectedRule()` / `…Disconnected…` calm; `AutomationRepository.ensureDefaultRules()` seeds only once; `AutomationReceiver.Companion.deviceEventFilter(): IntentFilter`.

- [ ] **Step 1: Update tests** in `AutomationRuleTest.kt` — replace the energetic/dramatic test with:

```kotlin
@Test
fun `default charger rules are calm`() {
    val connected = AutomationRule.createDefaultChargerConnectedRule()
    assertEquals(AutomationTrigger.POWER_CONNECTED, connected.trigger)
    assertEquals("Charging started", connected.targetMessage)
    assertEquals(VoiceStyle.CALM.pitch, connected.speechPitch)
    assertEquals(VoiceStyle.CALM.rate, connected.speechRate)
    val disconnected = AutomationRule.createDefaultChargerDisconnectedRule()
    assertEquals("Charger disconnected", disconnected.targetMessage)
    assertEquals(VoiceStyle.CALM.rate, disconnected.speechRate)
}

@Test
fun `hidden triggers are excluded from the picker`() {
    val hidden = setOf(AutomationTrigger.SCHEDULED_TIME, AutomationTrigger.WEATHER_RAIN, AutomationTrigger.DAILY_MIDNIGHT)
    hidden.forEach { assertTrue(!it.available && it !in AutomationTrigger.pickerOptions) }
    assertTrue(AutomationTrigger.POWER_CONNECTED in AutomationTrigger.pickerOptions)
}

@Test
fun `rule defaults carry no personal data`() {
    val rule = AutomationRule(name = "x", trigger = AutomationTrigger.POWER_CONNECTED, action = AutomationAction.VIBRATE)
    assertEquals("", rule.contactName)
    assertEquals("", rule.targetMessage)
}
```

- [ ] **Step 2: Run** — FAIL (values differ / unresolved `available`).

- [ ] **Step 3: Implement**

`AutomationTrigger`: add constructor params `val available: Boolean = true, val needsAppAlive: Boolean = false`. Set `available = false` on `WEATHER_RAIN`, `SCHEDULED_TIME`, `DAILY_MIDNIGHT`. Set `needsAppAlive = true` on `POWER_CONNECTED`, `POWER_DISCONNECTED`, `BATTERY_LOW`, `BATTERY_OKAY`, `SCREEN_UNLOCKED`, `HEADSET_PLUG`, `WIFI_CONNECTED`, `WIFI_DISCONNECTED`. Fix the WhatsApp subtitle to `"When a message arrives from a chosen contact"`. Companion:

```kotlin
val pickerOptions: List<AutomationTrigger> = entries.filter { it.available }
```

`AutomationAction.SPEAK_TEXT.subtitle` → `"Speak your text aloud in a calm voice"`.

`AutomationRule` defaults: `contactName = ""`, `targetMessage = ""`, `speechPitch = VoiceStyle.CALM.pitch`, `speechRate = VoiceStyle.CALM.rate`. Default factories:

```kotlin
fun createDefaultChargerConnectedRule(): AutomationRule = AutomationRule(
    id = "default_charger_connected",
    name = "When charger connects → speak",
    trigger = AutomationTrigger.POWER_CONNECTED,
    action = AutomationAction.SPEAK_TEXT,
    targetMessage = "Charging started",
)

fun createDefaultChargerDisconnectedRule(): AutomationRule = AutomationRule(
    id = "default_charger_disconnected",
    name = "When charger disconnects → speak",
    trigger = AutomationTrigger.POWER_DISCONNECTED,
    action = AutomationAction.SPEAK_TEXT,
    targetMessage = "Charger disconnected",
)
```

`AutomationRepository`:
- add `private val seededKey = booleanPreferencesKey("defaults_seeded")`;
- `ensureDefaultRules()` becomes:

```kotlin
suspend fun ensureDefaultRules() {
    context.automationDataStore.edit { values ->
        if (values[seededKey] == true) return@edit
        if (decode(values[rulesKey].orEmpty()).isEmpty()) values[rulesKey] = encode(defaultRules())
        values[seededKey] = true
    }
}
```

- `decode` must no longer substitute defaults: return `decoded` (and `emptyList()` on failure) instead of `defaultRules()`.
- decoder field defaults: `contactName` → `""`, `targetMessage` → `""`.

`AutomationReceiver`: remove the `ensureDefaultRules()` re-seeding block. Add:

```kotlin
companion object {
    fun deviceEventFilter(): IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_BATTERY_OKAY)
        addAction(Intent.ACTION_USER_PRESENT)
        addAction(Intent.ACTION_HEADSET_PLUG)
        @Suppress("DEPRECATION") addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
    }
}
```

and skip rules whose trigger is not `available`: `repository.current().filter { it.enabled && it.trigger == trigger && it.trigger.available }`.

`OmniDotApplication.kt`:

```kotlin
package com.omnidot

import android.app.Application
import androidx.core.content.ContextCompat
import com.omnidot.automation.AutomationReceiver

class OmniDotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Android 8+ no longer delivers these implicit broadcasts to manifest receivers;
        // a runtime registration receives them for as long as the app process is alive.
        ContextCompat.registerReceiver(this, AutomationReceiver(), AutomationReceiver.deviceEventFilter(), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
```

Manifest: add `android:name=".OmniDotApplication"` on `<application>`; reduce the `AutomationReceiver` intent-filter to `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` (add it — `resolveTrigger` already maps it) and `com.omnidot.TRIGGER_ACTION`.

`OmniDotViewModel.setAutomationEnabled`: ignore enabling rules whose trigger is unavailable:

```kotlin
fun setAutomationEnabled(id: String, enabled: Boolean) = viewModelScope.launch {
    val rule = automations.value.firstOrNull { it.id == id } ?: return@launch
    if (enabled && !rule.trigger.available) return@launch
    automationRepository.setEnabled(id, enabled)
}
```

- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "fix(automation): register device events at runtime, hide dead triggers, calm defaults seeded once"
```

---

### Task 9: Remote trigger key

**Files:**
- Create: `automation/RemoteTriggerGuard.kt`
- Modify: `data/PreferencesManager.kt` (`remoteKey()` generation, `regenerateRemoteKey()`), `automation/AutomationReceiver.kt`, `MainActivity.kt`
- Test: `test/.../automation/RemoteTriggerGuardTest.kt`

**Interfaces:**
- Produces: `object RemoteTriggerGuard { fun newKey(random: java.util.Random = SecureRandom()): String; fun isAuthorized(provided: String?, expected: String): Boolean }`; `PreferencesManager.remoteKey(): String` (creates on first call); `PreferencesManager.regenerateRemoteKey(): String`.

- [ ] **Step 1: Write failing test**

```kotlin
package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteTriggerGuardTest {
    @Test
    fun `keys are 24 url-safe characters and differ`() {
        val a = RemoteTriggerGuard.newKey()
        assertEquals(24, a.length)
        assertTrue(a.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertNotEquals(a, RemoteTriggerGuard.newKey())
    }

    @Test
    fun `authorization requires an exact non-blank match`() {
        assertTrue(RemoteTriggerGuard.isAuthorized("abc123", "abc123"))
        assertFalse(RemoteTriggerGuard.isAuthorized("abc124", "abc123"))
        assertFalse(RemoteTriggerGuard.isAuthorized(null, "abc123"))
        assertFalse(RemoteTriggerGuard.isAuthorized("", ""))
    }
}
```

- [ ] **Step 2: Run** — FAIL: unresolved `RemoteTriggerGuard`.

- [ ] **Step 3: Implement**

```kotlin
package com.omnidot.automation

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object RemoteTriggerGuard {
    fun newKey(random: java.util.Random = SecureRandom()): String {
        val bytes = ByteArray(18).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isAuthorized(provided: String?, expected: String): Boolean {
        if (provided.isNullOrBlank() || expected.isBlank()) return false
        return MessageDigest.isEqual(provided.toByteArray(), expected.toByteArray())
    }
}
```

`PreferencesManager`:

```kotlin
suspend fun remoteKey(): String {
    var key = ""
    context.omniDotDataStore.edit { values ->
        key = values[Keys.remoteKey] ?: RemoteTriggerGuard.newKey().also { values[Keys.remoteKey] = it }
    }
    return key
}

suspend fun regenerateRemoteKey(): String {
    val key = RemoteTriggerGuard.newKey()
    edit { it[Keys.remoteKey] = key }
    return key
}
```

`AutomationReceiver.onReceive` — inside the coroutine, before loading rules:

```kotlin
if (intent?.action == ACTION_REMOTE_TRIGGER &&
    !RemoteTriggerGuard.isAuthorized(intent.getStringExtra("key"), PreferencesManager(context).remoteKey())
) return@launch
```

(wrap so `pendingResult.finish()` still runs — it is in `finally`). Add `const val ACTION_REMOTE_TRIGGER = "com.omnidot.TRIGGER_ACTION"` to the companion and use it in `resolveTrigger`.

`MainActivity.handleDeepLink` — after the scheme check:

```kotlin
val expected = PreferencesManager(this@MainActivity).remoteKey()
if (!RemoteTriggerGuard.isAuthorized(uri.getQueryParameter("key"), expected)) return@launch
```

and drop the success/failure toast — remote runs are recorded silently (no Toast import needed afterwards).

- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "fix(security): require per-install key for remote triggers and deep links"
```

---

### Task 10: Calm voice and soft bell

**Files:**
- Create: `automation/BellTone.kt`
- Modify: `automation/SoundSynthesizer.kt`, `automation/VoiceFeedbackManager.kt`, `automation/AutomationEngine.kt`
- Test: `test/.../automation/BellToneTest.kt`

**Interfaces:**
- Produces: `object BellTone { const val DURATION_SEC = 2.4; fun generate(sampleRate: Int = 44_100): ShortArray }`; `SoundSynthesizer.playSoftBell()`; `VoiceFeedbackManager.speak(context, text, pitch, rate, volume = 0.8f, waitForCompletion = false)`; `VoiceFeedbackManager.timeoutFor(text: String): Long`.

- [ ] **Step 1: Write failing test**

```kotlin
package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class BellToneTest {
    private fun rms(samples: List<Short>) = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)

    @Test
    fun `bell is soft, decays, and never clips`() {
        val pcm = BellTone.generate(44_100)
        assertEquals((2.4 * 44_100).toInt(), pcm.size)
        assertTrue(pcm.maxOf { abs(it.toInt()) } <= (0.55 * Short.MAX_VALUE).toInt())
        val tenth = pcm.size / 10
        assertTrue(rms(pcm.take(tenth)) > 8 * rms(pcm.takeLast(tenth)))
    }

    @Test
    fun `speech timeout scales with text and is bounded`() {
        assertEquals(4_000L, VoiceFeedbackManager.timeoutFor("Hi"))
        assertTrue(VoiceFeedbackManager.timeoutFor("x".repeat(200)) > 4_000L)
        assertEquals(60_000L, VoiceFeedbackManager.timeoutFor("x".repeat(5_000)))
    }
}
```

- [ ] **Step 2: Run** — FAIL: unresolved `BellTone`, `timeoutFor`.

- [ ] **Step 3: Implement**

`BellTone.kt`:

```kotlin
package com.omnidot.automation

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** A low, slowly fading singing-bowl style tone (inharmonic partials, long exponential decay). */
object BellTone {
    const val DURATION_SEC = 2.4
    private const val FUNDAMENTAL = 392.0 // G4
    private val partials = listOf(1.0 to 1.0, 2.0 to 0.35, 2.76 to 0.15)
    private const val PEAK = 0.5

    fun generate(sampleRate: Int = 44_100): ShortArray {
        val total = (DURATION_SEC * sampleRate).toInt()
        val attackSamples = (0.015 * sampleRate).toInt()
        val norm = partials.sumOf { it.second }
        return ShortArray(total) { i ->
            val t = i.toDouble() / sampleRate
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val envelope = attack * exp(-t * 2.2)
            val wave = partials.sumOf { (ratio, amp) -> amp * sin(2 * PI * FUNDAMENTAL * ratio * t) } / norm
            (wave * envelope * PEAK * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
```

`SoundSynthesizer.kt`: delete `playPowerConnectedChime`, `playPowerDisconnectedChime`, `Note`, `generateArpeggio`. Add:

```kotlin
fun playSoftBell() {
    scope.launch { playPcm(44_100, BellTone.generate(44_100)) }
}
```

Keep `playPcm`; lower its attribute usage to `USAGE_ASSISTANCE_SONIFICATION` (unchanged) — no other change.

`VoiceFeedbackManager.speak`: add `volume: Float = 0.8f` param; use `putFloat(KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))`; switch `QUEUE_FLUSH` → `QUEUE_ADD`; replace `withTimeoutOrNull(4000L)` with `withTimeoutOrNull(timeoutFor(text))`; add:

```kotlin
fun timeoutFor(text: String): Long = (2_000L + text.length * 90L).coerceIn(4_000L, 60_000L)
```

`AutomationEngine.SPEAK_TEXT`:

```kotlin
AutomationAction.SPEAK_TEXT -> {
    val textToSpeak = resolveVariables(rule.targetMessage.ifBlank { rule.message }, context, rule)
    if (rule.playChime) {
        SoundSynthesizer.playSoftBell()
        kotlinx.coroutines.delay(700)
    }
    VoiceFeedbackManager.speak(
        context = context,
        text = textToSpeak,
        pitch = rule.speechPitch,
        rate = rule.speechRate,
        volume = currentPrefs.voiceStyle.volume,
        waitForCompletion = true,
    )
    "Spoke \"$textToSpeak\""
}
```

In `CALL_PHONE` pass `volume = currentPrefs.voiceStyle.volume`. In the old `ui/AutomationScreen.kt` preview button, replace the two chime calls with `SoundSynthesizer.playSoftBell()` so it compiles (the file is replaced in Task 14).

- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "feat(voice): calm defaults, no speech cut-off, soft bell instead of arpeggios"
```

---

### Task 11: Calm theme and shared components

**Files:**
- Modify: `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `res/values/themes.xml`
- Create: `res/values-night/themes.xml`, `ui/components/Common.kt`

**Interfaces:**
- Produces colour tokens `Teal`, `TealDeep`, `TierSmall`, `TierMedium`, `TierBig`; `OmniDotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)`; composables `SectionHeader(text: String, modifier: Modifier = Modifier)`, `SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`, `PhonePreview(bitmap: ImageBitmap, modifier: Modifier = Modifier)`.
- Keeps the old tokens (`OledBlack`, `SurfaceDark`, `OmniOrange`, `DotInactive`, `PureWhite`, `MutedText`) until Task 17 so old screens compile.

- [ ] **Step 1: Implement**

Append to `Color.kt`:

```kotlin
val Teal = Color(0xFF5FB3A6)
val TealDeep = Color(0xFF2E7D72)
val TierSmall = Color(0xFF7BC47F)
val TierMedium = Color(0xFF6FA8DC)
val TierBig = Color(0xFFE0B65A)

internal val DarkBackground = Color(0xFF121417)
internal val DarkSurface = Color(0xFF1B1E22)
internal val DarkSurfaceHigh = Color(0xFF24282D)
internal val DarkOutline = Color(0xFF2E3338)
internal val DarkMuted = Color(0xFF9AA3AB)
internal val LightBackground = Color(0xFFF6F5F2)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceHigh = Color(0xFFEFEEEA)
internal val LightOutline = Color(0xFFDEDCD6)
internal val LightMuted = Color(0xFF5F676E)
```

`Theme.kt`:

```kotlin
package com.omnidot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Teal, onPrimary = Color(0xFF0B1F1C),
    primaryContainer = Color(0xFF1F3A36), onPrimaryContainer = Color(0xFFCFEDE7),
    background = DarkBackground, onBackground = Color(0xFFE7EAEC),
    surface = DarkSurface, onSurface = Color(0xFFE7EAEC),
    surfaceVariant = DarkSurfaceHigh, onSurfaceVariant = DarkMuted,
    surfaceContainer = DarkSurface, surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline, outlineVariant = DarkOutline,
    error = Color(0xFFE5867A),
)

private val LightColors = lightColorScheme(
    primary = TealDeep, onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EEEA), onPrimaryContainer = Color(0xFF0E3B35),
    background = LightBackground, onBackground = Color(0xFF1B1F22),
    surface = LightSurface, onSurface = Color(0xFF1B1F22),
    surfaceVariant = LightSurfaceHigh, onSurfaceVariant = LightMuted,
    surfaceContainer = LightSurface, surfaceContainerHigh = LightSurfaceHigh,
    outline = LightOutline, outlineVariant = LightOutline,
    error = Color(0xFFB3261E),
)

@Composable
fun OmniDotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
```

`res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.OmniDot" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">#F6F5F2</item>
        <item name="android:windowActionModeOverlay">true</item>
    </style>
</resources>
```

`res/values-night/themes.xml`:

```xml
<resources>
    <style name="Theme.OmniDot" parent="android:style/Theme.Material.NoActionBar">
        <item name="android:windowBackground">#121417</item>
        <item name="android:windowActionModeOverlay">true</item>
    </style>
</resources>
```

(`enableEdgeToEdge()` in MainActivity already handles bar colours/contrast.)

`ui/components/Common.kt`:

```kotlin
package com.omnidot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun PhonePreview(bitmap: ImageBitmap, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.width(200.dp).height(416.dp).clip(RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
        ) {
            Image(bitmap, contentDescription = "Lock screen preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Text(
                "10:09",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 44.dp),
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}
```

- [ ] **Step 2: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 3: Commit**

```bash
git add -A app/src
git commit -m "feat(ui): calm light/dark theme and shared components"
```

---

### Task 12: App shell with bottom navigation

**Files:**
- Create: `ui/OmniDotApp.kt`
- Modify: `MainActivity.kt`

**Interfaces:**
- Consumes: existing `AutomationScreen`, `PreviewScreen`/`CustomizeScreen` (temporary), `OmniDotViewModel`.
- Produces: `enum class AppTab { WORKFLOWS, GOALS, SETTINGS }`; `@Composable fun OmniDotApp(initialTab: AppTab, workflows: @Composable (PaddingValues, SnackbarHostState, (Boolean) -> Unit) -> Unit, goals: @Composable (PaddingValues, SnackbarHostState) -> Unit, settings: @Composable (PaddingValues, SnackbarHostState) -> Unit)` — the `(Boolean) -> Unit` lets the Workflows tab hide the bottom bar while its full-screen editor is open.

- [ ] **Step 1: Implement** `ui/OmniDotApp.kt`

```kotlin
package com.omnidot.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val label: String, val icon: ImageVector) {
    WORKFLOWS("Workflows", Icons.Rounded.Bolt),
    GOALS("Goals", Icons.Rounded.Flag),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

@Composable
fun OmniDotApp(
    initialTab: AppTab,
    workflows: @Composable (PaddingValues, SnackbarHostState, (Boolean) -> Unit) -> Unit,
    goals: @Composable (PaddingValues, SnackbarHostState) -> Unit,
    settings: @Composable (PaddingValues, SnackbarHostState) -> Unit,
) {
    var tab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var hideBar by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!hideBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            AppTab.WORKFLOWS -> workflows(padding, snackbar) { hideBar = it }
            AppTab.GOALS -> goals(padding, snackbar)
            AppTab.SETTINGS -> settings(padding, snackbar)
        }
    }
}
```

- [ ] **Step 2: Rewrite `MainActivity.onCreate` content** — no toasts; tabs host the old screens for now:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleDeepLink(intent)
    val initialTab = if (intent?.getStringExtra(EXTRA_OPEN_TAB) == "goals") AppTab.GOALS else AppTab.WORKFLOWS
    setContent {
        OmniDotTheme {
            OmniDotApp(
                initialTab = initialTab,
                workflows = { padding, _, _ ->
                    val automations by viewModel.automations.collectAsStateWithLifecycle()
                    val testStatus by viewModel.testStatus.collectAsStateWithLifecycle()
                    Box(Modifier.padding(padding)) {
                        AutomationScreen(
                            rules = automations, testStatus = testStatus, onBack = {},
                            onSave = viewModel::saveAutomation,
                            onEnabledChanged = viewModel::setAutomationEnabled,
                            onDelete = viewModel::deleteAutomation,
                            onTest = { viewModel.testAutomation(it) },
                            onClearTestStatus = viewModel::clearTestStatus,
                        )
                    }
                },
                goals = { padding, _ ->
                    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
                    val status by viewModel.applyStatus.collectAsStateWithLifecycle()
                    Box(Modifier.padding(padding)) {
                        PreviewScreen(preferences, status, viewModel::selectPreset, onOpenCustomize = {}, onOpenAutomations = {}, onApply = viewModel::applyWallpaper)
                    }
                },
                settings = { padding, _ -> Box(Modifier.padding(padding)) { Text("Settings") } },
            )
        }
    }
}
```

Add `onNewIntent` handling of `EXTRA_OPEN_TAB` by calling `setIntent(intent)` and `recreate()` only when the extra is present (simple and rare). Remove `showToast` and the `Toast` import.

- [ ] **Step 3: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS.

- [ ] **Step 4: Install and eyeball** — `./gradlew installDebug -q && adb shell am start -n com.omnidot/.MainActivity` → three tabs visible.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "feat(ui): bottom navigation shell with Workflows, Goals and Settings tabs"
```

---

### Task 13: Workflows list, cards, recipes, health banner

**Files:**
- Create: `automation/WorkflowText.kt`, `automation/WorkflowRecipes.kt`, `automation/HealthCheck.kt`, `ui/workflows/WorkflowIcons.kt`, `ui/workflows/WorkflowCard.kt`, `ui/workflows/WorkflowsScreen.kt`
- Modify: `MainActivity.kt` (use `WorkflowsScreen`), `OmniDotViewModel.kt` (`deleteAutomation` returns removed rule via callback)
- Test: `test/.../automation/WorkflowTextTest.kt`, `test/.../automation/WorkflowRecipesTest.kt`, `test/.../automation/HealthCheckTest.kt`

**Interfaces:**
- Produces:
  - `object WorkflowText { fun whenText(rule): String; fun doText(rule): String; fun name(rule): String; fun lastRun(rule, nowMillis: Long): LastRun; fun canSave(rule): Boolean }` with `data class LastRun(val text: String, val failed: Boolean)`.
  - `data class Recipe(val title: String, val description: String, val build: () -> AutomationRule)`; `object WorkflowRecipes { val all: List<Recipe> }`.
  - `enum class HealthIssue(val title: String, val detail: String) { NOTIFICATIONS_BLOCKED, LISTENER_MISSING, BATTERY_OPTIMIZED }`; `object HealthCheck { fun issues(notificationsGranted, listenerGranted, ignoringBatteryOptimizations, rules: List<AutomationRule>): List<HealthIssue>; fun probe(context): HealthProbe; fun fixIntent(context, issue): Intent? }`; `data class HealthProbe(val notificationsGranted: Boolean, val listenerGranted: Boolean, val ignoringBatteryOptimizations: Boolean)`.
  - `@Composable fun WorkflowsScreen(rules, padding, snackbar, onOpenEditor: (AutomationRule?) -> Unit, onEnabledChanged, onDelete: (AutomationRule) -> Unit, onRestore: (AutomationRule) -> Unit, onTest: (AutomationRule) -> Unit)`; `fun triggerIcon(t): ImageVector`, `fun actionIcon(a): ImageVector` (moved verbatim from `AutomationScreen.kt`).

- [ ] **Step 1: Write failing tests**

`WorkflowTextTest.kt`:

```kotlin
package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowTextTest {
    private fun rule(trigger: AutomationTrigger, action: AutomationAction, block: AutomationRule.() -> AutomationRule = { this }) =
        AutomationRule(name = "", trigger = trigger, action = action).block()

    @Test
    fun `sentences read naturally`() {
        val r = rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SPEAK_TEXT) { copy(targetMessage = "Charging started") }
        assertEquals("When charger connects", WorkflowText.whenText(r))
        assertEquals("Speak \"Charging started\"", WorkflowText.doText(r))
        assertEquals("When charger connects → speak \"Charging started\"", WorkflowText.name(r))
        val wa = rule(AutomationTrigger.WHATSAPP_MESSAGE, AutomationAction.HTTP_POST) { copy(url = "https://hooks.example.com/x") }
        assertEquals("When anyone messages on WhatsApp", WorkflowText.whenText(wa))
        assertEquals("Send webhook to hooks.example.com", WorkflowText.doText(wa))
        assertEquals("Send webhook to a URL", WorkflowText.doText(wa.copy(url = "not a url")))
    }

    @Test
    fun `last run text`() {
        val now = 10_000_000L
        val base = rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.VIBRATE)
        assertEquals(WorkflowText.LastRun("Not run yet", false), WorkflowText.lastRun(base, now))
        assertEquals(WorkflowText.LastRun("✓ Ran just now", false), WorkflowText.lastRun(base.copy(lastRunTime = now - 20_000, lastRunStatus = "ok"), now))
        assertEquals(WorkflowText.LastRun("✓ Ran 5 min ago", false), WorkflowText.lastRun(base.copy(lastRunTime = now - 300_000, lastRunStatus = "ok"), now))
        assertEquals(WorkflowText.LastRun("✕ boom", true), WorkflowText.lastRun(base.copy(lastRunTime = now - 1, lastRunStatus = "Error: boom"), now))
        assertEquals(WorkflowText.LastRun("✕ bad", true), WorkflowText.lastRun(base.copy(lastRunTime = now - 1, lastRunStatus = "✕ Failed: bad"), now))
    }

    @Test
    fun `can save requires message, contact and url where needed`() {
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SPEAK_TEXT)))
        assertTrue(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SPEAK_TEXT) { copy(targetMessage = "hi") }))
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SEND_WHATSAPP) { copy(targetMessage = "hi") }))
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.HTTP_POST)))
        assertTrue(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.VIBRATE)))
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.SCHEDULED_TIME, AutomationAction.VIBRATE)))
    }
}
```

`WorkflowRecipesTest.kt`:

```kotlin
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
```

`HealthCheckTest.kt`:

```kotlin
package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthCheckTest {
    private val speak = AutomationRule(name = "", trigger = AutomationTrigger.POWER_CONNECTED, action = AutomationAction.SPEAK_TEXT, targetMessage = "hi")
    private val notify = speak.copy(id = "n", action = AutomationAction.SHOW_NOTIFICATION)
    private val whatsapp = speak.copy(id = "w", trigger = AutomationTrigger.WHATSAPP_MESSAGE)

    @Test
    fun `no rules means no issues`() {
        assertEquals(emptyList<HealthIssue>(), HealthCheck.issues(false, false, false, emptyList()))
    }

    @Test
    fun `issues appear only when relevant to enabled rules`() {
        assertEquals(listOf(HealthIssue.BATTERY_OPTIMIZED), HealthCheck.issues(false, false, false, listOf(speak)))
        assertEquals(listOf(HealthIssue.NOTIFICATIONS_BLOCKED), HealthCheck.issues(false, true, true, listOf(notify)))
        assertEquals(listOf(HealthIssue.LISTENER_MISSING), HealthCheck.issues(true, false, true, listOf(whatsapp)))
        assertEquals(emptyList<HealthIssue>(), HealthCheck.issues(false, false, false, listOf(whatsapp.copy(enabled = false))))
    }
}
```

- [ ] **Step 2: Run** — FAIL: unresolved `WorkflowText`, `WorkflowRecipes`, `HealthCheck`.

- [ ] **Step 3: Implement pure logic**

`automation/WorkflowText.kt`:

```kotlin
package com.omnidot.automation

import java.net.URI

object WorkflowText {
    data class LastRun(val text: String, val failed: Boolean)

    fun whenText(rule: AutomationRule): String = when (rule.trigger) {
        AutomationTrigger.POWER_CONNECTED -> "When charger connects"
        AutomationTrigger.POWER_DISCONNECTED -> "When charger disconnects"
        AutomationTrigger.BATTERY_LOW -> "When battery drops below 20%"
        AutomationTrigger.BATTERY_OKAY -> "When battery is charged"
        AutomationTrigger.DEVICE_BOOT -> "When the phone restarts"
        AutomationTrigger.SCREEN_UNLOCKED -> "When you unlock the phone"
        AutomationTrigger.HEADSET_PLUG -> "When headphones connect"
        AutomationTrigger.WIFI_CONNECTED -> "When Wi-Fi connects"
        AutomationTrigger.WIFI_DISCONNECTED -> "When Wi-Fi disconnects"
        AutomationTrigger.WHATSAPP_MESSAGE -> "When ${rule.contactName.ifBlank { "anyone" }} messages on WhatsApp"
        AutomationTrigger.INBOUND_WEBHOOK -> "When a remote trigger arrives"
        AutomationTrigger.SCHEDULED_TIME -> "Every day at ${rule.scheduledTime}"
        AutomationTrigger.WEATHER_RAIN -> "When rain is forecast"
        AutomationTrigger.DAILY_MIDNIGHT -> "At midnight"
    }

    fun doText(rule: AutomationRule): String = when (rule.action) {
        AutomationAction.SPEAK_TEXT -> "Speak \"${rule.targetMessage}\""
        AutomationAction.SEND_WHATSAPP -> "Open WhatsApp to ${rule.contactName.ifBlank { "a contact" }}"
        AutomationAction.CALL_PHONE -> "Open dialer for ${rule.contactName.ifBlank { "a contact" }}"
        AutomationAction.APPLY_PRESET -> "Show ${rule.preset.title} on lock screen"
        AutomationAction.REFRESH_CURRENT -> "Refresh lock screen"
        AutomationAction.CYCLE_PRESET -> "Switch lock screen view"
        AutomationAction.CYCLE_ACCENT -> "Change dot colour"
        AutomationAction.HTTP_REQUEST -> "Call ${host(rule.url)}"
        AutomationAction.HTTP_POST -> "Send webhook to ${host(rule.url)}"
        AutomationAction.DISCORD_WEBHOOK -> "Post to Discord"
        AutomationAction.SLACK_WEBHOOK -> "Post to Slack"
        AutomationAction.SHOW_NOTIFICATION -> "Show notification \"${rule.message}\""
        AutomationAction.VIBRATE -> "Vibrate"
        AutomationAction.COPY_STATS -> "Copy progress to clipboard"
        AutomationAction.OPEN_URL -> "Open ${host(rule.url)}"
    }

    fun name(rule: AutomationRule): String =
        "${whenText(rule)} → ${doText(rule).replaceFirstChar { it.lowercase() }}"

    fun lastRun(rule: AutomationRule, nowMillis: Long): LastRun {
        if (rule.lastRunTime == 0L) return LastRun("Not run yet", false)
        val status = rule.lastRunStatus
        val failed = status.startsWith("Error") || status.startsWith("✕") || status.startsWith("Failed")
        if (failed) {
            val reason = status.removePrefix("✕").trim().removePrefix("Failed:").removePrefix("Error:").trim()
            return LastRun("✕ $reason", true)
        }
        val minutes = (nowMillis - rule.lastRunTime) / 60_000
        val ago = when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 24 * 60 -> "${minutes / 60} h ago"
            else -> "${minutes / (24 * 60)} d ago"
        }
        return LastRun("✓ Ran $ago", false)
    }

    fun canSave(rule: AutomationRule): Boolean {
        if (!rule.trigger.available) return false
        return when (rule.action) {
            AutomationAction.SPEAK_TEXT -> rule.targetMessage.isNotBlank()
            AutomationAction.SEND_WHATSAPP, AutomationAction.CALL_PHONE ->
                rule.targetMessage.isNotBlank() && rule.contactPhone.isNotBlank()
            AutomationAction.HTTP_REQUEST, AutomationAction.HTTP_POST, AutomationAction.DISCORD_WEBHOOK,
            AutomationAction.SLACK_WEBHOOK, AutomationAction.OPEN_URL ->
                rule.url.startsWith("http://") || rule.url.startsWith("https://")
            AutomationAction.SHOW_NOTIFICATION -> rule.message.isNotBlank()
            else -> true
        }
    }

    private fun host(url: String): String =
        runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: "a URL"
}
```

`automation/WorkflowRecipes.kt`:

```kotlin
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
}
```

`automation/HealthCheck.kt`:

```kotlin
package com.omnidot.automation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class HealthIssue(val title: String, val detail: String) {
    NOTIFICATIONS_BLOCKED("Notifications are off", "Workflows that show notifications can't alert you."),
    LISTENER_MISSING("WhatsApp access needed", "Allow notification access so WhatsApp workflows can run."),
    BATTERY_OPTIMIZED("Battery saver may pause workflows", "Let OmniDot run unrestricted so device events reach it."),
}

data class HealthProbe(val notificationsGranted: Boolean, val listenerGranted: Boolean, val ignoringBatteryOptimizations: Boolean)

object HealthCheck {
    fun issues(
        notificationsGranted: Boolean,
        listenerGranted: Boolean,
        ignoringBatteryOptimizations: Boolean,
        rules: List<AutomationRule>,
    ): List<HealthIssue> {
        val enabled = rules.filter { it.enabled }
        if (enabled.isEmpty()) return emptyList()
        return buildList {
            if (!notificationsGranted && enabled.any { it.action == AutomationAction.SHOW_NOTIFICATION }) add(HealthIssue.NOTIFICATIONS_BLOCKED)
            if (!listenerGranted && enabled.any { it.trigger == AutomationTrigger.WHATSAPP_MESSAGE }) add(HealthIssue.LISTENER_MISSING)
            if (!ignoringBatteryOptimizations) add(HealthIssue.BATTERY_OPTIMIZED)
        }
    }

    fun probe(context: Context): HealthProbe = HealthProbe(
        notificationsGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        listenerGranted = OmniNotificationListenerService.isNotificationAccessGranted(context),
        ignoringBatteryOptimizations = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
    )

    /** Settings screen that fixes [issue]. Notification permission on API 33+ is requested in-app instead. */
    fun fixIntent(context: Context, issue: HealthIssue): Intent = when (issue) {
        HealthIssue.NOTIFICATIONS_BLOCKED -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        HealthIssue.LISTENER_MISSING -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        HealthIssue.BATTERY_OPTIMIZED -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
```

- [ ] **Step 4: Run tests** — `./gradlew testDebugUnitTest -q` → PASS.

- [ ] **Step 5: Implement UI**

`ui/workflows/WorkflowIcons.kt`: move `triggerIcon` and `actionIcon` from `AutomationScreen.kt` (lines ~1492–1525) unchanged except `internal` visibility; delete them from `AutomationScreen.kt` and import from the new file there.

`ui/workflows/WorkflowCard.kt`:

```kotlin
package com.omnidot.ui.workflows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.WorkflowText

@Composable
fun WorkflowCard(
    rule: AutomationRule,
    nowMillis: Long,
    onEnabledChanged: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val lastRun = WorkflowText.lastRun(rule, nowMillis)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(triggerIcon(rule.trigger), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(WorkflowText.whenText(rule), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("→ ${WorkflowText.doText(rule)}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val note = when {
                    !rule.trigger.available -> "Not available yet"
                    rule.trigger.needsAppAlive -> "${lastRun.text} · works while OmniDot is running"
                    else -> lastRun.text
                }
                Text(note, style = MaterialTheme.typography.labelMedium,
                    color = if (lastRun.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = rule.enabled && rule.trigger.available, enabled = rule.trigger.available, onCheckedChange = onEnabledChanged)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Test now") }, onClick = { menu = false; onTest() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
```

`ui/workflows/WorkflowsScreen.kt`:

```kotlin
package com.omnidot.ui.workflows

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.HealthCheck
import com.omnidot.automation.HealthIssue
import com.omnidot.automation.WorkflowRecipes
import com.omnidot.ui.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkflowsScreen(
    rules: List<AutomationRule>,
    padding: PaddingValues,
    snackbar: SnackbarHostState,
    onOpenEditor: (AutomationRule?) -> Unit,
    onEnabledChanged: (String, Boolean) -> Unit,
    onDelete: (AutomationRule) -> Unit,
    onRestore: (AutomationRule) -> Unit,
    onTest: (AutomationRule) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var probe by remember { mutableStateOf(HealthCheck.probe(context)) }
    LifecycleResumeEffect(Unit) { probe = HealthCheck.probe(context); onPauseOrDispose { } }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        probe = HealthCheck.probe(context)
    }
    val issues = HealthCheck.issues(probe.notificationsGranted, probe.listenerGranted, probe.ignoringBatteryOptimizations, rules)

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Workflows", style = MaterialTheme.typography.headlineMedium)
                Text("Small automations that run on your phone.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            issues.firstOrNull()?.let { issue ->
                item {
                    HealthBanner(issue, extra = issues.size - 1) {
                        if (issue == HealthIssue.NOTIFICATIONS_BLOCKED && Build.VERSION.SDK_INT >= 33) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(HealthCheck.fixIntent(context, issue))
                        }
                    }
                }
            }
            if (rules.isEmpty()) {
                item { SectionHeader("Start with a recipe") }
                items(WorkflowRecipes.all, key = { it.title }) { recipe ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { onOpenEditor(recipe.build()) },
                        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                            Text(recipe.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    WorkflowCard(
                        rule = rule, nowMillis = now,
                        onEnabledChanged = { onEnabledChanged(rule.id, it) },
                        onEdit = { onOpenEditor(rule) },
                        onTest = { onTest(rule) },
                        onDelete = {
                            onDelete(rule)
                            scope.launch {
                                val result = snackbar.showSnackbar("Workflow deleted", actionLabel = "Undo", withDismissAction = true)
                                if (result == SnackbarResult.ActionPerformed) onRestore(rule)
                            }
                        },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onOpenEditor(null) },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("New workflow") },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun HealthBanner(issue: HealthIssue, extra: Int, onFix: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(issue.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(issue.detail + if (extra > 0) " (+$extra more in Settings)" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(0.dp))
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}
```

`OmniDotViewModel`: `deleteAutomation` unchanged; add `fun restoreAutomation(rule: AutomationRule) = saveAutomation(rule)`. `testAutomation` keeps writing `testStatus`; Workflows tab shows test results via snackbar instead (Step 6).

- [ ] **Step 6: Wire into `MainActivity`** — replace the temporary Workflows lambda (editor still old until Task 14):

```kotlin
workflows = { padding, snackbar, setHideBar ->
    val automations by viewModel.automations.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<AutomationRule?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    LaunchedEffect(editorOpen) { setHideBar(editorOpen) }
    val scope = rememberCoroutineScope()
    if (editorOpen) {
        Box(Modifier.padding(padding)) {
            AutomationScreen(
                rules = automations, testStatus = null, onBack = { editorOpen = false },
                onSave = { viewModel.saveAutomation(it); editorOpen = false },
                onEnabledChanged = viewModel::setAutomationEnabled, onDelete = viewModel::deleteAutomation,
                onTest = { viewModel.testAutomation(it) }, onClearTestStatus = {},
            )
        }
    } else {
        WorkflowsScreen(
            rules = automations, padding = padding, snackbar = snackbar,
            onOpenEditor = { editing = it; editorOpen = true },
            onEnabledChanged = viewModel::setAutomationEnabled,
            onDelete = { viewModel.deleteAutomation(it.id) },
            onRestore = viewModel::restoreAutomation,
            onTest = { rule -> viewModel.testAutomation(rule) { msg -> scope.launch { snackbar.showSnackbar(msg) } } },
        )
    }
}
```

- [ ] **Step 7: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS; `./gradlew installDebug -q` and open the Workflows tab: cards read as sentences, the FAB shows, deleting shows Undo.

- [ ] **Step 8: Commit**

```bash
git add -A app/src
git commit -m "feat(workflows): sentence cards, recipes, health banner and undo delete"
```

---

### Task 14: Full-screen workflow editor with searchable pickers

**Files:**
- Create: `ui/workflows/ChoiceSheet.kt`, `ui/workflows/WorkflowEditor.kt`
- Modify: `MainActivity.kt`
- Delete: `ui/AutomationScreen.kt`

**Interfaces:**
- Consumes: `WorkflowText.canSave/name`, `AutomationTrigger.pickerOptions`, `AutomationAction.workflowOptions`, `VoiceStyle`, `HealthCheck.probe`, icons.
- Produces: `@Composable fun WorkflowEditor(initial: AutomationRule?, defaultVoice: VoiceStyle, onClose: () -> Unit, onSave: (AutomationRule) -> Unit, onTest: (AutomationRule) -> Unit)`; `@Composable fun <T> ChoiceSheet(title: String, items: List<T>, categoryOf: (T) -> String, label: (T) -> String, subtitle: (T) -> String, icon: (T) -> ImageVector, locked: (T) -> Boolean, onUnlock: (T) -> Unit, onSelect: (T) -> Unit, onDismiss: () -> Unit)`.

- [ ] **Step 1: Implement `ChoiceSheet.kt`**

```kotlin
package com.omnidot.ui.workflows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceSheet(
    title: String,
    items: List<T>,
    categoryOf: (T) -> String,
    label: (T) -> String,
    subtitle: (T) -> String,
    icon: (T) -> ImageVector,
    locked: (T) -> Boolean,
    onUnlock: (T) -> Unit,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val categories = remember(items) { items.map(categoryOf).distinct() }
    val visible = items.filter { item ->
        (category == null || categoryOf(item) == category) &&
            (query.isBlank() || label(item).contains(query, true) || subtitle(item).contains(query, true))
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                categories.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) }
            }
            LazyColumn(Modifier.heightIn(max = 520.dp).padding(top = 8.dp)) {
                items(visible) { item ->
                    val isLocked = locked(item)
                    Row(
                        Modifier.fillMaxWidth().clickable { if (isLocked) onUnlock(item) else onSelect(item) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon(item), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label(item), style = MaterialTheme.typography.bodyLarge)
                            Text(subtitle(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isLocked) AssistChip(onClick = { onUnlock(item) }, label = { Text("Grant access") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) })
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Implement `WorkflowEditor.kt`** — full screen; state is a single `AutomationRule` copied on each edit; field sections port the existing logic from `AutomationScreen.kt` `WorkflowEditor` (contact picker launcher lines 433–477, URL samples, JSON payload, message template) with Material 3 defaults instead of orange colours:

```kotlin
package com.omnidot.ui.workflows

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnidot.automation.*
import com.omnidot.core.MatrixPreset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowEditor(
    initial: AutomationRule?,
    defaultVoice: VoiceStyle,
    onClose: () -> Unit,
    onSave: (AutomationRule) -> Unit,
    onTest: (AutomationRule) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = initial != null && initial.name.isNotBlank()
    var rule by remember {
        mutableStateOf(initial ?: AutomationRule(
            name = "", trigger = AutomationTrigger.POWER_CONNECTED, action = AutomationAction.SPEAK_TEXT,
            speechPitch = defaultVoice.pitch, speechRate = defaultVoice.rate,
        ))
    }
    var sheet by remember { mutableStateOf<String?>(null) } // "trigger" | "action"
    var contactNote by remember { mutableStateOf<String?>(null) }
    var listenerGranted by remember { mutableStateOf(OmniNotificationListenerService.isNotificationAccessGranted(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        listenerGranted = OmniNotificationListenerService.isNotificationAccessGranted(context); onPauseOrDispose { }
    }
    BackHandler(onBack = onClose)

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: run { contactNote = "No contact was selected."; return@rememberLauncherForActivityResult }
        contactNote = try {
            context.contentResolver.query(uri, arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER,
            ), null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use "Could not read that contact. Please choose again."
                val name = c.getString(0).orEmpty()
                val number = c.getString(1).orEmpty()
                if (number.isBlank()) "That contact has no phone number." else { rule = rule.copy(contactName = name, contactPhone = number); null }
            } ?: "Could not read that contact. Please choose again."
        } catch (_: SecurityException) { "Android did not grant access to that contact." }
        catch (_: IllegalArgumentException) { "That contact is no longer available." }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit workflow" else "New workflow") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onTest(rule) }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Test")
                    }
                    Button(
                        onClick = { onSave(rule.copy(name = WorkflowText.name(rule))) },
                        enabled = WorkflowText.canSave(rule),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Save") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("When", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            ChoiceRow(triggerIcon(rule.trigger), WorkflowText.whenText(rule), rule.trigger.subtitle) { sheet = "trigger" }
            if (rule.trigger.needsAppAlive) Note("Works while OmniDot is running. Background reliability comes in the next update.")
            if (rule.trigger == AutomationTrigger.WHATSAPP_MESSAGE) {
                Field(rule.contactName, { rule = rule.copy(contactName = it) }, "Message from", "Contact name (leave empty for anyone)")
            }

            Text("Do", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            ChoiceRow(actionIcon(rule.action), WorkflowText.doText(rule), rule.action.subtitle) { sheet = "action" }
            ActionFields(rule, onChange = { rule = it }, contactNote = contactNote, onPickContact = {
                contactNote = null
                contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            }, onPreview = {
                scope.launch {
                    if (rule.playChime) { SoundSynthesizer.playSoftBell(); kotlinx.coroutines.delay(700) }
                    VoiceFeedbackManager.speak(context, rule.targetMessage.ifBlank { "This is how I sound" }, rule.speechPitch, rule.speechRate, defaultVoice.volume)
                }
            })
            Spacer(Modifier.height(24.dp))
        }
    }

    when (sheet) {
        "trigger" -> ChoiceSheet(
            title = "When should it run?",
            items = AutomationTrigger.pickerOptions + listOfNotNull(initial?.trigger?.takeIf { !it.available }),
            categoryOf = { it.category.label }, label = { it.label }, subtitle = { it.subtitle }, icon = { triggerIcon(it) },
            locked = { it == AutomationTrigger.WHATSAPP_MESSAGE && !listenerGranted },
            onUnlock = { OmniNotificationListenerService.openNotificationAccessSettings(context) },
            onSelect = { rule = rule.copy(trigger = it); sheet = null },
            onDismiss = { sheet = null },
        )
        "action" -> ChoiceSheet(
            title = "What should it do?",
            items = AutomationAction.workflowOptions + listOfNotNull(initial?.action?.takeIf { it !in AutomationAction.workflowOptions }),
            categoryOf = { it.category.label }, label = { it.label }, subtitle = { it.subtitle }, icon = { actionIcon(it) },
            locked = { false }, onUnlock = {},
            onSelect = { rule = rule.copy(action = it); sheet = null },
            onDismiss = { sheet = null },
        )
    }
}

@Composable
private fun ChoiceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, placeholder: String, minLines: Int = 1) {
    OutlinedTextField(value, onChange, label = { Text(label) }, placeholder = { Text(placeholder) },
        minLines = minLines, singleLine = minLines == 1, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ActionFields(
    rule: AutomationRule,
    onChange: (AutomationRule) -> Unit,
    contactNote: String?,
    onPickContact: () -> Unit,
    onPreview: () -> Unit,
) {
    when (rule.action) {
        AutomationAction.SPEAK_TEXT -> {
            Field(rule.targetMessage, { onChange(rule.copy(targetMessage = it)) }, "Words to speak", "Charging started", minLines = 2)
            Text("Voice", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VoiceStyle.entries.forEachIndexed { i, style ->
                    SegmentedButton(
                        selected = rule.speechPitch == style.pitch && rule.speechRate == style.rate,
                        onClick = { onChange(rule.copy(speechPitch = style.pitch, speechRate = style.rate)) },
                        shape = SegmentedButtonDefaults.itemShape(i, VoiceStyle.entries.size),
                    ) { Text(style.label) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Soft bell first", style = MaterialTheme.typography.bodyLarge)
                    Text("A gentle tone before speaking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = rule.playChime, onCheckedChange = { onChange(rule.copy(playChime = it)) })
            }
            TextButton(onClick = onPreview, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Preview voice")
            }
        }
        AutomationAction.SEND_WHATSAPP, AutomationAction.CALL_PHONE -> {
            Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (rule.contactPhone.isBlank()) "No contact chosen" else "${rule.contactName} · ${rule.contactPhone}", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onPickContact) { Text(if (rule.contactPhone.isBlank()) "Choose contact" else "Change contact") }
                    contactNote?.let { Note(it) }
                }
            }
            Field(rule.targetMessage, { onChange(rule.copy(targetMessage = it)) },
                if (rule.action == AutomationAction.CALL_PHONE) "Say aloud on this phone" else "WhatsApp message", "Running 5 minutes late", minLines = 2)
            Note(if (rule.action == AutomationAction.CALL_PHONE) "Opens the dialer and speaks this on your phone. Android doesn't send app speech into calls."
                 else "Opens WhatsApp with your message ready. You tap Send.")
        }
        AutomationAction.HTTP_REQUEST, AutomationAction.HTTP_POST, AutomationAction.DISCORD_WEBHOOK,
        AutomationAction.SLACK_WEBHOOK, AutomationAction.OPEN_URL -> {
            Field(rule.url, { onChange(rule.copy(url = it.trim())) }, "URL", when (rule.action) {
                AutomationAction.DISCORD_WEBHOOK -> "https://discord.com/api/webhooks/…"
                AutomationAction.SLACK_WEBHOOK -> "https://hooks.slack.com/services/…"
                else -> "https://example.com/webhook"
            })
            if (rule.action == AutomationAction.HTTP_POST) {
                Field(rule.customPayload, { onChange(rule.copy(customPayload = it)) }, "JSON body (optional)", "{ \"battery\": \"{battery}\" }", minLines = 3)
            }
            if (rule.action == AutomationAction.DISCORD_WEBHOOK || rule.action == AutomationAction.SLACK_WEBHOOK) {
                Field(rule.message, { onChange(rule.copy(message = it)) }, "Message", "Battery {battery}%")
            }
            Note("Variables: {battery} {date} {year_percent} {time} {trigger}")
        }
        AutomationAction.SHOW_NOTIFICATION -> {
            Field(rule.message, { onChange(rule.copy(message = it)) }, "Notification text", "Battery is at {battery}%")
            Note("Variables: {battery} {date} {year_percent} {time} {trigger}")
        }
        AutomationAction.APPLY_PRESET -> {
            Text("Lock screen view", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            MatrixPreset.entries.forEach { p ->
                Row(Modifier.fillMaxWidth().clickable { onChange(rule.copy(preset = p)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = rule.preset == p, onClick = { onChange(rule.copy(preset = p)) })
                    Text(p.title)
                }
            }
        }
        else -> Unit
    }
}
```

- [ ] **Step 3: Wire into `MainActivity`** — replace the `if (editorOpen)` branch body with:

```kotlin
val prefs by viewModel.preferences.collectAsStateWithLifecycle()
WorkflowEditor(
    initial = editing, defaultVoice = prefs.voiceStyle,
    onClose = { editorOpen = false },
    onSave = { viewModel.saveAutomation(it); editorOpen = false; scope.launch { snackbar.showSnackbar("Workflow saved") } },
    onTest = { rule -> viewModel.testAutomation(rule) { msg -> scope.launch { snackbar.showSnackbar(msg) } } },
)
```

(Editor draws its own Scaffold, so it is placed outside the outer padding: wrap nothing.) Remove the `testStatus`/`clearTestStatus` usage from MainActivity.

- [ ] **Step 4: Delete `ui/AutomationScreen.kt`** — `git rm app/src/main/java/com/omnidot/ui/AutomationScreen.kt`; delete `testStatus`/`clearTestStatus` from `OmniDotViewModel` (keep `testAutomation` callback).

- [ ] **Step 5: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS; install; create a workflow from a recipe, search "batt" in the trigger sheet, save, test.

- [ ] **Step 6: Commit**

```bash
git add -A app/src
git commit -m "feat(workflows): full-screen editor with searchable bottom-sheet pickers"
```

---

### Task 15: Goals tab

**Files:**
- Create: `ui/goals/GoalsViewModel.kt`, `ui/goals/GoalVisuals.kt`, `ui/goals/GoalsScreen.kt`, `ui/goals/GoalEditorSheet.kt`, `ui/goals/GoalDetailSheet.kt`, `ui/goals/LockscreenSection.kt`
- Modify: `MainActivity.kt`
- Delete: `ui/PreviewScreen.kt`; remove `selectPreset`, `setBirthDate`, `setVerticalOffset`, `setMatrixScale`, `setAccentColor`, `applyWallpaper`, `applyStatus` from `OmniDotViewModel` (moved), keep `ApplyStatus` sealed interface (move it to `GoalsViewModel.kt`).

**Interfaces:**
- Consumes: `GoalRepository`, `GoalDays`, `GoalsMatrix`, `WallpaperRefresher`, `CheckInReminderWorker`, `PreferencesManager`, `AdaptiveMatrixEngine.render`, `PhonePreview`, `SectionHeader`, `SurfaceCard`.
- Produces: `class GoalsViewModel(app): AndroidViewModel` with `goals: StateFlow<List<Goal>>`, `preferences: StateFlow<OmniDotPreferences>`, `applyStatus: StateFlow<ApplyStatus>`, `fun save(goal: Goal)` (returns via `firstGoalCreated: SharedFlow<Unit>` when it was the first goal and reminder got auto-enabled), `fun delete(goal)`, `fun restore(goal)`, `fun archive(goal)`, `fun toggle(goalId, date)`, `fun setPreset`, `setPeriod`, `setBirthDate`, `setOffset`, `setScale`, `setAutomaticUpdates`, `fun apply()`; `@Composable fun GoalsScreen(padding, snackbar, vm: GoalsViewModel = viewModel())`; `fun GoalTier.color(): Color`; `@Composable fun TierShape(tier: GoalTier, size: Dp)`.

- [ ] **Step 1: `GoalsViewModel.kt`**

```kotlin
package com.omnidot.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omnidot.core.LockPeriod
import com.omnidot.core.MatrixPreset
import com.omnidot.core.MatrixSyncWorker
import com.omnidot.core.WallpaperRefresher
import com.omnidot.data.OmniDotPreferences
import com.omnidot.data.PreferencesManager
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalRepository
import com.omnidot.goals.reminder.CheckInReminderWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface ApplyStatus {
    data object Idle : ApplyStatus
    data object Applying : ApplyStatus
    data object Applied : ApplyStatus
    data class Error(val message: String) : ApplyStatus
}

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GoalRepository(application)
    private val prefs = PreferencesManager(application)

    val goals: StateFlow<List<Goal>> = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences: StateFlow<OmniDotPreferences> = prefs.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OmniDotPreferences())
    private val _applyStatus = MutableStateFlow<ApplyStatus>(ApplyStatus.Idle)
    val applyStatus = _applyStatus.asStateFlow()
    private val _firstGoalCreated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val firstGoalCreated = _firstGoalCreated.asSharedFlow()

    fun save(goal: Goal) = viewModelScope.launch {
        val wasEmpty = repository.current().none { !it.archived }
        repository.save(goal)
        val settings = prefs.current()
        if (wasEmpty && !settings.reminderConfigured) {
            prefs.setReminder(true)
            CheckInReminderWorker.schedule(getApplication(), settings.reminderMinuteOfDay)
            _firstGoalCreated.tryEmit(Unit)
        }
        refresh()
    }

    fun delete(goal: Goal) = viewModelScope.launch { repository.delete(goal.id); refresh() }
    fun restore(goal: Goal) = viewModelScope.launch { repository.save(goal); refresh() }
    fun archive(goal: Goal) = viewModelScope.launch { repository.save(goal.copy(archived = true)); refresh() }
    fun toggle(goalId: String, date: LocalDate) = viewModelScope.launch { repository.toggle(goalId, date); refresh() }

    fun setPreset(preset: MatrixPreset) = edit { prefs.setPreset(preset) }
    fun setPeriod(period: LockPeriod) = edit { prefs.setLockPeriod(period) }
    fun setBirthDate(date: LocalDate) = edit { prefs.setBirthDate(date) }
    fun setOffset(offset: Float) = edit { prefs.setVerticalOffset(offset) }
    fun setScale(scale: Float) = edit { prefs.setMatrixScale(scale) }
    fun setAutomaticUpdates(enabled: Boolean) = viewModelScope.launch {
        prefs.setAutomaticUpdates(enabled)
        if (enabled) MatrixSyncWorker.scheduleNext(getApplication()) else MatrixSyncWorker.cancel(getApplication())
    }

    fun apply() = viewModelScope.launch {
        _applyStatus.value = ApplyStatus.Applying
        _applyStatus.value = try {
            WallpaperRefresher.apply(getApplication())
            if (prefs.current().automaticUpdates) MatrixSyncWorker.scheduleNext(getApplication())
            ApplyStatus.Applied
        } catch (error: Exception) {
            ApplyStatus.Error(error.message ?: "Could not set the lock screen wallpaper")
        }
    }

    private fun edit(block: suspend () -> Unit) = viewModelScope.launch { block(); _applyStatus.value = ApplyStatus.Idle }

    private suspend fun refresh() { runCatching { WallpaperRefresher.refreshIfEnabled(getApplication()) } }
}
```

- [ ] **Step 2: `GoalVisuals.kt`**

```kotlin
package com.omnidot.ui.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnidot.goals.GoalTier
import com.omnidot.ui.theme.TierBig
import com.omnidot.ui.theme.TierMedium
import com.omnidot.ui.theme.TierSmall

fun GoalTier.color(): Color = when (this) {
    GoalTier.SMALL -> TierSmall
    GoalTier.MEDIUM -> TierMedium
    GoalTier.BIG -> TierBig
}

@Composable
fun TierShape(tier: GoalTier, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val c = tier.color()
        when (tier) {
            GoalTier.SMALL -> drawCircle(c)
            GoalTier.MEDIUM -> drawRoundRect(c, cornerRadius = CornerRadius(this.size.width * .24f))
            GoalTier.BIG -> drawPath(Path().apply {
                moveTo(this@Canvas.size.width / 2, 0f); lineTo(this@Canvas.size.width, this@Canvas.size.height / 2)
                lineTo(this@Canvas.size.width / 2, this@Canvas.size.height); lineTo(0f, this@Canvas.size.height / 2); close()
            }, c)
        }
    }
}
```

- [ ] **Step 3: `GoalEditorSheet.kt`** — add/edit sheet:

```kotlin
package com.omnidot.ui.goals

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalTier
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorSheet(initial: Goal?, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var tier by remember { mutableStateOf(initial?.tier ?: GoalTier.SMALL) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: tier.defaultEmoji) }
    var start by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var end by remember { mutableStateOf(initial?.endDate) }

    fun pick(current: LocalDate, min: LocalDate?, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(context, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) }, current.year, current.monthValue - 1, current.dayOfMonth).apply {
            min?.let { datePicker.minDate = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
        }.show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (initial == null) "New goal" else "Edit goal", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(emoji, { emoji = it.take(8) }, label = { Text("Icon") }, singleLine = true, modifier = Modifier.width(88.dp))
                OutlinedTextField(title, { title = it.take(60) }, label = { Text("Goal") }, placeholder = { Text("Run 5 km") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Text("Size", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalTier.entries.forEach { t ->
                    val selected = tier == t
                    Surface(
                        modifier = Modifier.weight(1f).clickable {
                            if (emoji == tier.defaultEmoji) emoji = t.defaultEmoji
                            tier = t
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) t.color().copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (selected) BorderStroke(2.dp, t.color()) else null,
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t.defaultEmoji, style = MaterialTheme.typography.headlineSmall)
                            TierShape(t, 14.dp, Modifier.padding(vertical = 6.dp))
                            Text(t.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { pick(start, null) { start = it; if (end?.isBefore(it) == true) end = it } }, modifier = Modifier.fillMaxWidth()) {
                Text("Starts ${start.format(dateFormat)}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pick(end ?: start.plusDays(29), start) { end = it } }, modifier = Modifier.weight(1f)) {
                    Text(end?.let { "Ends ${it.format(dateFormat)}" } ?: "No end date")
                }
                if (end != null) TextButton(onClick = { end = null }) { Text("Clear") }
            }
            Button(
                onClick = {
                    onSave((initial ?: Goal(UUID.randomUUID().toString(), "", "", tier, start, null, emptySet()))
                        .copy(title = title.trim(), emoji = emoji.ifBlank { tier.defaultEmoji }, tier = tier, startDate = start, endDate = end))
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save goal") }
        }
    }
}
```

- [ ] **Step 4: `GoalDetailSheet.kt`** — month calendar with prev/next; tap toggles allowed days:

```kotlin
package com.omnidot.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalDays
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailSheet(
    goal: Goal,
    onDismiss: () -> Unit,
    onToggle: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val (done, elapsed) = GoalDays.progress(goal, today)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.title, style = MaterialTheme.typography.titleLarge)
                    Text("$done of $elapsed days · 🔥 ${GoalDays.currentStreak(goal, today)} in a row",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TierShape(goal.tier, 16.dp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month") }
                Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.from(today)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month")
                }
            }
            val lead = month.atDay(1).dayOfWeek.value - 1
            val cells = List<LocalDate?>(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { i ->
                        val date = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                            if (date != null) {
                                val checked = date in goal.checkIns && goal.activeOn(date)
                                val enabled = goal.canCheckIn(date, today)
                                Box(
                                    Modifier.fillMaxSize().clip(CircleShape)
                                        .then(if (checked) Modifier.background(goal.tier.color()) else Modifier)
                                        .then(if (date == today && !checked) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                        .clickable(enabled = enabled) { onToggle(date) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            checked -> androidx.compose.ui.graphics.Color(0xFF121417)
                                            enabled -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f)
                                        })
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                OutlinedButton(onClick = onArchive, modifier = Modifier.weight(1f)) { Text("Archive") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Delete") }
            }
        }
    }
}
```

- [ ] **Step 5: `LockscreenSection.kt`**

```kotlin
package com.omnidot.ui.goals

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnidot.core.AdaptiveMatrixEngine
import com.omnidot.core.LockPeriod
import com.omnidot.core.MatrixPreset
import com.omnidot.data.OmniDotPreferences
import com.omnidot.goals.Goal
import com.omnidot.ui.components.PhonePreview
import com.omnidot.ui.components.SectionHeader
import com.omnidot.ui.components.SurfaceCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockscreenSection(prefs: OmniDotPreferences, goals: List<Goal>, status: ApplyStatus, vm: GoalsViewModel) {
    val context = LocalContext.current
    var offset by remember(prefs.verticalOffset) { mutableFloatStateOf(prefs.verticalOffset) }
    var scale by remember(prefs.matrixScale) { mutableFloatStateOf(prefs.matrixScale) }
    val preview = remember(prefs, goals, offset, scale) {
        AdaptiveMatrixEngine().render(android.util.Size(432, 900), prefs.copy(verticalOffset = offset, matrixScale = scale), goals).asImageBitmap()
    }
    SectionHeader("Lock screen")
    PhonePreview(preview)
    Spacer(Modifier.height(16.dp))
    SurfaceCard {
        Dropdown("Show", MatrixPreset.entries, prefs.preset, { it.title }, vm::setPreset)
        if (prefs.preset == MatrixPreset.GOALS) Dropdown("Period", LockPeriod.entries, prefs.lockPeriod, { it.label }, vm::setPeriod)
        if (prefs.preset == MatrixPreset.LIFE_IN_WEEKS) {
            OutlinedButton(onClick = {
                val d = prefs.birthDate
                DatePickerDialog(context, { _, y, m, day -> vm.setBirthDate(LocalDate.of(y, m + 1, day)) }, d.year, d.monthValue - 1, d.dayOfMonth)
                    .apply { datePicker.maxDate = System.currentTimeMillis() }.show()
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Birth date · ${prefs.birthDate}") }
        }
        Text("Position", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
        Slider(offset, { offset = it }, valueRange = -.15f..0.15f, onValueChangeFinished = { vm.setOffset(offset) })
        Text("Size", style = MaterialTheme.typography.labelLarge)
        Slider(scale, { scale = it }, valueRange = .6f..1.4f, onValueChangeFinished = { vm.setScale(scale) })
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Update every midnight", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(prefs.automaticUpdates, vm::setAutomaticUpdates)
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = { vm.apply() }, enabled = status !is ApplyStatus.Applying, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text(if (status is ApplyStatus.Applying) "Applying…" else "Apply to lock screen")
    }
    when (status) {
        ApplyStatus.Applied -> Text("Lock screen updated. It refreshes after each check-in.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        is ApplyStatus.Error -> Text(status.message, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Dropdown(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = text(selected), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(text(it)) }, onClick = { onSelect(it); expanded = false }) }
        }
    }
}
```

- [ ] **Step 6: `GoalsScreen.kt`**

```kotlin
package com.omnidot.ui.goals

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalDays
import com.omnidot.ui.components.SectionHeader
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun GoalsScreen(padding: PaddingValues, snackbar: SnackbarHostState, vm: GoalsViewModel = viewModel()) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val status by vm.applyStatus.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var editor by remember { mutableStateOf<Goal?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        vm.firstGoalCreated.collect {
            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            snackbar.showSnackbar("We'll ask at 9 pm how it went. Change this in Settings.")
        }
    }
    val active = goals.filter { !it.archived }
    val todays = active.filter { it.activeOn(today) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Goals", style = MaterialTheme.typography.headlineMedium)
                Text("Mark the days you showed up. Your lock screen keeps count.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (todays.isNotEmpty()) {
                item { SectionHeader("Today") }
                items(todays, key = { "today-${it.id}" }) { goal -> TodayRow(goal, today) { vm.toggle(goal.id, today) } }
            }
            item { SectionHeader("All goals") }
            if (active.isEmpty()) {
                item {
                    Text("No goals yet. Add a small habit, a medium goal, or a big dream.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(active, key = { it.id }) { goal -> GoalRow(goal, today) { detailId = goal.id } }
            item { LockscreenSection(prefs, goals, status, vm) }
        }
        ExtendedFloatingActionButton(
            onClick = { editor = null; editorOpen = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("New goal") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (editorOpen) GoalEditorSheet(editor, onDismiss = { editorOpen = false }) { vm.save(it); editorOpen = false }
    goals.firstOrNull { it.id == detailId }?.let { goal ->
        GoalDetailSheet(
            goal = goal, onDismiss = { detailId = null },
            onToggle = { vm.toggle(goal.id, it) },
            onEdit = { detailId = null; editor = goal; editorOpen = true },
            onArchive = { detailId = null; vm.archive(goal) },
            onDelete = {
                detailId = null
                vm.delete(goal)
                scope.launch {
                    if (snackbar.showSnackbar("Goal deleted", actionLabel = "Undo", withDismissAction = true) == SnackbarResult.ActionPerformed) vm.restore(goal)
                }
            },
        )
    }
}

@Composable
private fun TodayRow(goal: Goal, today: LocalDate, onToggle: () -> Unit) {
    val done = today in goal.checkIns
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(goal.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierShape(goal.tier, 10.dp); Spacer(Modifier.width(6.dp))
                    Text(goal.tier.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (done) FilledTonalButton(onClick = onToggle) { Icon(Icons.Rounded.Check, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Done") }
            else Button(onClick = onToggle) { Text("Mark done") }
        }
    }
}

@Composable
private fun GoalRow(goal: Goal, today: LocalDate, onClick: () -> Unit) {
    val (done, elapsed) = GoalDays.progress(goal, today)
    val streak = GoalDays.currentStreak(goal, today)
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TierShape(goal.tier, 14.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${goal.emoji} ${goal.title}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (elapsed == 0) "Starts ${goal.startDate}" else "$done of $elapsed days" + if (streak > 0) " · 🔥 $streak in a row" else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

The spec's "Not today" in the Today card is represented by simply leaving a goal unmarked; the explicit **Not today** action lives in the reminder notification (Task 7).

- [ ] **Step 7: Wire into `MainActivity`** — `goals = { padding, snackbar -> GoalsScreen(padding, snackbar) }`. Delete `ui/PreviewScreen.kt` and the moved functions/state from `OmniDotViewModel` (keep `init` scheduling `MatrixSyncWorker` and `ensureDefaultRules`).

- [ ] **Step 8: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS. Install; add a Big goal, mark done, apply; confirm lock screen shows a gold diamond for today.

- [ ] **Step 9: Commit**

```bash
git add -A app/src
git commit -m "feat(goals): goals tab with today check-ins, calendar, editor and lock screen controls"
```

---

### Task 16: Settings tab

**Files:**
- Create: `ui/settings/SettingsScreen.kt`
- Modify: `OmniDotViewModel.kt`, `MainActivity.kt`

**Interfaces:**
- Consumes: `PreferencesManager` (`setVoiceStyle`, `setReminder`, `setReminderMinuteOfDay`, `remoteKey`, `regenerateRemoteKey`), `CheckInReminderWorker`, `HealthCheck.probe/fixIntent`, `VoiceFeedbackManager`, `SoundSynthesizer`.
- Produces in `OmniDotViewModel`: `preferences` (already), `remoteKey: StateFlow<String>`, `fun setVoiceStyle(style)`, `fun setReminderEnabled(enabled)`, `fun setReminderTime(minuteOfDay)`, `fun regenerateRemoteKey()`. `@Composable fun SettingsScreen(padding: PaddingValues, snackbar: SnackbarHostState, vm: OmniDotViewModel)`.

- [ ] **Step 1: ViewModel additions**

```kotlin
private val _remoteKey = MutableStateFlow("")
val remoteKey = _remoteKey.asStateFlow()
// in init { … }: _remoteKey.value = preferencesManager.remoteKey()

fun setVoiceStyle(style: VoiceStyle) = viewModelScope.launch { preferencesManager.setVoiceStyle(style) }

fun setReminderEnabled(enabled: Boolean) = viewModelScope.launch {
    preferencesManager.setReminder(enabled)
    if (enabled) CheckInReminderWorker.schedule(getApplication(), preferencesManager.current().reminderMinuteOfDay)
    else CheckInReminderWorker.cancel(getApplication())
}

fun setReminderTime(minuteOfDay: Int) = viewModelScope.launch {
    preferencesManager.setReminderMinuteOfDay(minuteOfDay)
    if (preferencesManager.current().reminderEnabled) CheckInReminderWorker.schedule(getApplication(), minuteOfDay)
}

fun regenerateRemoteKey() = viewModelScope.launch { _remoteKey.value = preferencesManager.regenerateRemoteKey() }
```

- [ ] **Step 2: `SettingsScreen.kt`**

```kotlin
package com.omnidot.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnidot.OmniDotViewModel
import com.omnidot.automation.HealthCheck
import com.omnidot.automation.HealthIssue
import com.omnidot.automation.SoundSynthesizer
import com.omnidot.automation.VoiceFeedbackManager
import com.omnidot.automation.VoiceStyle
import com.omnidot.ui.components.SectionHeader
import com.omnidot.ui.components.SurfaceCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(padding: PaddingValues, snackbar: SnackbarHostState, vm: OmniDotViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val key by vm.remoteKey.collectAsStateWithLifecycle()
    var probe by remember { mutableStateOf(HealthCheck.probe(context)) }
    LifecycleResumeEffect(Unit) { probe = HealthCheck.probe(context); onPauseOrDispose { } }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { probe = HealthCheck.probe(context) }

    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionHeader("Voice")
        SurfaceCard {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VoiceStyle.entries.forEachIndexed { i, style ->
                    SegmentedButton(prefs.voiceStyle == style, { vm.setVoiceStyle(style) }, SegmentedButtonDefaults.itemShape(i, VoiceStyle.entries.size)) { Text(style.label) }
                }
            }
            TextButton(onClick = {
                scope.launch {
                    SoundSynthesizer.playSoftBell(); delay(700)
                    VoiceFeedbackManager.speak(context, "Take a breath. You're doing well.", prefs.voiceStyle.pitch, prefs.voiceStyle.rate, prefs.voiceStyle.volume)
                }
            }) { Text("Preview") }
            Text("New speak steps use this style. More voices are coming.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionHeader("Check-in reminder")
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ask me every evening", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(prefs.reminderEnabled, { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 && !probe.notificationsGranted) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    vm.setReminderEnabled(on)
                })
            }
            val h = prefs.reminderMinuteOfDay / 60; val m = prefs.reminderMinuteOfDay % 60
            OutlinedButton(onClick = { TimePickerDialog(context, { _, hh, mm -> vm.setReminderTime(hh * 60 + mm) }, h, m, false).show() },
                enabled = prefs.reminderEnabled, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("At %02d:%02d".format(h, m))
            }
        }

        SectionHeader("Permissions")
        SurfaceCard {
            PermissionRow("Notifications", probe.notificationsGranted) {
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(HealthCheck.fixIntent(context, HealthIssue.NOTIFICATIONS_BLOCKED))
            }
            PermissionRow("Notification access (WhatsApp)", probe.listenerGranted) { context.startActivity(HealthCheck.fixIntent(context, HealthIssue.LISTENER_MISSING)) }
            PermissionRow("Unrestricted battery", probe.ignoringBatteryOptimizations) { context.startActivity(HealthCheck.fixIntent(context, HealthIssue.BATTERY_OPTIMIZED)) }
        }

        SectionHeader("Remote trigger")
        SurfaceCard {
            Text("Tasker, ADB and links must include this key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionContainer { Text(key, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp)) }
            Row {
                TextButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("OmniDot key", key))
                    scope.launch { snackbar.showSnackbar("Key copied") }
                }) { Text("Copy") }
                TextButton(onClick = vm::regenerateRemoteKey) { Text("New key") }
            }
            SelectionContainer {
                Text(
                    "adb shell am broadcast -n com.omnidot/.automation.AutomationReceiver -a com.omnidot.TRIGGER_ACTION --es key $key\n\nomnidot://trigger?action=refresh&key=$key",
                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionHeader("About")
        SurfaceCard {
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
            Text("OmniDot $version", style = MaterialTheme.typography.bodyLarge)
            Text("Local-first automations and goals. Apache License 2.0.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(if (granted) "Allowed" else "Not allowed", style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!granted) TextButton(onClick = onFix) { Text("Fix") }
    }
}
```

- [ ] **Step 3: Wire** — `settings = { padding, snackbar -> SettingsScreen(padding, snackbar, viewModel) }`.

- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest assembleDebug -q` → PASS; install; Preview speaks calmly after a soft bell; key copies.

- [ ] **Step 5: Commit**

```bash
git add -A app/src
git commit -m "feat(settings): voice style, reminder, permissions, remote key and about"
```

---

### Task 17: Final cleanup, README and device verification

**Files:**
- Modify: `ui/theme/Color.kt` (delete old tokens), `AutomationEngine.kt` (drop any leftover old-token import), `README.md`, `AndroidManifest.xml` (verify)

- [ ] **Step 1: Remove old tokens** — delete `OledBlack`, `SurfaceDark`, `OmniOrange`, `DotInactive`, `PureWhite`, `MutedText` from `Color.kt`; run `grep -rn "OledBlack\|OmniOrange\|SurfaceDark\|DotInactive\|MutedText\|PureWhite" app/src` → expect no matches; fix any leftovers by switching to `MaterialTheme.colorScheme`.

- [ ] **Step 2: Rewrite `README.md`**

```markdown
# OmniDot

OmniDot is a calm, local-first Android app for small automations and daily goals.

- **Workflows** — "when this happens, do that": charger, battery, unlock, headphones, Wi-Fi,
  WhatsApp notifications and remote triggers → speak in a calm voice, notify, vibrate,
  call webhooks (Discord, Slack, Home Assistant, n8n) and more.
- **Goals** — small 🌱, medium 🌊 and big ⛰️ goals with a daily check-in. Your lock screen
  shows one dot per day in the colour and shape of your biggest win.
- **Lock screen views** — Goals, Year in dots, Life in weeks; refreshed after each check-in
  and at midnight once you've applied it.

## Permissions

| Permission | Why |
|---|---|
| Set wallpaper | Draw the lock screen dots |
| Run at startup | Reschedule the midnight refresh and check-in reminder |
| Notifications | Check-in reminders and "show notification" workflows |
| Internet, network state | Webhook workflows and Wi-Fi triggers (nothing is sent unless a workflow does it) |
| Vibrate | "Vibrate" workflows |
| Notification access (optional) | WhatsApp message triggers |

Device-event triggers currently work while OmniDot is running; background reliability is next.

## Build

Requires the Android SDK 35. Gradle provisions JDK 17 automatically.

    ./gradlew testDebugUnitTest assembleDebug

The debug APK is written to `app/build/outputs/apk/debug/`.

## License

Apache License 2.0. See [LICENSE](LICENSE).
```

- [ ] **Step 3: Full verification**

Run: `./gradlew clean testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Device checks** (connected phone)

```bash
./gradlew installDebug -q
adb shell am start -n com.omnidot/.MainActivity
adb exec-out screencap -p > /tmp/omnidot-workflows-dark.png
```

Walk through and screenshot (save under the session scratchpad, not the repo): Workflows (empty → recipe → saved card), editor + trigger sheet search, Goals (add Small/Medium/Big goals, mark two done, detail calendar), Lock screen section, Settings. Toggle light mode with `adb shell cmd uimode night no` and repeat the three tab screenshots; restore with `adb shell cmd uimode night yes`.

Functional checks:
- Apply to lock screen → lock the phone → today's dot is the biggest tier marked.
- `adb shell cmd jobscheduler run -f com.omnidot <job-id>` is brittle; instead set reminder time to 2 minutes ahead in Settings, wait, tap **Done** on the notification → lock screen updates.
- Remote trigger without key does nothing; with key runs:
  `adb shell am broadcast -n com.omnidot/.automation.AutomationReceiver -a com.omnidot.TRIGGER_ACTION --es key <key>`.
- Plug/unplug the charger with the app open → calm voice with soft bell.

- [ ] **Step 5: Commit**

```bash
git add -A README.md app/src
git commit -m "chore: remove legacy tokens and document the new OmniDot"
```
