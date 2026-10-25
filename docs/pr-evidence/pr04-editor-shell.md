# PR 04 evidence — Responsive editor shell

## Dependency / merge order
- Head: `milestone/04-editor-shell`
- Base: `milestone/03-a4-model`
- Merge order: **4 of 24**

## Layouts
- `layout/activity_editor.xml` — portrait two-row chrome
- `layout-land/activity_editor.xml` — landscape top chrome
- Library shows **Weave** + **Connecting ideas together**
- Controls use `@dimen/min_touch_target` (48dp)

## Tests
```
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Device / configuration
- Rotation recreation: state saved in `EditorActivity.onSaveInstanceState` — instrumented **NOT RUN**
- Manual 1200×1920 / 1920×1200: **NOT RUN** (no tablet); layout resources present for both orientations
