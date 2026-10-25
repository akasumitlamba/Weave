# PR 02 evidence — Device input probe

## Dependency / merge order
- Head: `milestone/02-input-probe`
- Base: `milestone/01-bootstrap`
- Merge order: **2 of 24** (after PR 01; retarget to `main` only after PR 01 merges)

## Tests
```
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Device findings
**NOT RUN / UNVERIFIED** — no TCL TAB 10s attached.

### Capture steps
See `InputProbeActivity.CAPTURE_STEPS` and debug Input Probe screen.
Record whether passive pen reports `TOOL_TYPE_STYLUS` or only `TOOL_TYPE_FINGER`.
