# Tool palette artwork

Generated with the built-in image_gen tool using the owner's supplied compact tool-tip toolbar image as a **style reference**, not an edit target. The app contains only local PNG assets; image generation is a development step, never an app feature or network dependency.

Final assets: `app/src/main/res/drawable-nodpi/writing_{pen,marker,highlighter,eraser}.png`.

Packaging: preserve generated alpha, trim transparent clearance, downsample with Lanczos, center on a 224 × 304 transparent canvas. Runtime size: 26 × 36 dp. No theme tint is applied to instrument materials; tray, selection, menus, and controls resolve Material theme colors.

## Shared final generation prompt

Use case: stylized-concept. Create a production UI asset for a tablet handwriting toolbar. Input image is STYLE REFERENCE ONLY: reproduce its simple clean small tool-tip visual language, NOT its entire toolbar. Output only ONE compact instrument TIP ICON on actual transparent alpha. Flat graphic illustration with subtle gray edge shading, extremely clean geometry, like the small pen tips in the reference. Do NOT draw a complete real-world pen, do NOT draw a rounded capsule or a product photograph. Short barrel is a rectangular flat-cut top with a nib tapering downward. Front view, vertical, tip points DOWN, centered. Icon occupies 72% of a square canvas's height and 36% width, crisp edges at small 24x36 pixels. No text/letters/numbers/logos, tile, border, background, shadow or extra objects.

Append one subject per call:

- Pen: ONE fine PEN TIP: light silver rectangular short barrel with two thin vertical gray grooves, black tapered collar, tiny black fine nib. Match the selected silver tool shape in reference, without any letters.
- Marker: ONE broad MARKER TIP: black rectangular short barrel with a muted sage-green top color band, black tapered collar, broad black felt nib. Match the reference's simple marker silhouette.
- Highlighter: ONE HIGHLIGHTER TIP: bright soft yellow rectangular short wide barrel, black tapered collar, yellow angled flat chisel nib. Match the rightmost highlighter from the reference.
- Eraser: ONE ERASER TOOL: cream rectangular short eraser with black graphite sleeve around middle and exposed pale lilac rubber at the bottom. Match the leftmost eraser in the reference. Bottom is flat rounded rubber, not a pointed nib.

## Eraser cleanup prompt

Use case: background-extraction. Edit target is this eraser tool icon. Remove ALL background, ALL shadow, ALL glow, ALL halo around the eraser. Keep ONLY the eraser instrument silhouette (cream flat rectangular top, black sleeve middle, lilac rounded rubber bottom), preserving its clean illustration. Output genuine transparent alpha outside the exact eraser edge, including between object and canvas border. No black background, no white background, no gradient matte, no checkerboard. No drop shadow or rim glow. Crisp antialiased contour. Center the isolated eraser on square transparent canvas. No other objects or changes.
