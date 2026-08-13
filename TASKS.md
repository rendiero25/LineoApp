# TASKS.md — Lineo build plan

Ordered work breakdown. One task is meant to be one focused session.

Read `AGENTS.md` before picking up anything here.

---

## How to use this file

**Status markers**

- `[ ]` not started
- `[~]` in progress
- `[x]` done, tests green, merged
- `[!]` blocked — add a one-line reason inline

**Task anatomy**

Each task lists the module it touches, what it depends on, and its definition of
done. A task is finished only when every DoD line is true — not when the code
compiles.

**Rules**

1. Do not start a task whose dependencies are unfinished.
2. One task = one branch = one PR. Do not bundle.
3. If a task turns out to need edits in two modules, stop and split it.
4. If a task requires a decision listed in `AGENTS.md` §7, stop and ask.
5. Complexity labels are `S` (single sitting), `M` (a day), `L` (multi-day, expect
   to split it further once you start).

---

## Phase 0 — Foundations

Nothing ships. Goal: an engine that cannot be broken silently.

### Build infrastructure

- [x] **P0-01 · Project skeleton** — `S` — `:app`
  - Empty Activity (Compose) template, `app.lineo`, minSdk 26
  - Delete generated `MainActivity` and `ui.theme`
  - Repo root contains `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.gemini/`, `docs/`
  - **DoD:** `./gradlew assembleDebug` succeeds on a clean checkout

- [x] **P0-02 · Convention plugins + version catalog** — `M` — `build-logic`
  - `build-logic` with `lineo.android.application`, `lineo.android.library`,
    `lineo.jvm.library`, `lineo.android.compose`
  - All versions in `gradle/libs.versions.toml`. No inline version strings anywhere
  - **DoD:** adding a new module requires ≤ 5 lines in its `build.gradle.kts`

- [x] **P0-03 · Static analysis + CI** — `S` — repo
  - detekt with ktlint formatting, dependency licence check task
  - GitHub Actions: build, test, detekt on every PR
  - **DoD:** CI fails on a deliberately introduced style violation and on a GPL dependency

### `:core:engine` — the heart

Depends on P0-02. Pure JVM library, no Android imports.

- [x] **P0-04 · Test harness first** — `M` — `:core:engine`
  - Golden file runner reading `src/test/resources/golden/*.txt`, format per
    `docs/GRAMMAR.md` §5, including `locale=` directives and `!ErrorType@span`
  - `EngineFuzzTest`: random input, asserts no throw, 1 s timeout per case
  - **DoD:** harness runs against a stub evaluator and reports failures with the
    offending golden line number
  - *Written before the engine on purpose. It defines what "correct" means.*

- [x] **P0-05 · Quantity and units** — `M` — `:core:engine`
  - `Quantity(BigDecimal, UnitTerm?)`, `UnitTerm` with base-dimension exponents
  - Arithmetic: add/sub require dimension match, mul/div compose dimensions
  - Affine temperature handled as a distinct kind (`docs/GRAMMAR.md` §3.8)
  - `MathContext.DECIMAL128` throughout. No `Double` in any result path
  - **DoD:** golden cases for `5 km + 300 m`, `2 h * 60 km/h`, `20°C + 5°C` (rejected),
    `0.1 + 0.2 = 0.3`

- [x] **P0-06 · Lexer** — `M` — `:core:engine`
  - Locale-aware decimal separator, argument separator, grouping (ignored on parse)
  - Indian grouping, magnitude suffixes, scientific notation per `docs/GRAMMAR.md` §3.4
  - Every token carries a source span
  - **DoD:** golden cases pass for `en-US`, `id-ID`, `de-DE`, `hi-IN`

- [x] **P0-07 · Pratt parser + AST** — `L` — `:core:engine`
  - Binding powers derived directly from the precedence table in `docs/GRAMMAR.md` §1
  - Implicit multiplication, right-associative `^`, postfix `%` `!` `°`
  - Every AST node carries a span
  - **DoD:** every row of `docs/CONVENTIONS.md` §4 has a passing golden case;
    fuzz test green

