# PR 03 evidence — A4 model and transforms

## Dependency / merge order
- Head: `milestone/03-a4-model`
- Base: `milestone/02-input-probe`
- Merge order: **3 of 24**

## Tests
```
./gradlew testDebugUnitTest lintDebug assembleDebug
```
Round-trip error &lt; 0.01 mm; zoom-anchor invariance; A4 aspect across 1200×1920 and 1920×1200.
