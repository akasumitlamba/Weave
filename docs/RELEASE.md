# Weave release / backup guide

## Build
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

Release (unsigned instructions):
```bash
./gradlew assembleRelease
```
Sign with your local keystore; this project ships no cloud signing.

## Backup
Use notebook archive export (`NotebookArchive`) for editable backups. PDF export is not an editable backup.

## Offline contract
No network permission. Note content stays on device.
