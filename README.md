# Weave

**Connecting ideas together**

Offline handwriting notebook for the TCL TAB 10s (passive capacitive stylus).
Native Kotlin + Android Views with a custom Canvas ink path. No login, ads,
analytics, cloud, OCR, or network permission.

## Toolchain (pinned)

| Component | Version |
|-----------|---------|
| JDK (compile) | 17 |
| Android Gradle Plugin | 8.5.2 |
| Gradle | 8.9 |
| Kotlin | 1.9.24 |
| compileSdk / targetSdk | 34 |
| minSdk | 29 (Android 10) |
| Build-Tools | 34.0.0 |

Version 0.2.0-tablet (versionCode 2).

## Build commands

Requires Android SDK with `platforms;android-34` and `build-tools;34.0.0`.
Set `sdk.dir` in `local.properties` (not committed) or export `ANDROID_HOME`.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Product limits

- Offline only — note content stays on device
- No `INTERNET` / network permissions
- Passive stylus may report as finger; palm rejection is best-effort via guards
- Tagline appears on the library screen (not the writing toolbar)

## PR stack

Milestones 1–24 are merged. Milestone 25 completes the native tablet flows on
`milestone/25-tablet-completion`. Merge order: after 24. Do not merge without the
owner’s explicit instruction; preserve the historical milestone commits.

## CI

`.github/workflows/ci.yml` runs unit tests, lint, and `assembleDebug` for pushes and
pull requests targeting `main` or any `milestone/**` base branch.

## Review handoff

See [tablet completion](docs/TABLET_COMPLETION.md) for current behavior and measured
device checks. Historical milestone evidence remains in `docs/REVIEW_INDEX.md`.
Use the library’s Backup action for editable `.weave` archives and Restore to
import a separate copy. PDF export is for reading and sharing, not editing.

