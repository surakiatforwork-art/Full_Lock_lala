# Validation: v0.1.12

2026-10-23. Android API 36 emulator and Java 17/Node 24 reference checks. No physical device was modified.

## Build and compatibility

- Debug/release APK and test APK built successfully; release lint: 0 errors, 11 warnings.
- Release is R8-minified, non-debuggable, and signed v2/v3 with the same certificate as v0.1.9/v0.1.10.
- Installed signed v0.1.10 on the test emulator, updated in place to signed v0.1.12, and verified normal first-run setup and the migrated default-profile header in the minified release. Integrity checks accepted the genuine APK.
- Previous emulator debug preferences were archived for the signing migration check and restored afterwards.

## Automated checks

- ParserChecks: 29 passed; FloatingChecks: 21 passed.
- OverlayChecks: 67 passed; FilterChecks: 7 passed; HistoryChecks: 8 passed; UiChecks: 48 passed. Includes the previously local route exclusions, themed card headers, endpoint start feedback/release, stop confirmation/cancel, chat bubbles and configurable motion.
- ProfileChecks: 28 passed. Includes isolated migration of legacy prices/routes/theme, idempotence, rename of the initial profile, autosave, copy isolation, restored palette on selection, preservation of global controls, overwrite preserving identity/count, deletion fallback and last-profile protection.
- ProfileUiChecks: 35 passed. Includes header selector, add/rename/copy, live-switch confirmation/cancel, duplicate import cancel/overwrite, URI-based import/export result handling, and active deletion selecting a replacement.
- ProfileFileChecks: 34 passed. Includes round trips, Thai UTF-8, malformed/truncated files, wrong version/types/ranges/CRC, duplicate records, bounded reads, and preservation of legacy long lists in local profiles while enforcing portable-file limits.
- FlpWebChecks: 10 passed. Browser-compatible JavaScript encoder reads the Java fixture independently; Java decodes the JavaScript fixture with matching Thai routes and default palette. Typical fixtures are 186–193 bytes.

Total: 287 assertions across the above suites. Instrumentation uses synthetic notifications and test files. System document-provider UX can differ by device; vibration feel and actual Driver navigation still require physical-device testing.

## Visual review and future website

Profile header and duplicate-name overwrite dialog screenshots inspected under ignored `app/build/ui-validation/profile-header.png` and `profile-overwrite.png`. Previous bubble/filter/stop/menu screenshots were also reviewed for the included UI changes.

Website handoff and FLP v1 binary format: [website-and-flp-plan.md](website-and-flp-plan.md). Browser reference encoder: `tools/flp-web-codec.mjs`. Profile files are compressed data, not encrypted or signed and never executed as scripts. CRC detects corruption, not a malicious author.

APK SHA-256: `d4d684bc568101f2f83ce899bf4339a9daaa1054946a32ded5e707f2f0122f3f`.
