# Milestone 27 — Compact editor palettes

Merge order: #25 → #26 → this PR. Base: milestone/26-gesture-fullscreen-editor. Main remains unchanged.

## Layout

- Remove the bottom bar entirely. Fit page remains available in the Notebook menu.
- Keep icon-only back and fullscreen/exit-fullscreen actions at the top, above the writing area.
- Right rail: pen, marker, highlighter, eraser, one thickness dropdown, and one color dropdown. Width rows share a consistent layout with numeric millimetres and stroke previews; each tool retains its own physically meaningful size range and default.
- Left rail: icon-only undo/redo and a page-preview toggle. The preview rail starts collapsed, persists across rotation, and supports tapping a page or holding it for page actions.
- Themed instrument silhouettes share a common scale; colors use an outlined circular swatch, while thickness uses a numeric dropdown.
- Notebook menu includes paper, export, page actions, fit, optional hold-shape toggle, and smoothing. Paper selection includes native rendered previews. Palm guard / writing-window controls are removed from the menu and legacy palm-guard blocking is disabled.
- System bar and display-cutout insets are consumed explicitly in both normal and immersive mode, including when transient system bars appear.

## Visual reference

Apple's Notes drawing guide: https://support.apple.com/en-gb/108919. Uses compact instrument selection with on-demand thickness/color controls as inspiration; all shipped instrument artwork is original native Canvas drawing.

## Distribution

Version code 4, version name 0.4.0-editor. Debug APK retains com.weave.notebook.debug and the existing local signing identity for updates without uninstalling.

## Validation

- JVM tests, lintDebug, debug app and instrumentation APK builds pass.
- All 12 Android 14 instrumented checks pass, including actual pen width / eraser dropdown selection, live erase, undo/redo, save/reopen, page collapse, paper previews, export, fullscreen, rotation, and system-inset/canvas-bottom assertions.
- Thumbnail rule lines retain a minimum screen-pixel weight for legible paper previews; editor/PDF line geometry stays unchanged.
- Synthetic screenshots and Android 14 test output are under docs/evidence/m27. No user notebook content is included.
