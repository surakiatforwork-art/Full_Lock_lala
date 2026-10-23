# Local preview 0.1.11

2026-10-23. Local changes only. No commit, push, tag or release. Profile storage/import/export is not implemented in this iteration.

- Collapsible unwanted-route controls live inside the wanted-route card. Origin/destination exclusions have independent switches and comma-separated lists. Exclusions take precedence over positive filters, including the คลองสาม / คลองสามประเวศ case. Positive matching semantics are unchanged.
- Filter cards have a theme-colored heading band and rounded corners.
- Start requires reaching the slider endpoint and releasing there. Reaching the endpoint gives a 35ms vibration (where supported) and a 340ms droplet-like particle burst. Holding, retracting, partial drags and cancellation do not start operation.
- While running, tapping requests confirmation. Confirm stops; cancel retains operation. Cancel is white with a shadow. Operation continues while the confirmation is visible.
- Floating job bubbles widened from 205dp to 260dp (subject to screen size), shortened from 86dp to 72dp at normal font scale, with 18dp rounded corners. White/default or inverted-theme body, theme/complement type band, bold route and fare, prominent green check. Ten jobs and the 3.5-row viewport are retained.
- Expand/collapse duration defaults to 200ms. A collapsible control below the floating toggle stores 0.00–1.00 seconds in 0.01-second steps and applies to panel/item transitions. Permission controls remain in the menu.

Validation:

- Release/debug build and release lint passed (0 errors, 11 warnings).
- ParserChecks 29; FloatingChecks 21; OverlayChecks 67; FilterChecks 7; HistoryChecks 8; UiChecks 48: 180 assertions passed on API 36 emulator/pure Java.
- UI checks include holding at endpoint, splash presence, start on release, stop confirmation/cancel, exclusions dropdown, slider endpoints/intermediate value, and persistence after activity recreation.
- Screenshots inspected in ignored `app/build/ui-validation`: local-bubbles.png, local-filters.png, local-stop.png, local-menu.png.
- Physical vibration feel and real Driver navigation require device testing.

Installable signed candidate: `app/build/local-preview/FullLock-0.1.11-local.apk`. Uses the v0.1.9/v0.1.10 production signing identity; no private key is embedded or committed.
