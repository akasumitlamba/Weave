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

Recorded for PR 01 bootstrap. Update this table when a later milestone intentionally
pins a different compatible set.

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

Implementation proceeds as 24 stacked dependent PRs (`milestone/01-bootstrap` …
`milestone/24-release-handoff`). Merge in ascending order with merge commits only.
Do not squash, rebase published branches, or combine numbered PRs.

## CI

`.github/workflows/ci.yml` runs unit tests, lint, and `assembleDebug` for pushes and
pull requests targeting `main` or any `milestone/**` base branch.

## Review handoff

See `docs/REVIEW_INDEX.md` for the 24-PR stack, merge order, simulated merge evidence,
and device gates marked NOT RUN. Backup guide: `docs/RELEASE.md`.

