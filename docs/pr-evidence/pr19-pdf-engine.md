# PR 19 evidence — PDF rendering engine

## Dependency / merge order
- Head: `milestone/19-pdf-engine`
- Base: `milestone/18-lifecycle`
- Merge order: **19 of 24**

A4 MediaBox approx 595×842 pt (&lt;0.2 mm error). Full PdfDocument render comparison requires device/instrumented JVM with android.graphics — unit tests cover bounds math.
