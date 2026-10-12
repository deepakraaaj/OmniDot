# Code action: write your own logic in JavaScript

Date: 2026-10-12 · Branch: `calm-alarm` · Status: awaiting approval

## Goal
Let developers write their own workflow logic on the phone, in JavaScript, with the app's abilities
exposed as an `omni` API. A "Run code" action can do anything the built-in actions do, branch on the
trigger's details, call web APIs (n8n, Home Assistant), remember state, and chain workflows.

## Engine
- Mozilla Rhino 1.7.15 (pure Java, ES6 subset incl. const/let/arrow functions), interpreted mode
  (`optimizationLevel = -1`, required on Android). Spike verified: runs `[1,2,3].map(x => x*2)` and dexes into the APK.
- Sandbox: `initSafeStandardObjects()` (no Java class access), a 10 s instruction-count timeout via
  `observeInstructionCount`, one script per run on a background thread.

## Model
- New action `RUN_CODE` (category "Code"). Rule gains `code: String`.
- Script runs with `event` = `{ trigger, sender, message, link, time }` (from `TriggerEvent` + trigger name).
- Return / throw → run history: success shows the last `log` line or "Script finished"; errors show
  `Line N: message`.

## `omni` API (each call maps to existing engine code)
| JS | Behaviour |
|---|---|
| `log(...args)` | Appends to the run log (kept for the editor's log panel) |
| `omni.speak(text, {style})` | Calm/neutral/cheerful voice, waits until spoken |
| `omni.bell()` | Soft bell |
| `omni.notify(title, text)` · `omni.vibrate()` · `omni.toast(text)` | Alerts |
| `omni.alarm.set(minutesFromNow, label)` | Clock app alarm (same as Set phone alarm) |
| `omni.alarm.calm(minutesFromNow, label)` | OmniDot calm alarm |
| `omni.time.now()` → `{hour, minute, weekday, date}` | Current time |
| `omni.goals.today()` → `[{title, tier, done, streak}]` · `omni.goals.checkIn(title)` | Goals |
| `omni.http.get(url)` / `omni.http.post(url, obj)` → `{status, body}` | Web; JSON bodies |
| `omni.battery()` · `omni.wifi()` · `omni.clipboard(text)` · `omni.open(url)` | Device |
| `omni.store.get(key)` / `omni.store.set(key, value)` | Persistent string/number/bool storage per app |
| `omni.run(name)` | Runs another enabled workflow by its name |

## Editor
- When the action is "Run code": a monospace code field with line numbers and an accessory row of
  symbols `{ } ( ) [ ] ; " ' = => .`, a **Templates** menu, **Run with sample event**, and a log panel
  (last 50 lines, errors in red with line numbers).
- Templates: Alarm after a link · Weekdays only · Post to n8n · Count messages per day · Morning summary.
- `canSave`: code not blank.

## Safety
- No Java access, no file system; network only through `omni.http`.
- 10 s limit per run; long-running loops stop with "Script took too long".
- Uses only permissions already granted; missing ones surface as errors in the log.

## Tests (JVM)
`ScriptRunnerTest` with a fake `OmniApi`: event fields visible; log capture; errors carry line numbers;
infinite loop stops by timeout; `omni.store` persistence via fake; JS objects → JSON for `http.post`;
templates all parse. Sentence/validation tests for `RUN_CODE`.
