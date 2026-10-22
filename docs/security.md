# APK hardening (0.1.9)

Release builds enable R8 shrinking, optimization, class/member renaming and internal class repackaging. Debug builds remain readable for development. APK resources and Thai UI strings are still readable; obfuscation does not encrypt the application or prevent reconstruction.

Release builds are non-debuggable and require a private signing key; there is no fallback to the Android debug key. The expected signing certificate SHA-256 is derived at build time from the signing keystore. At runtime the activity and notification reader refuse to operate if the package name, debuggable flag or current signing certificate do not match. This is defense against simple re-signing: someone who patches the check or rebuilds the source can bypass it. It does not detect every modification or validate the app on a server.

Android verifies the APK signature on installation and refuses to update an installed application with a different signing key. No root/emulator/debugger-tool detection, screen-reading restrictions, or device identifiers are added. The app still has no Internet permission, backend, account system or remote licensing.

Backup rules exclude application data from Android cloud backups and device transfers, including notification history and preferences. Cleartext network traffic is disabled as an additional default. The notification service remains protected by Android's `BIND_NOTIFICATION_LISTENER_SERVICE`; only the launcher activity is publicly launchable.

## Private signing setup

This workstation's files are stored in `%USERPROFILE%/.fulllock/signing/`:

- `fulllock-release.p12`: private signing key.
- `release.properties`: keystore path and passwords.

The directory ACL allows the current Windows user. Back up both files together to a secure location. Losing the signing key prevents future updates signed with that identity. Do not commit, attach to a release, or send these files. Passwords must never be printed in logs.

On a new Windows signing workstation, `tools/Initialize-ReleaseSigning.ps1` generates a new identity only if no signing files exist. **For future updates, restore the original files instead of generating a new identity.** Optional `FULLLOCK_SIGNING_PROPERTIES` points to a private properties file with `storeFile`, `storePassword`, `keyAlias`, `keyPassword` (PKCS12). Use forward slashes in the path. Run `./gradlew.bat :app:assembleRelease :app:lintRelease`. A release build without signing configuration fails. Mapping files under `app/build/outputs/mapping/release` are private troubleshooting artifacts and should not accompany the APK.

The earlier test APKs use a different debug key. They cannot be updated in place with this release key. Uninstalling them deletes preferences/history and requires granting permissions again; do not uninstall a user's existing app automatically.

## Limits of copy protection

No APK-only mechanism guarantees protection against REA, AI analysis, decompilation or copying. A public source repository also exposes the implementation independently of APK hardening. Repository access, licensing and ownership notices are separate decisions; no repository visibility or licensing changes were made here. Strong authorization for a future paid service would need a separately designed backend; embedded secrets or local checks are not sufficient.

References: [R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [Android signing](https://developer.android.com/studio/publish/app-signing), [backup guidance](https://developer.android.com/privacy-and-security/risks/backup-best-practices).
