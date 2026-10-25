# PR 05 evidence — Raw ink pipeline

## Dependency / merge order
- Head: `milestone/05-raw-ink`
- Base: `milestone/04-editor-shell`
- Merge order: **5 of 24**

## Behavior
- Pointer-ID ownership; historical MOVE samples appended in order
- CANCEL clears provisional ink
- Bounded transient buffer
- `InkCanvasView` overlays live stroke on page renderer

## Tests
```
./gradlew testDebugUnitTest lintDebug assembleDebug
```
Replay: fast curves, dots, out-of-page, CANCEL.

## Frame baseline
**NOT RUN** on device — no Perfetto capture in this environment.
