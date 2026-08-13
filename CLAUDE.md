# CLAUDE.md

All project instructions live in **[AGENTS.md](./AGENTS.md)**. Read it before doing anything.

@./AGENTS.md

## Claude Code specifics

- Use plan mode for anything touching `:core:engine`. The grammar decisions in
  `docs/GRAMMAR.md` are load-bearing — propose before editing.
- Prefer editing existing files over creating new ones. This repo has a fixed module
  layout; a new file usually means the work belongs in a different module.
- When a task spans modules, use subagents — one per module — rather than one agent
  editing across boundaries.
- Run `./gradlew :core:engine:test` after every engine edit, not just at the end.
- Do not create summary markdown files describing what you did. Report in chat.
