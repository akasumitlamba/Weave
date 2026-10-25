# Weave 24-PR review index

**Connecting ideas together**

Merge with **Create a merge commit** only, ascending order. Never squash/rebase published branches. Sumit retains sole merge authority. No automatic merges.

## Starting main SHA (merge simulation)
`100eceb50e8f2ad316e8cd417aa8129b61de7c2a`

## Ordered merge simulation
Temporary worktree merged PR 01→22 heads with `--no-ff` against that main: **no conflicts**.
Simulated result SHA (not pushed): `fa1d81ec2ae0697f5ee0692b5b31917952450865`
PR 23 is a fast-forward ancestor of this handoff branch; include it in Sumit’s live merge after CI green.

## Pull requests

| PR | Head | Base | URL | Head SHA |
|----|------|------|-----|----------|
| 01 | milestone/01-bootstrap | main | https://github.com/akasumitlamba/Weave/pull/1 | 43fb104 |
| 02 | milestone/02-input-probe | milestone/01-bootstrap | https://github.com/akasumitlamba/Weave/pull/2 | 756f499 |
| 03 | milestone/03-a4-model | milestone/02-input-probe | https://github.com/akasumitlamba/Weave/pull/3 | 5357c18 |
| 04 | milestone/04-editor-shell | milestone/03-a4-model | https://github.com/akasumitlamba/Weave/pull/4 | 0f6877d |
| 05 | milestone/05-raw-ink | milestone/04-editor-shell | https://github.com/akasumitlamba/Weave/pull/5 | 55650b7 |
| 06 | milestone/06-gesture-ownership | milestone/05-raw-ink | https://github.com/akasumitlamba/Weave/pull/6 | 229e373 |
| 07 | milestone/07-palm-guards | milestone/06-gesture-ownership | https://github.com/akasumitlamba/Weave/pull/7 | 4053b9c |
| 08 | milestone/08-smoothing | milestone/07-palm-guards | https://github.com/akasumitlamba/Weave/pull/8 | 7de0e0f |
| 09 | milestone/09-tools-brushes | milestone/08-smoothing | https://github.com/akasumitlamba/Weave/pull/9 | 7b2e15d |
| 10 | milestone/10-commands-eraser | milestone/09-tools-brushes | https://github.com/akasumitlamba/Weave/pull/10 | 6bd9c00 |
| 11 | milestone/11-shape-hold-basic | milestone/10-commands-eraser | https://github.com/akasumitlamba/Weave/pull/11 | 73a6de1 |
| 12 | milestone/12-polygon-shape-undo | milestone/11-shape-hold-basic | https://github.com/akasumitlamba/Weave/pull/12 | 1be2639 |
| 13 | milestone/13-persistence | milestone/12-polygon-shape-undo | https://github.com/akasumitlamba/Weave/pull/13 | 42327d3 |
| 14 | milestone/14-notes-library | milestone/13-persistence | https://github.com/akasumitlamba/Weave/pull/14 | efc6b75 |
| 15 | milestone/15-page-management | milestone/14-notes-library | https://github.com/akasumitlamba/Weave/pull/15 | 52ec7c3 |
| 16 | milestone/16-backup-restore | milestone/15-page-management | https://github.com/akasumitlamba/Weave/pull/16 | c077d28 |
| 17 | milestone/17-zoom-strip | milestone/16-backup-restore | https://github.com/akasumitlamba/Weave/pull/17 | e5e5dff |
| 18 | milestone/18-lifecycle | milestone/17-zoom-strip | https://github.com/akasumitlamba/Weave/pull/18 | 26ba691 |
| 19 | milestone/19-pdf-engine | milestone/18-lifecycle | https://github.com/akasumitlamba/Weave/pull/19 | a5da93d |
| 20 | milestone/20-export-ui | milestone/19-pdf-engine | https://github.com/akasumitlamba/Weave/pull/20 | 0e24c01 |
| 21 | milestone/21-rendering-perf | milestone/20-export-ui | https://github.com/akasumitlamba/Weave/pull/21 | 586c868 |
| 22 | milestone/22-integration-suite | milestone/21-rendering-perf | https://github.com/akasumitlamba/Weave/pull/22 | 4463d22 |
| 23 | milestone/23-usability-a11y | milestone/22-integration-suite | https://github.com/akasumitlamba/Weave/pull/23 | e1b890a |
| 24 | milestone/24-release-handoff | milestone/23-usability-a11y | (this PR) | (see commit) |

## Device gates (all honest)
| Gate | Status |
|------|--------|
| TCL TAB 10s input identity / palm | **NOT RUN** |
| Ink latency / Perfetto | **NOT RUN** |
| Shape corpus accuracy on device | **NOT RUN** |
| Process-death after Saved | **NOT RUN** (unit coverage only) |
| PDF visual comparison / print preview | **NOT RUN** |
| 30-minute stability / PSS memory | **NOT RUN** |
| TalkBack / font scale 1.3× | **NOT RUN** |
| Sumit comfort acceptance | **pending** |

## Debug APK
Built at handoff head: see CI artifact `weave-debug-apk` and local evidence path under agent media when available.

## Known limitations
- Passive stylus may report as finger; guards are explicit touch blockers, not hardware rejection.
- SQLite on-disk journal is modeled via codec + save queue; expand Room wiring before production release if required.
- Zoom strip model/mapping shipped; full dual-viewport chrome can be polished on tablet.
- Shape recognition thresholds need tablet tuning; engineering targets are not measured claims.
