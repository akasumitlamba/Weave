# PR 13 evidence — Transactional persistence

## Dependency / merge order
- Head: `milestone/13-persistence`
- Base: `milestone/12-polygon-shape-undo`
- Merge order: **13 of 24**

## Behavior
- Versioned stroke codec (schema v1)
- Serialized SaveQueue with Saving / Saved / Save failed + Retry
- InMemoryNoteStore as authoritative model cache (SQLite journal wiring continues in library/backup PRs)

## Device / process-death
Instrumented process-death **NOT RUN** in this environment.
