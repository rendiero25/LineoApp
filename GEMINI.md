# GEMINI.md

All project instructions live in **AGENTS.md**. Read it before doing anything.

@./AGENTS.md

## Gemini CLI specifics

- `.gemini/settings.json` already lists `AGENTS.md` first in `context.fileName`, so
  this file exists only as a fallback for tools that ignore that setting.
- Be conservative: this repo prefers small, reviewable diffs over sweeping refactors.
  Do not reformat or restructure files you were not asked to change.
- Do not add dependencies. See AGENTS.md §2.
