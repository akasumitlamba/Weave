# Milestone 26 — gesture editor, fullscreen and owl identity

Merge order: milestone 25, then 26. This branch targets `milestone/25-tablet-completion`; neither branch is merged automatically. Version 0.3.0-editor, versionCode 3.

## Writing and navigation

There is no Hand, Navigate or Write mode in the editor. A single eligible contact writes or erases. A second eligible contact cancels provisional ink/erasing and starts centroid-based two-finger pan plus anchored pinch. Lifting one finger never resumes ink until every finger has lifted. Guard-origin contacts remain blocked.

All brush presets remain visible in a fixed order. Width controls use a horizontal stroke sample, numeric millimetres, and an explicit Default marker; colors remain circular swatches. Pen, marker, highlighter and eraser use distinct native vector instrument illustrations. The eraser always offers 2, 4 (default), and 8 mm.

Eraser hit testing builds a page-space index once per gesture and queries only new movement segments, including historical samples. Hit strokes disappear from both writing viewports during MOVE. The document, history and database change once on lift. CANCEL, a second eligible finger, changing tools, or leaving the view restores the provisional erasure. Undo/redo treats the completed eraser sweep as one operation.

Fullscreen hides the normal header, page rail, zoom strip and footer. Tools and undo/redo sit on the left; all widths, color swatches, fit, pages, options and save status sit on the right. Exit fullscreen and Android Back restore the normal editor. Hold shapes is suspended and hidden inside fullscreen, with its remembered setting restored on exit. No zoom percentage or +/- controls remain at the bottom.

## Logo, splash and colors

The supplied folded-paper owl is represented as scalable gradient vector artwork, with a transparent outer canvas and no external shadow or matte. Reusable source: [owl SVG](../assets/branding/weave-owl.svg). Android source: `app/src/main/res/drawable/weave_owl.xml`. No upscaled bitmap is used in the shipped icon or splash.

The full-color adaptive icon and a separate monochrome owl layer fit inside the circular adaptive safe zone. Android 13+ launchers can use the monochrome layer when themed icons are enabled. AndroidX SplashScreen provides the owl launch screen on Android 10 and the native system splash on Android 12+; it waits for the initial library read, without an artificial delay.

Material 3 DynamicColors supplies wallpaper colors on supported Android 12+ devices. Library, editor, fullscreen palettes, dialogs, inputs, selections, status and export chrome resolve theme attributes instead of fixed teal colors. Light/dark sage fallback themes match the owl on older devices. Notebook paper and saved ink colors remain stable, including exported PDFs. The TCL tablet runs Android 10 and therefore uses the fallback rather than OS wallpaper colors.

## Verification

Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` with JDK 17 and SDK 34. Device tests use `androidx.test.runner.AndroidJUnitRunner`. Synthetic input checks do not substitute for human passive-stylus comfort testing.

The instrumentation covers:

- Two-finger pan and pinch anchoring, no committed/resumed ink after navigation.
- Eraser preview before lift, cancellation rollback, and one completed undo/redo operation.
- Every pen/eraser preset remaining selectable, including restoring the default width.
- Fullscreen entry/exit, hidden Hold shapes/zoom strip, side controls and rotation.
- Transparent vector-logo bounds and circular mask safety at 4× icon resolution.
- Theme attribute propagation with a non-brand accent and Android 14 system color-role matching; the framework exposes the monochrome adaptive icon.
- Existing SQLite recovery, A4 PDF, cancellation and editable backup/restore checks.

Final run logs and screenshots are under `docs/evidence/m26`. Only synthetic QA notebooks and the clean emulator library are included in screenshots; existing tablet notes are excluded. Temporary notebooks are removed by exact generated UUIDs in a test finally block, and the tablet's pre-test writing preferences are restored before handoff.

### Final results — 26 October 2026

- JDK 17 build: debug APK and test APK assembled; all 94 JVM tests passed; lint reports zero errors (warnings remain).
- Physical TCL TAB 10s / Android 10: 11 instrumentation tests passed; the Android 13+ system-palette/monochrome test was skipped by an SDK assumption. The runner reports 12 total tests, including that skip.
- Dedicated Android 14 emulator: all 12 instrumentation tests passed in dark mode; all three theme/asset tests also passed in light mode. The app primary matches Android 14's system primary role, and the adaptive icon exposes its monochrome layer.
- Compared all pre-existing tablet notebook rows, page order and stroke payloads before/after QA: unchanged. Generated QA UUIDs are gone. Restored the tablet's original writing preferences.

Screenshots: [normal editor](evidence/m26/weave-editor-landscape.png), [fullscreen landscape](evidence/m26/weave-fullscreen-landscape.png), [fullscreen portrait](evidence/m26/weave-fullscreen-portrait.png), [normal portrait](evidence/m26/weave-editor-portrait.png), [eraser before lift](evidence/m26/weave-eraser-before-lift.png), [Material You library](evidence/m26/material-you-library-light.png), [recorded native splash](evidence/m26/splash-android14.png), [themed icon, light](evidence/m26/material-you-icon-light.png), [themed icon, dark](evidence/m26/material-you-icon-dark.png), and [transparent owl render](evidence/m26/weave-owl-vector-render.png).
