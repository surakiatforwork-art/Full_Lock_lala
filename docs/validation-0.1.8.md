# Local validation: 0.1.8

2026-10-22. Local changes only; no commit, push, or release. Existing icon retained.

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`: passed. Lint has warnings (including existing target SDK/backup guidance and programmatic swipe view constructor/accessibility guidance), no errors.
- ParserChecks: 29 passed.
- FloatingChecks: 21 passed.
- Android API 36 emulator: OverlayChecks 56 passed; UiChecks 32 passed.
- Verified first-run permission guidance, permissions and floating toggle placement, no permission controls on filter/history pages, swipe tap/partial/cancel/full gestures, theme persistence after activity relaunch, black/white text inversion, and opening two different matching jobs immediately without cooldown.
- Existing overlay regressions include outside taps, selected row opening, preview timeout, pinned updates, side expansion, hidden states, ongoing/summary exclusion, and reading while auto-open/floating display are disabled.
- Screenshots inspected: permissions, filters/swipe, menu, inverted blue theme, custom color dialog. Stored in ignored `app/build/ui-validation`.

APK: `app/build/local-preview/FullLock-0.1.8.apk` (debug signed).
SHA256: `A8BB901AE9D1B0E345661151BF27C922ADBDA60A6BB69AB772CC9F45E326E41F`.

Instrumentation used synthetic notifications and a test PendingIntent on the emulator. Actual Lalamove navigation, device-specific permission screens, and background restrictions still require physical-device testing. Security changes and new icon are deferred to the next user instructions.
