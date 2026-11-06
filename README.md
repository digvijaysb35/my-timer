# My Timer — Android Countdown App

A native Android countdown timer with a dark theme, quick presets, start, pause/resume, reset, an app-specific alarm volume slider (separate from Android alarm volume), a completion alarm, and a foreground service for running while the app is in the background.

## Build an APK without installing Android Studio on your phone

This project includes a GitHub Actions workflow that builds the APK in GitHub's cloud.

1. Create a new GitHub repository.
2. Upload the **contents** of this project folder to the repository (the `.github` folder must be included).
3. Open the repository's **Actions** tab.
4. Select **Build My Timer APK** and press **Run workflow** (or push to `main`/`master`).
5. Open the completed workflow run, scroll to **Artifacts**, and download `My-Timer-debug-apk`.
6. Extract the downloaded ZIP and install `app-debug.apk` on your Android phone. If Android asks, allow installation from that source.

## Build locally

Requires JDK 17, Gradle 8.9+ and Android SDK platform 35/build-tools installed.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Notes

- The countdown uses a foreground service and a partial wake lock while running so it can continue with the screen off.
- Android notification permission is requested on Android 13+.
- Device battery-management settings can still restrict background work on some phone models. If the timer is critical, allow background activity for My Timer.
- This is a debug APK build configuration. A release APK should be signed with your own private signing key before public distribution.


## Alarm volume

The in-app alarm volume slider is stored independently. The app plays its built-in alarm tone on the media audio stream with its own software gain, rather than using the Android alarm stream. The phone media volume can still affect final loudness, but the Android alarm-volume slider is independent.


## Version 3 additions

- While the My Timer screen is open and the alarm is sounding, pressing either hardware volume button silences/dismisses the alarm without changing the phone's volume. Android does not allow ordinary apps to capture hardware volume keys globally while another app or the lock screen is active, so use the persistent notification's **Stop alarm** action in that situation.
- Quick presets can be customized from the **Customize presets** button. Enter up to four minute values separated by commas (for example `2, 7, 15, 45`). Values are saved on the device.
