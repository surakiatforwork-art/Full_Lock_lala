# Local security validation: 0.1.9

2026-10-22. No commit, push, release, repository access change or physical-device install.

## Build and signing

- `:app:assembleDebug :app:assembleRelease :app:lintRelease`: passed, no lint errors (9 warnings).
- APK signature verified by Android SDK `apksigner`: v2/v3, one RSA-3072 signer. Release is not debuggable.
- Expected signing certificate SHA-256 in generated release configuration matches the APK certificate.
- R8 mapping confirms internal classes/members renamed and repackaged. Mapping stays in ignored build output.
- Packaged manifest inspected for backup rules, disabled cleartext traffic, unchanged permissions, and notification service binding permission.
- Build with a missing signing properties file correctly fails; no debug-key fallback.
- New icon rendered at five launcher densities and inspected at 192px; notification monochrome icon retained.

## API 36 emulator checks

- Genuine signed/minified release launches first-run setup normally.
- Android refuses a differently signed APK as an in-place update (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
- Separately installed re-signed release displays the blocking dialog instead of normal UI. No AndroidRuntime crash logged.
- Prior emulator preferences saved and restored; physical devices untouched.
- ParserChecks: 29 passed. FloatingChecks: 21 passed.
- After restoring the debug installation: OverlayChecks 56 passed; UiChecks 32 passed (138 regression assertions in total with the pure-Java checks).

Artifact: `app/build/local-preview/FullLock-0.1.9-protected.apk`.
SHA256: `98158E8A4F908E623F30AD77C8F25F8995E9EBD4837BC0EE855BD953710B4F32`.

These checks demonstrate resistance to simple re-signing, not to patched integrity checks, source rebuilds, or an analyst reconstructing the app. Actual Driver operation and OEM backup behavior still need physical-device testing. See [security.md](security.md) for key backup, signing migration and public-source limitations.
