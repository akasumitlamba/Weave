# AGENTS.md — Weave

## Scope

Build and maintain Weave, an offline Android handwriting notebook.
Follow `/docs` handoff requirements when present. Prefer one `:app` module
with clear packages over elaborate modularization.

## Hard rules

- Kotlin + Android Views + custom Canvas drawing View only
- No WebView, Flutter, React Native, game engines, or heavy drawing frameworks
- No login, ads, analytics, cloud, OCR, AI, or network permission
- Keep note content on device
- minSdk 29 with Android 10-compatible ink path fallbacks

## PR workflow

- One numbered milestone per PR (`milestone/NN-…`)
- Stacked dependent branches; never merge, never push `main`
- Never squash/rebase published branches or combine numbered PRs
- Each PR must build, include scoped tests/evidence, and state merge order
