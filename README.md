# OmniDot

OmniDot is a local-first Android lockscreen matrix HUD. It renders time visualizations directly with `Bitmap`, `Canvas`, and `Paint`, then applies the result only to the lockscreen through `WallpaperManager.FLAG_LOCK`.

## Features

- Year progress, 4,160-week life grid, 100-day sprint, and a 52 × 7 local heatmap
- Native Kotlin/Compose UI with an OLED-first Material 3 theme
- Preferences stored on-device with DataStore
- One-shot WorkManager refresh scheduled for local midnight and restored after reboot
- No network permission, account, analytics, server, or resident background service

## Build

Install Android SDK 35 and JDK 17, then run:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Privacy

OmniDot requests only wallpaper and boot-completion permissions. It has no internet permission, so configuration and rendering stay on the device.

## License

Apache License 2.0. See [LICENSE](LICENSE).