- [ ] **P0-08 · Evaluator + error model** — `L` — `:core:engine`
  - `EvalContext`: variables, previous results, angle mode, function registry
  - Full `CalcError` hierarchy per `docs/ARCHITECTURE.md` §3
  - `UnknownIdentifier` produces a nearest-match suggestion (edit distance)
  - Identifier resolution order per `docs/GRAMMAR.md` §3.1
  - **DoD:** `evaluate()` never throws; one golden case per error variant with span assertion

- [ ] **P0-09 · Built-in functions and constants** — `M` — `:core:engine`
  - Scientific set: trig + inverse + hyperbolic, log/ln/exp, roots, `abs`, `round`,
    `floor`, `ceil`, `mod`, `gcd`, `lcm`, factorial, `nPr`, `nCr`
  - Constants `pi`, `e`, `phi`; angle mode respected and visible
  - **DoD:** golden case per function including at least one domain error each

### Core plumbing

- [ ] **P0-10 · `:core:registry`** — `S`
  - `CalculatorModule`, `CalcFunction`, `Tier`, `LocaleGate`, `EditorCommand`,
    `InputSurface` per `docs/ARCHITECTURE.md` §4–5
  - Hilt multibinding wiring in `:app`
  - **DoD:** a dummy module registers a function that the engine can then evaluate by name

- [ ] **P0-11 · `:core:data`** — `M`
  - Room schema per `docs/ARCHITECTURE.md` §6, exported schemas committed
  - `schemaVersion` on documents and formulas
  - DataStore for settings: locale override, angle mode, theme, separator
  - **DoD:** `MigrationTestHelper` test passes for v1; a corrupt DB enters recovery
    mode instead of crashing

- [ ] **P0-11b · Adaptive shell + edge-to-edge** — `M` — `:app`, `:core:ui`
  - `WindowSizeClass` drives layout; no device-type or width branching
  - Edge-to-edge with correct insets; keypad consumes `ime` + `navigationBars` without
    double padding
  - Predictive back opted in; back never discards notepad work
  - **DoD:** verified with gesture and 3-button nav, display cutout, split-screen, and a
    foldable emulator across a fold

- [ ] **P0-12 · `:core:ui` design system** — `M`
  - Dynamic color on API 31+, generated seed palette fallback below
  - Token mapping for the keypad per `docs/CONVENTIONS.md` §10; true-black variant stubbed
  - Typography with tabular figures; Android 14+ contrast levels honoured
  - **DoD:** Paparazzi snapshots in light, dark, and RTL for the token showcase screen

- [ ] **P0-13 · Keypad and accessory row** — `M` — `:core:ui`
  - Both implement `InputSurface`, emitting `EditorCommand` only
  - Accessory row docks above the system IME using `WindowInsets.ime`
  - **DoD:** verified against GBoard, SwiftKey, and Samsung Keyboard; no layout jump

- [ ] **P0-14 · Expression editor** — `L` — `:core:ui`
  - Consumes `EditorCommand`, renders result inline
  - Error underline at `CalcError.span`, message below the line, tap-to-fix chip
  - 400 ms debounce; incomplete input shows nothing
  - **DoD:** typing `5 +` shows no error state; `sni(1)` shows a suggestion chip

**Phase 0 exit criteria:** engine golden suite and fuzz test green in CI; editor
evaluates a single line end to end.

---

## Phase 1 — First release

- [ ] **P1-01 · Notepad document model** — `M` — `:feature:notepad`
  - Stable `LineId`; `ordinal` used only for display
  - Reference rewriting on insert/delete/reorder without repointing
  - **DoD:** inserting a line above line 3 leaves `line3` pointing at the same content

- [ ] **P1-02 · Incremental evaluation** — `L` — `:feature:notepad`
  - Dependency graph, topological evaluation, cycle detection marking all lines in the cycle
  - Only dependents of a changed line recompute
  - **DoD:** 200-line document, keystroke to result under 50 ms on an API 26 emulator

- [ ] **P1-03 · Notepad UI + hybrid input** — `L` — `:feature:notepad`
  - Keypad default; `Aa` raises system keyboard
  - Context auto-switch: line starting with a digit → keypad, with a letter → keyboard
  - Suggestion chips: in-scope variables, recent units
  - **DoD:** switching surfaces preserves cursor position; no visible layout jump

