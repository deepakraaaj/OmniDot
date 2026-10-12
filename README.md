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
