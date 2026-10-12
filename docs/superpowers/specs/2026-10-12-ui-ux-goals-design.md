# OmniDot — Repo Cleanup, UI/UX Overhaul & Goals

Date: 2026-10-12 · Branch: `ui-ux-overhaul` · Status: awaiting review

## 1. Intent

OmniDot is becoming a calm, local-first Android automation app (MacroDroid-like, n8n-style later)
with a lockscreen that shows **whether you showed up for your goals**. This round cleans the repo,
redesigns the UI around three tabs, and adds Goals with daily check-ins. It does **not** add the
modular architecture, background trigger host, new triggers/actions, WhatsApp auto-send, or neural
voices — those are later phases.

Success criteria:
- `./gradlew testDebugUnitTest assembleDebug` works with the machine's default JDK.
- A new user can create a goal, check in, and see the dot on their lockscreen in under a minute.
- No screen offers a feature that does not work.
- Every screen is reviewed on the connected device via screenshots (dark + light).

## 2. Repo cleanup

| # | Change |
|---|---|
| C1 | ✅ Done — checkpoint commit `d0275b9` on `ui-ux-overhaul`. |
| C2 | Delete the 14 root-level test screenshots (`*.png` in repo root). |
| C3 | Pin JDK 17 via Gradle toolchain (`kotlin { jvmToolchain(17) }` + foojay resolver plugin in `settings.gradle.kts`). |
| C4 | Hide triggers with no backend: `SCHEDULED_TIME`, `WEATHER_RAIN`, `DAILY_MIDNIGHT`. Enum values stay for stored data; existing rules using them show a "Not available yet" badge and cannot be enabled. |
| C5 | Remove the `CONTRIBUTION_HEATMAP` preset and `SPRINT_100` preset (superseded by Goals). |
| C6 | Remove personal defaults ("Pradeep", "Hi I'm Deepak"). Replace the "YESSS!/Oh Noo!!" default rules with calm ones ("Charging started" / "Charger disconnected"). Existing user rules are not modified. |
| C7 | External triggers (`com.omnidot.TRIGGER_ACTION` broadcast and `omnidot://` deep links) require a `key` parameter matching a random per-install token, shown in Settings → Remote trigger. Requests without a valid key are ignored. |
| C8 | Split `ui/AutomationScreen.kt` (1,525 lines) into `ui/workflows/` files: list, card, editor, pickers, guide. Split `ui/PreviewScreen.kt` likewise into `ui/goals/`. |
| C9 | Rewrite `README.md`: what the app is, real permission list, build instructions. |

## 3. Navigation & visual system

Bottom navigation, three tabs; the app opens on **Workflows**:

```
[ ⚡ Workflows ]   [ ◆ Goals ]   [ ⚙ Settings ]
```

Visual language — calm:
- Dark: background `#121417`, surface `#1B1E22`. Light: background `#F6F5F2`, surface `#FFFFFF`. Follows system theme.
- One accent: muted teal `#5FB3A6` (replaces `#E65100` orange).
- Goal tiers: Small green `#7BC47F`, Medium blue `#6FA8DC`, Big gold `#E0B65A`.
- Sentence-case headings (no ALL-CAPS letter-spaced titles), Material 3 type scale, ≥48dp touch targets, content descriptions on all icons, respects font scaling.
- Toasts only for meaningful outcomes; deletes show a Snackbar with **Undo**. Remove the "Selected: …", "Accent color updated" style toasts.

## 4. Workflows tab

- **Card** reads as a sentence: trigger icon + "When charger connects" → "Speak 'Charging started'"; last run line (`✓ Ran 2 min ago` / `✕ Failed: <reason>`); enable switch; overflow menu: Edit · Test · Delete (Undo).
- **Empty state**: 3 one-tap recipes built only from existing triggers/actions: *Calm charging chime* (charger → speak), *Battery low → notification*, *Headphones → speak "Enjoy your music"*.
- **Editor**: full screen, two sections **When** and **Do**; each opens a `ModalBottomSheet` picker with search + category tabs. Items needing an ungranted permission show a 🔒 *Grant access* row instead of being hidden. Sticky bottom bar: **Test** · **Save**.
- **Health banner** (top of tab, only when relevant): missing notification permission, missing notification-listener access when a WhatsApp rule exists, battery optimisation not disabled. One **Fix** button opens the right settings screen.
- Honest copy for the known limitation: device-event triggers (charger, battery, unlock, headphones, Wi-Fi) note "Works while OmniDot is running — background reliability comes in the next update."

## 5. Goals tab