- [ ] **P1-04 · `:feature:scientific`** — `M`
  - Registers its functions through `CalcFunction`, not in the Screen
  - Landscape expands to the full scientific keypad
  - **DoD:** every function is callable as text in notepad mode

- [ ] **P1-05 · `:feature:converter`** — `M`
  - Length, mass, volume, area, speed, temperature, data, time, pressure, energy
  - Registers `UnitDefinition`s so `5 km to mi` works in notepad
  - **DoD:** ISO 80000 symbols; IEC binary prefixes correct (`KiB` = 1024)

- [ ] **P1-06 · History** — `S` — `:app`
  - Tape view, tap to reuse, capped at 50 for free tier
  - **DoD:** survives process death

- [ ] **P1-07 · Settings** — `S` — `:app`
  - Separator override, angle mode, theme, unit system, decimal places
  - **DoD:** changing separator updates the keypad immediately

- [ ] **P1-08 · Accessibility pass** — `M` — all
  - TalkBack reads `2^3` semantically from the AST
  - RTL verified with `ar-XB`; 48 dp minimum targets; no colour-only meaning
  - **DoD:** full task flow completable with TalkBack only

- [ ] **P1-09 · Localisation plumbing** — `S`
  - No hardcoded user-facing strings; `en` base complete
  - Pseudo-locale `en-XA` shows no truncation
  - **DoD:** lint rule fails the build on a hardcoded string

- [ ] **P1-08b · Font scaling and large-screen pass** — `S` — all
  - Maximum system font scale causes no clipping in display or keypad
  - Hardware keyboard: full entry, arrow keys between lines, Enter for new line
  - **DoD:** full flow completable on a tablet with a hardware keyboard

- [ ] **P1-10 · Performance pass** — `M`
  - Baseline Profile + Startup Profile from a Macrobenchmark journey covering cold start
    and typing a document
  - R8 full mode; release build tested, not just debug
  - `reportFullyDrawn()` called; Macrobenchmark wired into CI
  - Room: `lines.documentId` indexed; no synchronous main-thread reads
  - **DoD:** cold start under 500 ms on an API 26 device; APK under 12 MB; no dropped
    frames while typing

- [ ] **P1-10b · Security and privacy pass** — `M` — `:app`
  - Permission list is exactly `INTERNET` + `ACCESS_NETWORK_STATE`
  - Network security config forbids cleartext; hostile/malformed rate payload yields
    `RateUnavailable`, never a crash or a silently wrong rate
  - Cloud auto-backup excluded unless the user opts in
  - **DoD:** `ANDROID_STANDARDS.md` §7 checklist passes end to end

- [ ] **P1-11 · Store readiness** — `M` — non-code
  - Privacy policy live; Data Safety form; icon, feature graphic, screenshots
  - Title `Lineo: Notepad Calculator`; keystore backed up in two places
  - **DoD:** internal testing track accepts an upload

- [ ] **P1-12 · Closed testing** — `L` — non-code
  - 12 testers, 14 continuous days (personal accounts)
  - **DoD:** production access granted; crash-free sessions above 99.5%

- [ ] **P1-13 · Release 1.0** — staged rollout 5% → 20% → 50% → 100%

---

## Phase 2 — Monetisation and network

Do not start before 1.0 has two weeks of stable data.

- [ ] **P2-01** `:core:billing` — Play Billing, entitlement gate, restore purchases
- [ ] **P2-02** Premium gating across features; free tier stays genuinely usable
- [ ] **P2-03** Tip jar — three consumable tiers, Supporter badge
- [ ] **P2-04** UMP consent SDK — must ship before any ad request
- [ ] **P2-05** AdMob native placements only, per `docs/SPEC.md` §5; 3-day install grace.
  Every placement gated behind Remote Config so it can be disabled without a release.
  Re-verify the Data Safety declaration against actual SDK behaviour
- [ ] **P2-06** Rewarded unlocks — 24 h premium theme, extra notepad lines
- [ ] **P2-07** `:feature:currency` — device-side fetch, Room cache, stale-rate badge, offline first
- [ ] **P2-08** `:feature:finance` — loan schedules, compound interest, discount, tip
- [ ] **P2-09** `:pack:tax-*` — locale-gated regional packs
- [ ] **P2-10** `:feature:datetime` — differences, age, working days with per-country holidays
- [ ] **P2-11** Custom keypad as full `InputSurface` (the Phase 1 → 2 input upgrade)
- [ ] **P2-12** Localisation wave 1 — ID, ES, PT-BR, HI
- [ ] **P2-13** In-app updates; JankStats in production; vitals dashboard reviewed weekly

