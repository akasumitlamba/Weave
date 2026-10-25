# PR 01 evidence — Bootstrap

## Dependency / merge order
- Head: `milestone/01-bootstrap`
- Base: `main`
- Merge order: **1 of 24** (merge first)

## Commands
```
./gradlew assembleDebug testDebugUnitTest lintDebug
```
Result: PASS (local, 2026-10-25)

## Versions
See README toolchain table. JDK used locally: OpenJDK 21 (compile target 17).

## Device
- Emulator / TCL TAB 10s launch: **NOT RUN** (no device attached in CI agent)
- Steps: install `app/build/outputs/apk/debug/app-debug.apk`, confirm Weave splash text, confirm Settings → App info shows no network permission.

## Limitations
Bootstrap shell only; library/editor arrive in later PRs.
