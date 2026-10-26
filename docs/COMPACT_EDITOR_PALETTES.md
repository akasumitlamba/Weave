# Milestone 27 — Compact editor palettes

Merge order: #25 → #26 → this PR. Base: milestone/26-gesture-fullscreen-editor. Main remains unchanged.

## Layout

- Remove the bottom bar entirely. Fit page remains available in the Notebook menu.
- Keep icon-only back and fullscreen/exit-fullscreen actions at the top, above the writing area.
- Right floating tray (48 dp wide): pen, marker, highlighter, eraser, one thickness dropdown, and one color dropdown. Width rows share a consistent layout with numeric millimetres and stroke previews; each tool retains its own physically meaningful size range and default.
- Left floating utility tray: icon-only undo/redo and a page-preview toggle. The preview rail starts collapsed, overlays the workspace without shrinking or shifting the canvas, persists across rotation, and supports tapping a page or holding it for page actions.
- Generated transparent tool-tip illustrations match the supplied reference and share a common display scale; colors use an outlined circular swatch, while thickness uses a numeric dropdown.
- Notebook menu includes paper, export, page actions, fit, optional hold-shape toggle, and smoothing. Paper selection includes native rendered previews. Palm guard / writing-window controls are removed from the menu and legacy palm-guard blocking is disabled.
- System bar and display-cutout insets are consumed explicitly in both normal and immersive mode, including when transient system bars appear.

## Visual reference

Apple's Notes drawing guide: https://support.apple.com/en-gb/108919. Uses compact instrument selection with on-demand thickness/color controls as inspiration; tool illustrations are generated with the built-in image_gen tool against the supplied style reference, then packaged locally. Prompts and asset paths are documented in TOOL_ASSET_PROMPTS.md.

## Distribution

Version code 4, version name 0.4.0-editor. Debug APK retains com.weave.notebook.debug and the existing local signing identity for updates without uninstalling.

## Validation

- JVM tests, lintDebug, debug app and instrumentation APK builds pass.
- All 12 Android 14 instrumented checks pass, including actual pen width / eraser dropdown selection, live erase, undo/redo, save/reopen, page collapse, paper previews, export, fullscreen, rotation, and system-inset/canvas-bottom assertions.
- Thumbnail rule lines retain a minimum screen-pixel weight for legible paper previews; editor/PDF line geometry stays unchanged.
- Synthetic screenshots and Android 14 test output are under docs/evidence/m27. No user notebook content is included.

## Tablet testing coordination

The owner confirmed another chat was testing another app on the connected TCL tablet. Stopped further tablet automation and installations after that clarification. Final generated-icon / floating-tray build is validated in the dedicated Weave Android 14 emulator; final tablet installation is deferred until the shared device is free. Earlier instrument attempts were interrupted and do not count as a passed final tablet test.

- Additional Android 14 dark-theme editor interaction check passes with the generated assets and floating trays. See editor-dark.png.