---

## Phase 3 — Depth

- [ ] **P3-01** `:feature:formula` — user-defined formula builder *(strongest purchase trigger)*
- [ ] **P3-02** Graphing — plot, pan, zoom, trace
- [ ] **P3-03** Statistics — descriptive, regression, combinatorics
- [ ] **P3-04** Matrices and vectors via EJML
- [ ] **P3-05** Theme seed picker, true-black AMOLED
- [ ] **P3-06** Notepad folders, search, templates gallery
- [ ] **P3-07** Tablet and foldable two-pane layout; hardware keyboard support
- [ ] **P3-08** Localisation wave 2 — DE, RU, JA

---

## Phase 4 — Reach

- [ ] **P4-01** Glance home screen widget
- [ ] **P4-02** Quick Settings tile and floating bubble
- [ ] **P4-03** CSV/PDF export; Drive App Data backup
- [ ] **P4-04** Share result as image
- [ ] **P4-05** Symbolic derivative with visible steps *(in-house AST rules)*
- [ ] **P4-06** ML Kit OCR scan — beta
- [ ] **P4-07** Compose Multiplatform exploration

---

## Global definition of done

Applies to every code task:

- [ ] Unit tests written and passing; golden cases added for engine behaviour
- [ ] `./gradlew test lint detekt` green
- [ ] Paparazzi snapshots updated if UI changed, including RTL
- [ ] No new dependency outside Apache-2.0 / MIT / BSD
- [ ] No user expression content sent to analytics or crash reporting
- [ ] No hardcoded user-facing strings
- [ ] Conventional Commit, scoped by module
- [ ] Doc updated if a decision in `docs/` changed

---

## Blocked and decisions log

Record anything that stopped work and how it was resolved. This is what prevents the
same question being re-litigated in a future session.

| Date | Task | Question | Decision |
|---|---|---|---|
| open | P0-01 | Navigation Compose or Navigation 3? Google now points multi-screen apps at Nav 3; `ARCHITECTURE.md` §1 assumes Navigation Compose | **Unresolved — decide before P1-10 (app shell)** |
| open | P3-01 | Encrypt the Room database at rest? Notepad documents contain salaries and debts | **Unresolved — decide before Phase 3** |
| 2026-08-13 | P0-03 | detekt 1.23.8 cannot run on JDK 25 — its bundled Kotlin compiler fails to parse the version string | detekt runs through its CLI, forked onto the Java 17 toolchain. The rest of the build stays on the daemon JVM |
| 2026-08-13 | P0-03 | The licence gate cannot read licences from POMs deterministically without network access | Allowlist instead: `config/licenses/allowed-dependencies.txt` records every module and its licence, and an unlisted dependency fails the build — which is also what §7 wants, since adding one is a human decision |
| open | P0-03 | `junit:junit` is EPL-1.0, outside the Apache-2.0 / MIT / BSD rule of §2, but JUnit is approved by name in `docs/ARCHITECTURE.md` §7 | Recorded in the allowlist as test-only, marked EPL. **Needs a human confirmation** |
| 2026-08-13 | P0-04 | Should a golden expectation be the locale-formatted result or the canonical one? | Canonical and locale-free. `locale=` selects how the input is read; the expectation asserts engine behaviour, not display formatting (`docs/CONVENTIONS.md` §1) |
| 2026-08-13 | P0-06 | In dot-decimal locales `,` is both the grouping and the argument separator, so `max(1,5)` is ambiguous | `,` counts as grouping only when it is followed by a full group of digits — three, or two or three for Indian grouping. Otherwise it is the argument separator. `1,234` is a number, `max(1,5)` is two arguments |
| 2026-08-13 | P0-07 | `of` has no level in the `docs/GRAMMAR.md` §1 precedence table | Parsed at the multiplicative level, so `50% of 80 + 10` is `50` |
