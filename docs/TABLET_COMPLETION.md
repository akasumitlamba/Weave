# Tablet completion milestone 25

The owner's Word design and UI references define the writing experience. Historical milestones 1–24 supplied disconnected models; milestone 25 connects the native app into usable notebook flows. Historical PRs and commits are preserved. This milestone branches from `main` at `ebd0c11`; merge requires the owner's explicit instruction.

## Implemented flows

- Native library with A4 previews, create with plain/lined paper, rename, duplicate, confirmed delete, editable backup and validated restore through Android's file picker.
- Landscape and portrait editor with hand-drawn vector line icons, tool widths, colour swatches, remembered tool/smoothing settings, undo/redo, ordered page operations and undoable templates.
- SQLite transactions store notes, ordered pages and versioned stroke blobs. A single IO worker acknowledges Saved after commit. Failed saves retain the editable model and expose Retry. Writes occur after commands, never on MOVE; unchanged page blobs are not rewritten.
- Stable millimetre coordinates, screen-space incremental pinch, panning, fit page/width, cancellation and pointer ownership. Guard-origin contacts remain blocked; a stylus contact outside a resting guard can own ink independently.
- Optional shape dwell preview and raw/fitted undo; conservative geometric fits replace the previous unconditional polygon fallback. Off/Low/Medium smoothing is deterministic in the editor, thumbnails and PDF.
- Zoom writing shares the same page strokes/history. Source rectangle, horizontal movement and next-line controls alter only the viewport. Guards are separate in each viewport.
- A4 vector PDF export supports all/current/range, optional ruled lines, destination picker, background rendering and cancellation. Success follows stream close. A temporary local PDF is rendered before writing the selected destination.
- Bounded static vector scene recordings and path caches avoid replaying the whole committed scene for each new point. Erasing uses a page-space grid and interpolates sparse gesture samples.

## Identity and data

Version 0.2.0-tablet, versionCode 2. Installed debug id remains `com.weave.notebook.debug`. No network permission, analytics, account, OCR, AI, imported PDF or page typing. Android automatic app backup is disabled; editable copies are created explicitly through the library Backup action. Tagline appears on the library only.

Stroke format v2 stores 64-bit times, fitted geometry and the original freehand samples. Decoder accepts the previous v1 stroke format. New `.weave` backups are versioned ZIP containers; lengths, counts and payloads are validated before the imported notebook is committed under fresh UUIDs. Existing notebook rows are never modified by restore.

The original installed CI debug APK used an ephemeral signing key. Before replacing it, its private data was backed up externally; inspection found only `files/profileInstalled`, no saved notebook database. Future local updates use the existing workstation debug keystore. Keystores and private backups are not committed.

## Reproducible checks

Use JDK 17 and Android platform/build-tools 34:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.weave.notebook.debug.test/androidx.test.runner.AndroidJUnitRunner
```

## Acceptance boundaries

The automated shape corpus is synthetic: 20 noisy seeded traces per class plus 100 open scribble negatives. It does not prove recognition accuracy for a user's handwriting. A held handwritten “o” can be geometrically indistinguishable from an intended circle; Hold shapes is optional and initially off.

Passive stylus identity, human writing comfort, high-speed-camera latency, true palm rejection and a continuous 30-minute human writing session require owner acceptance. None is inferred from mockups or automated synthetic input. Guards are explicit touch blockers.

## Device results — 26 October 2026

Tested on the connected TCL 9080G / TAB 10s, Android 10, physical 1200 × 1920 display. These are real-device automated checks using synthetic input, not human stylus acceptance.

- JVM: 94 tests passed, zero failures/errors/skips.
- Android lint: zero errors; existing style/internationalization and deprecation warnings remain.
- Device instrumentation: seven tests passed. Coverage includes library create, synthetic ink, undo/redo, acknowledged save and reopen, portrait rotation, zoom writing, and export screen navigation; transactional rollback, two-page A4 PDF raster verification, cancellation, and navigation/CANCEL producing no ink.
- A 20-page / 500-stroke / 50,000-sample fixture survived SQLite reopen, editable backup/restore under fresh identities, and a metadata-only incremental save. This is a functional capacity check, not a latency benchmark.
- Screenshot evidence: [landscape editor](evidence/m25/weave-editor-landscape.png), [portrait editor](evidence/m25/weave-editor-portrait.png), [zoom writing](evidence/m25/weave-strip-landscape.png), [new note](evidence/m25/weave-new-note.png), [export](evidence/m25/weave-export-landscape.png), [library](evidence/m25/weave-library-landscape.png).
- Removed empty-library filler, notebook-count footer, tutorial modal, bootstrap screen and input-probe activity/layouts. Only actionable save/error/progress status remains; tagline stays on the library.
- Temporary verification notebooks were removed. No user notebook existed in the inspected original installation; app data backups remain outside the repository.

On this TCL firmware an instrumentation command may remain queued until the target app is foregrounded. If needed, start LibraryActivity after starting the runner. Always select the tablet serial when an emulator is also connected.

File-picker destination/provider behavior, human passive-stylus ergonomics, long-session latency and a 30-minute human writing session remain manual acceptance gates. The PDF engine and editable archive are tested; automated checks do not claim every third-party document provider has been exercised.
