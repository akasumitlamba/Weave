# PR — Horizontal editor tool rail

## Summary
Editor tool selection (Pen, Marker, Highlighter, Eraser, Hand) is a **horizontal strip** along the top edge of the writing surface in both portrait and landscape — matching handoff UI refs. Not a vertical sidebar column.

## Changes
- `res/layout/activity_editor.xml` and `layout-land/activity_editor.xml`: `@+id/toolRail` with `orientation=horizontal`, equal-weight chips
- Compact chip background `tool_chip_bg.xml`; selection via `isSelected`
- Long-press on a tool cycles stroke width (hold-for-options) without blocking tap select
- Version bump: `versionCode 2`, `versionName 0.1.1`
- Contract test: `ToolRailLayoutContractTest`

## Verification
```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```
BUILD SUCCESSFUL

## Device
Tablet screenshot on TCL TAB 10s: **NOT RUN** (no device). Layout diagrams + debug APK under agent media store.
