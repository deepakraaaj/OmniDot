# Calm Alarm & "At a time" trigger

Date: 2026-10-12 · Branch: `calm-alarm` · Status: building (user asked to design and build directly)

## Why (user needs)
1. Jarring alarms start the day stressed → begin very quietly, rise slowly.
2. Fear of oversleeping → never stop on its own; firmer after 3 min; alarm audio stream (rings in silent/DND);
   `AlarmManager.setAlarmClock` (Doze-proof, survives reboot via BootReceiver).
3. Half-asleep users → full screen over the lock screen with two big buttons: Snooze, I'm up.
4. The first minute shapes the day → after "I'm up", calm voice greets and reads today's goals and streaks.
5. Weekdays ≠ weekends → per-alarm day chips, label, on/off, several alarms.

## Calm alarm
- Model `CalmAlarm(id, minuteOfDay, days: Set<DayOfWeek> /*empty = once*/, label, enabled, snoozeMinutes = 10, speakGreeting = true)`;
  stored as JSON in DataStore `omnidot_alarms`.
- `AlarmSchedule.nextTrigger(now, alarm)` → next ZonedDateTime strictly after now (respects days; once = today/tomorrow).
- Ringing: `AlarmRingService` (foreground, mediaPlayback) loops a soft bell on `USAGE_ALARM`.
  Volume ramp `RisingBell.volumeAt(seconds)`: 0.05 → 1.0 over 90 s; after 180 s stays 1.0 with gentle vibration.
  Posts a full-screen, category-ALARM notification opening `AlarmActivity` (shows over lock screen, turns screen on).
- Snooze reschedules +snoozeMinutes. I'm up stops ringing, reschedules the next occurrence (or disables a one-time alarm),
  and (if `speakGreeting`) speaks `MorningGreeting.text(goals, today)` with the calm voice.
- Alarms tab: list (time, label, days, switch), FAB "New alarm", editor sheet (time picker, day chips, label,
  snooze 5/10/15, greeting switch), delete with Undo. Next-alarm line ("Rings in 7 h 12 min").
- Permissions: `USE_EXACT_ALARM` (API 33+), `SCHEDULE_EXACT_ALARM` (≤32), `USE_FULL_SCREEN_INTENT`,
  `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`.

## "At a time" workflow trigger
- Re-enable `SCHEDULED_TIME` (renamed "At a time") with weekday chips; rule gains `days` (empty = every day).
- Scheduled with `setExactAndAllowWhileIdle`, re-armed after each run, on save/enable, and on boot.

## Tests
`AlarmScheduleTest`, `RisingBellTest`, `MorningGreetingTest`, `AlarmCodecTest`, rule `days` round-trip, sentence text.
