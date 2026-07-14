# Server Sentinel

An Android app for locally monitoring Minecraft Java servers. It checks configured servers on-device, keeps a local history, and sends notifications when a server goes offline.

## Features

- Add and manage any number of Minecraft Java servers.
- Periodic on-device monitoring with local notification alerts.
- Optional ongoing monitoring notification.
- Local check history for manual and automatic checks.
- First-run guidance for notification and battery settings.

## Build

Open the project in Android Studio, or run:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

Server addresses and monitoring logs are stored only on the user's device. This repository contains no preconfigured server addresses.

## License

MIT. See [LICENSE](LICENSE).