### Model
```kotlin
enum class GoalTier { SMALL, MEDIUM, BIG }          // rank: SMALL < MEDIUM < BIG
data class Goal(
    val id: String,
    val title: String,
    val emoji: String,            // user-chosen, default by tier: 🌱 / 🌊 / ⛰️
    val tier: GoalTier,
    val startDate: LocalDate,
    val endDate: LocalDate?,      // null = open-ended
    val checkIns: Set<LocalDate>,
    val archived: Boolean = false,
)
```
Stored as JSON in a new DataStore `omnidot_goals` (same pattern as `AutomationRepository`).
A goal is *active on* day `d` when `startDate ≤ d ≤ (endDate ?: ∞)` and not archived. Daily cadence only.

### Screen
- **Today card** at top: for each goal active today, a row with emoji, title, tier chip, and **✓ Done** / **Not today**. Done goals show a filled check; tapping again undoes.
- **Goal list**: tier icon + colour, title, `23 of 41 days`, `🔥 5 in a row`. Tapping opens **Goal detail**: month calendar of that goal's days (tap any past day to toggle), edit, archive, delete (Undo).
- **Add goal** sheet: title, emoji, tier (three large selectable cards showing colour, icon, shape), start date (default today), end date or "No end date".
- **Lockscreen** section (bottom of tab): live phone preview, **View** dropdown (Goals · Year in dots · Life in weeks), **Period** dropdown for Goals (This month · Last 12 weeks · This year), birth date (Life only), position/size sliders, **Apply to lock screen** button.

### Evening check-in reminder
- Settings: on/off (default on once a goal exists), time (default 21:00). Scheduled with WorkManager like `MatrixSyncWorker`.
- Fires only if some goal active today is unchecked. One unchecked goal → notification with **Done** / **Not today** actions. Several → "3 goals to check in" opening the Goals tab Today card.
- Notification actions go to a non-exported `CheckInReceiver`, which updates `GoalRepository` and re-renders the wallpaper (see §6).
- Requests `POST_NOTIFICATIONS` at the moment the user first enables the reminder (API 33+).

## 6. Lockscreen rendering

`AdaptiveMatrixEngine` gains a **Goals** view (layout A — one grid, each day shows the biggest win):

| Day state | Rendering |
|---|---|
| Achieved — highest tier checked in that day | Filled shape in tier colour: Big ◆ gold, Medium ■ blue, Small ● green |
| Missed — past, ≥1 goal active, none checked | Faint ring |
| No goals active that day | Inactive dark dot |
| Today, not yet checked | Accent-outlined dot |
| Future | Inactive dark dot |

Header text: period name + `18 of 31 days showed up`. Period grid: This month (7 columns, calendar-aligned), Last 12 weeks (7 × 12), This year (existing year layout).
Year in dots and Life in weeks keep their current rendering. Existing safe-area/position/scale logic is reused.

**When the wallpaper re-renders:** after any check-in change and at midnight — but only if the user has pressed **Apply to lock screen** at least once (`wallpaperEnabled` flag), so check-ins never change a wallpaper the user didn't opt into.

## 7. Settings tab
- **Voice**: style dropdown Calm (default: rate 0.9, pitch 1.0, volume 0.8) · Neutral · Cheerful; ▶ Preview. Remove the 4-second speech cut-off. Replace the arpeggio chimes with one soft synthesized bell (slow decay, low partials). Voice-engine dropdown and neural voices are the next phase.
- **Check-in reminder**: toggle + time.
- **Permissions**: list with status and Fix buttons (same checks as the health banner).
- **Remote trigger**: shows the per-install key with copy button and example `adb`/deep-link commands (replaces the Guide modal's ADB section).
- **About**: version, licences.

## 8. Migration
- Stored preset `SPRINT_100` or `CONTRIBUTION_HEATMAP` → `YEAR_PROGRESS`.
- Automation actions `APPLY_PRESET`/`CYCLE_PRESET` cycle over Goals · Year · Life.
- Rules referencing hidden triggers are kept, disabled, and badged (C4).
- No goals exist initially; the Goals view with no goals renders inactive dots and the header "Add a goal to start".

## 9. Testing & verification
Unit tests (JVM):
- Day state: achieved tier = max tier checked; missed vs no-active-goal vs today vs future; goal date ranges incl. open-ended and archived.
- Period grids: month column alignment, 12-week and year sizes; header counts.
- Check-in toggle and streak calculation (current run ending today or yesterday).
- Migration of removed presets; hidden-trigger filtering; remote-trigger key validation.
- Existing `AdaptiveMatrixEngineTest` / `AutomationRuleTest` updated.

Device: build with default JDK, install on the connected phone, screenshot each tab in dark and light, create goal → check in → apply → confirm lockscreen dot; trigger a notification check-in and confirm the wallpaper updates.

## 10. Out of scope (next phases, in order)
1. Modular Gradle split + node registry + background trigger host + run log.
2. Calm voice engine & voice dropdown → Kokoro Calm Pack → Piper/Kitten voices.
3. New trigger/action packs; WhatsApp send (notification reply → accessibility → SMS fallback).
