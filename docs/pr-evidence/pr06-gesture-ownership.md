# PR 06 evidence — Gesture ownership

## Dependency / merge order
- Head: `milestone/06-gesture-ownership`
- Base: `milestone/05-raw-ink`
- Merge order: **6 of 24**

## Behavior
- Write vs Navigate modes via `GestureArbiter`
- Second touch cancels provisional ink; suppress until full lift
- Navigate: one-finger pan, two-finger pinch; never creates ink
- Zoom buttons remain available in editor chrome

## Tests
Unit: pointer IDs, one-to-two cancel, full-lift reset, navigate no-ink.
