# Validation: 0.1.10

2026-10-23, Android API 36 emulator. No physical device was modified.

- Release/debug builds and release lint passed (0 errors, 9 warnings).
- ParserChecks: 29 passed; FloatingChecks: 21 passed, including 10-item retention and unread behavior.
- Overlay/reader regression run: 56 passed.
- HistoryChecks: 8 passed: exact active notification sends the original PendingIntent, removed/reused/changed notifications fail, legacy text-only entries fail, canceled PendingIntents fail without sending another job.
- UiChecks: 35 passed, including developer credit, clickable history and on-screen unavailable banner, swipe controls, first-run setup and theme persistence.
- Focused PanelChecks: 18 passed, including transparent/headerless panel, 3.5-row viewport, scrolling, complementary scheduled-job color, 28sp matching mark, newest-first scroll reset, delayed removal during collapse, and rapid transition reversal. Screenshot inspected under ignored `app/build/ui-validation/ten-jobs.png`.
- The original orange launcher image is unchanged. No transparent cutout is used on the launcher.

History stores the notification key, posting time and original content alongside result text. Actual PendingIntents are retained in a bounded in-memory cache, with active-notification re-resolution after process recreation. Every history open validates the current notification identity and content; it never guesses a replacement by title. Missing/expired entries produce an in-app OSD, a 90ms vibration when supported, and a 420ms row shake. Persisted PendingIntent parcels are deliberately not used.

Release uses the same signing certificate as v0.1.9 (`eb500eb3f5a9fb46441169df8e88f8d70e98844f7c43a93fdfaf52fe2ac63e73`) and keeps R8 and integrity checks enabled. Real Lalamove navigation still depends on the notification's supplied command and Android background restrictions.

Published APK SHA-256: `3700aeaf1d89f0ce26f476ebbdc9ca30183313a01b22e29aa61deec969900187`.
