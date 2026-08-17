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

- [x] **P0-08 · Evaluator + error model** — `L` — `:core:engine`
  - `EvalContext`: variables, previous results, angle mode, function registry
  - Full `CalcError` hierarchy per `docs/ARCHITECTURE.md` §3
  - `UnknownIdentifier` produces a nearest-match suggestion (edit distance)
  - Identifier resolution order per `docs/GRAMMAR.md` §3.1
  - **DoD:** `evaluate()` never throws; one golden case per error variant with span assertion

- [x] **P0-09 · Built-in functions and constants** — `M` — `:core:engine`
  - Scientific set: trig + inverse + hyperbolic, log/ln/exp, roots, `abs`, `round`,
    `floor`, `ceil`, `mod`, `gcd`, `lcm`, factorial, `nPr`, `nCr`
  - Constants `pi`, `e`, `phi`; angle mode respected and visible
  - **DoD:** golden case per function including at least one domain error each

### Core plumbing

- [x] **P0-10 · `:core:registry`** — `S`
  - `CalculatorModule`, `CalcFunction`, `Tier`, `LocaleGate`, `EditorCommand`,
    `InputSurface` per `docs/ARCHITECTURE.md` §4–5
  - Hilt multibinding wiring in `:app`
  - **DoD:** a dummy module registers a function that the engine can then evaluate by name

- [x] **P0-11 · `:core:data`** — `M`
  - Room schema per `docs/ARCHITECTURE.md` §6, exported schemas committed
  - `schemaVersion` on documents and formulas
  - DataStore for settings: locale override, angle mode, theme, separator
  - **DoD:** `MigrationTestHelper` test passes for v1; a corrupt DB enters recovery
    mode instead of crashing

*P0-11b was one task across `:app` and `:core:ui`, which rule 3 forbids. Split into the two
below: the reusable pieces first, then the shell that wires them to a window.*

- [x] **P0-11b-1 · Adaptive layout and insets** — `M` — `:core:ui`
  - `WindowSizeClass` exposed as state a composable can read. Layout branches on the class,
    never on device type, orientation, or a raw width
  - Inset helpers for the input surface: `ime` and `navigationBars` combined into one padding,
    so a keypad above a shown keyboard is not padded twice
  - Uses `androidx.window:window-core`, already on the release classpath — no new dependency
  - **DoD:** a pane reflows across compact, medium, and expanded with no width literal in any
    composable; a unit test covers every combination of ime and navigation-bar insets, present
    and absent; Paparazzi snapshots at the three widths

- [x] **P0-11b-2 · Activity shell** — `M` — `:app` — depends on P0-11b-1
  - `enableEdgeToEdge()`; the window size class is computed at the activity and fed into the
    layout as state, so a fold changes it without the activity being recreated
  - Predictive back opted in via `android:enableOnBackInvokedCallback`
  - **DoD:** verified on `Pixel_10` with gesture and with 3-button navigation, on a display
    cutout, in split-screen, and on a foldable AVD across the fold
  - *"Back never discards notepad work" moved to P1-03: there is no notepad in Phase 0 to
    discard, and a guarantee nothing can violate is a guarantee nothing tests.*

- [x] **P0-12 · `:core:ui` design system** — `M`
  - Dynamic color on API 31+, generated seed palette fallback below
  - Token mapping for the keypad per `docs/CONVENTIONS.md` §10; true-black variant stubbed
  - Typography with tabular figures; Android 14+ contrast levels honoured
  - **DoD:** Paparazzi snapshots in light, dark, and RTL for the token showcase screen

- [x] **P0-13 · Keypad and accessory row** — `M` — `:core:ui`
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
  - Back never discards work: with unsaved state, back commits it rather than prompting
    (moved here from P0-11b, which had no document to protect)
  - **DoD:** switching surfaces preserves cursor position; no visible layout jump; back with
    an uncommitted line leaves the line in the document, verified across process death

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
  - Open-source licences entry: renders `attributedDependencies` and the licence texts in
    `res/raw`. The list is already generated and tested; only the screen is left
  - **DoD:** changing separator updates the keypad immediately; the licences screen lists
    every shipped dependency and can open the full text of each licence it names

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
  - Attribution reachable from Settings — the obligation Apache-2.0 §4 puts on the binary,
    not on the repo. The list generates itself from the licence allowlist; what P1-11 owes is
    that the release build actually reaches it
  - **DoD:** internal testing track accepts an upload; the licences screen is reachable in a
    release build and names all shipped dependencies

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
| 2026-08-17 | P0-03 | `junit:junit` is EPL-1.0, outside the Apache-2.0 / MIT / BSD rule of §2, but JUnit is approved by name in `docs/ARCHITECTURE.md` §7 | Recorded in the allowlist as test-only, marked EPL. Resolved by the shipped/test-only split recorded below |
| 2026-08-13 | P0-04 | Should a golden expectation be the locale-formatted result or the canonical one? | Canonical and locale-free. `locale=` selects how the input is read; the expectation asserts engine behaviour, not display formatting (`docs/CONVENTIONS.md` §1) |
| 2026-08-13 | P0-06 | In dot-decimal locales `,` is both the grouping and the argument separator, so `max(1,5)` is ambiguous | `,` counts as grouping only when it is followed by a full group of digits — three, or two or three for Indian grouping. Otherwise it is the argument separator. `1,234` is a number, `max(1,5)` is two arguments |
| 2026-08-13 | P0-07 | `of` has no level in the `docs/GRAMMAR.md` §1 precedence table | Parsed at the multiplicative level, so `50% of 80 + 10` is `50` |
| 2026-08-14 | P0-09 | A golden file could not express the angle mode, so RAD and GRAD behaviour had nowhere to be asserted | `angle=DEG\|RAD\|GRAD` directive added alongside `locale=`, sticky the same way, defaulting to DEG. `docs/GRAMMAR.md` §5 updated |
| 2026-08-14 | P0-09 | Trigonometry and logarithms run on `Double`, so `sin(30°)` computes as `0.49999999999999994` and `cos(90°)` as `6.12e-17` | Results are re-boxed at 15 significant digits, HALF_UP, and angles landing exactly on a quarter turn are answered exactly — `cos(90)` is `0`, `tan(90)` is a `DomainError`. The whole `Double` boundary is `function/Precision.kt` |
| 2026-08-14 | P0-09 | `docs/GRAMMAR.md` §4 listed `inf` and `nan`, which `BigDecimal` cannot represent | Removed from §4. The engine has no non-finite value; `DivisionByZero` and `Overflow` cover what would have produced one, and both carry a span |
| 2026-08-14 | P0-09 | `mod` was to be both an operator and a function name | Operator only. `mod` is a reserved word (§4), so the lexer never yields it as an identifier and `mod(10, 3)` cannot parse as a call |
| 2026-08-17 | P0-10 | Hilt 2.57.2 fails to apply on AGP 9.3.1 — `Android BaseExtension not found`, the extension AGP 9 removed | Hilt raised to 2.60.1. Hilt itself is already approved by name in `docs/ARCHITECTURE.md` §7, so this is a version bump, not a new dependency |
| 2026-08-17 | P0-10 | KSP `2.2.10-2.0.2` adds its generated sources through `kotlin.sourceSets`, which AGP 9's built-in Kotlin rejects | KSP raised to the decoupled `2.3.11` line, which registers its output with AGP directly. The documented `android.disallowKotlinSourceSets=false` escape hatch was not used — it suppresses a real check for the whole build |
| 2026-08-17 | P0-10 | detekt `FunctionNaming` rejects `@Composable fun Screen(...)`, but `AGENTS.md` §5 requires composables to be PascalCase | `ignoreAnnotated: ['Composable']` on that rule only. The lowercase pattern still applies to every other function |
| 2026-08-17 | P0-10 | JUnit 5 (`org.junit.jupiter:*`, `org.junit.platform:*`, `org.junit:junit-bom`) is EPL-2.0, the same situation as the `junit:junit` row above | Recorded in the allowlist as test-only, marked EPL-2.0. Resolved by the shipped/test-only split recorded below |
| 2026-08-17 | P0-11 | `MigrationTestHelper` reads the exported schema as a test asset, but the generated `android { sourceSets }` accessor resolves to the legacy type AGP 9 removed | The schema directory is registered through the typed `com.android.build.api.dsl.LibraryExtension` instead. No source-set escape hatch, no schema copy step |
| 2026-08-17 | P0-11 | Room's migration test needs a real Android framework, so `:core:data` unit tests run on Robolectric rather than plain JVM | Accepted for this module only. Robolectric is approved by name in `docs/ARCHITECTURE.md` §7; the engine stays a pure JVM library with no Android test runtime |
| 2026-08-17 | P0-12 | The §10 row `Error underline and message → error / onErrorContainer` puts message text at 2.66:1 in the light scheme and 1.31:1 in the dark one, far below the WCAG AA minimum of 4.5:1 | The row was two things at once. Split into `Error underline` (`error`, a rule measured against the editor at the 3:1 non-text minimum) and `Error message` (`onErrorContainer` on `errorContainer`, 13.25:1 and 7.24:1). `LineoRole.Error` became `ErrorUnderline` and `ErrorMessage`; `docs/CONVENTIONS.md` §10 updated |
| 2026-08-17 | P0-12 | The role mapping was only reachable from a composition, so no test could measure it — which is how the error row shipped unread | `RoleColors.of(scheme, role)` added as a plain function; the composable overload delegates to it. `RoleContrastTest` now asserts every row of §10 in all three seeded schemes, and the assertion was verified by reverting the mapping and watching it fail |
| 2026-08-17 | P0-12 | The seed colour for the fallback palette had never been chosen | `#4C5FD5`, an indigo. Neutral enough that the equals key reads as the accent without competing with the numbers, and it is the hue the whole tonal scheme is derived from |
| 2026-08-17 | P0-12 | The Material Theme Builder is a web tool, so no agent can run it, yet §10 forbids hand-picking hex values | Material's HCT colour space and its TonalSpot scheme were ported to a one-off generator and the output committed. The port was validated against Material's own baseline: seed `#6750A4` reproduces the published `primary #65558F`, `primaryContainer #EADDFF`, `secondary #625B71`, `tertiary #7D5260`. `docs/CONVENTIONS.md` §10 updated to allow any equivalent generator |
| 2026-08-17 | P0-12 | Paparazzi 1.3.5 cannot apply on AGP 9.3.1 — `Extension of type 'BaseExtension' does not exist`, the same extension AGP 9 removed for Hilt | Paparazzi raised to `2.0.0-alpha05`, the line that targets AGP 9. It is a pre-release; Paparazzi itself is approved by name in `docs/ARCHITECTURE.md` §7, and it is test-only, so nothing pre-release reaches the APK |
| 2026-08-17 | P0-12 | Paparazzi 2.0 ships Java 21 bytecode, but the shared test toolchain is 17, pinned there by detekt | The launcher is overridden to 21 in `:core:ui` alone. A build-wide bump would have moved the engine and data tests off the version detekt is verified against, for no gain |
| 2026-08-17 | P0-12 | In an RTL layout, bidi reordered `1 234,5 + 67,89` into `67,89 + 234,5 1` — same characters, different sum | Expressions are forced left to right through `TextStyle.asExpression()`. Alignment stays a layout decision: text is positioned by `Alignment.CenterStart`/`CenterEnd`, never by `TextAlign`, which would follow the forced direction instead of the locale |
| open | P0-12 | Android 14+ contrast levels are honoured on the dynamic path only — the system rebuilds its colour resources and `LineoTheme` re-reads them. The seeded fallback has no medium or high contrast variant | Accepted for Phase 0: below API 31 there is no contrast setting to honour, so the gap is only a user on API 34+ who has turned dynamic colour off. **Close it with the Phase 3 seed picker**, which has to generate contrast variants anyway |
| 2026-08-17 | P0-12 | Paparazzi's test-only transitives include three outside the Apache-2.0 / MIT / BSD rule of §2: `org.jetbrains.intellij.deps:trove4j` (LGPL-2.1), `com.sun.activation:javax.activation` (CDDL-1.0 or GPL-2.0-with-classpath-exception), and `net.java.dev.jna:*` (dual LGPL-2.1 or Apache-2.0, Apache elected) | Recorded in the allowlist with the licence named. None is linked into the shipped APK. `trove4j` is a plain LGPL row and the strongest case of the three — resolved by the shipped/test-only split recorded below |
| 2026-08-17 | P0-11 | Robolectric drags in two test-only transitives outside the Apache-2.0 / MIT / BSD rule of §2: `com.ibm.icu:icu4j` (Unicode-3.0) and `javax.annotation:javax.annotation-api` (CDDL-1.1 or GPL-2.0-with-classpath-exception) | Recorded in the allowlist as test-only with the licence named. Neither is linked into the shipped APK. The GPL-2.0-with-classpath-exception text touches §2 directly — resolved by the shipped/test-only split recorded below |
| 2026-08-17 | P0-03 | §2 held a test transitive and a shipped library to the same standard, so the allowlist grew fifteen harmless EPL and LGPL rows, each asking for the same approval as a library that actually reaches users. A rule that raises fifteen false alarms is not read by the time the real one arrives | The gate now classifies by resolved classpath. Shipped — reachable from a runtime classpath that is not a unit-test, instrumented-test, screenshot-test, or test-fixture one — is held to Apache-2.0 / MIT / BSD, **and the recorded licence is now checked, not merely present**. Test-only moves to `config/licenses/allowed-test-dependencies.txt` and may carry any licence. Verified by marking a shipped entry LGPL-2.1 and by deleting another: both fail the build. `AGENTS.md` §2 and `docs/ARCHITECTURE.md` §7 updated. Shipped is 149 modules, all permissive; test-only is 110 |
| 2026-08-17 | P0-03 | Four `writeDependencyLicenseAllowlist` tasks run in parallel and read-modify-write the same two files, which produced torn lines and duplicated entries. A Gradle `BuildService` — the documented fix — could not be shared: each project loads the convention plugin in its own classloader, so one project's service type is not the next one's | The task action synchronises on an interned string. The JVM string pool is shared across every classloader in the daemon, so the monitor is one object everywhere |
| 2026-08-17 | P0-11b | The task spanned `:app` and `:core:ui`, which rule 3 forbids | Split into P0-11b-1 (`:core:ui` — width class, insets, adaptive pane) and P0-11b-2 (`:app` — edge-to-edge, predictive back, device verification). "Back never discards notepad work" moved to P1-03: Phase 0 has no notepad to protect, and a guarantee nothing can violate is a guarantee nothing tests |
| 2026-08-17 | P0-11b-1 | `AdaptivePane` measures the input pane before the document, so an input asking for `fillMaxSize` took the whole window. The first compact snapshot showed an input pane and no document at all | The input pane must size itself to its content — the same contract Material's `Scaffold` places on its `bottomBar`, and natural for a keypad, which is rows of keys. Stated in the KDoc and demonstrated by the snapshot test, which now uses a wrapping input |
| 2026-08-17 | P0-11b-1 | `WindowSizeClass.compute` is deprecated in `window-core` 1.5.0, and `allWarningsAsErrors` turns that into a build failure | Switched to `WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(...)`. `androidx.window:window-core` is now declared rather than inherited transitively; it was already on the release classpath and already allowlisted, so no licence decision |
| 2026-08-17 | P1-11 | Apache-2.0 §4 requires a copy of the licence to travel with the binary, and MIT and BSD require the copyright notice. Nothing in the app carried either | Attribution is generated from `config/licenses/allowed-dependencies.txt` at build time by `:app:generateLicenseAttribution`, registered as a source directory rather than committed. Google's `play-services-oss-licenses` was rejected: it is a new dependency, needs the network at build time, weighs against the 12 MB target of P1-10, and reads a different source than the gate — so it can list a library the APK does not contain. Generating from the enforced allowlist cannot drift. The screen itself is P1-07 |
| 2026-08-17 | P1-11 | `com.google.code.findbugs:jsr305` was recorded as BSD-3-Clause | Its POM declares Apache-2.0, so the record now says so. The source headers do carry BSD-3-Clause; both are permitted, and the distributor's own declaration is the one to record. Every shipped dependency is now Apache-2.0, so the app bundles exactly one licence text |
| 2026-08-17 | P1-11 | An MIT or BSD dependency needs its copyright holder recorded, and the allowlist has no column for one. Adding an empty column to 149 Apache-2.0 rows would be noise | No column. The generator refuses the first MIT or BSD entry that arrives without a `Copyright` note, and `AttributionTest` refuses any licence family whose text is not bundled in `res/raw`. Both were verified by introducing a fake MIT dependency and watching each fail in turn |
| 2026-08-17 | P0-11b-2 | The window size class had to come from somewhere the shell could read without a configuration callback | `LocalWindowInfo.containerSize`, not the display or the resources configuration. The window is not the screen: in split-screen the app owns a fraction of it, and on a foldable the fraction changes as the device opens. It is snapshot state, so the class recomputes on resize |
| 2026-08-17 | P0-11b-2 | Running the shell on a device showed the keypad's background stopping at the top of the navigation bar, leaving a strip of editor surface behind it | `AdaptivePane` no longer pads the input pane from outside; the pane draws full bleed and pads its own content. Paparazzi could not have caught this — a snapshot has no system bars to be wrong about, which is why the DoD asks for a device |
| 2026-08-17 | P0-11b-2 | The SDK has no `cmdline-tools`, so `avdmanager` could not create the foldable AVD the DoD requires | The AVD was hand-authored against the `pixel_10_pro_fold` skin already in the SDK: hinge sensor, posture list, and a secondary display region. It boots, reports CLOSED/HALF_OPENED/OPENED/REAR_DISPLAY_MODE, and folds without killing the app |
| 2026-08-17 | P0-11b-2 | Disk C: fell to 3.49 GB and the foldable AVD refused to create its data partition. The `Pixel_10` quick-boot snapshot had regrown to 8 GB | Snapshot deleted again and the foldable capped at a 2 GB data partition with `fastboot.forceColdBoot=yes`, so it never writes one. **This is the same shortage that corrupted the `Pixel_10` snapshot earlier today** — a standing risk, not a one-off |
| 2026-08-17 | P0-13 | A calculator keypad needs an all-clear key, and `EditorCommand` has no variant for one. Adding `ClearLine` changes the contract in `:core:registry` and `docs/ARCHITECTURE.md` §5 | The keypad ships without `AC`; `⌫` takes the top-left slot. An empty `InsertText("")` would have looked like a key and done nothing. Resolved: `ClearLine` added to the contract, and the keypad grew to five columns to give `AC` a slot without crowding `=`. The five-column layout then exposed a second bug — see the row below |
| 2026-08-17 | P0-13 | The accessory row has to stay attached to keyboards that report different heights and animate for different durations | It never measures the keyboard. It pads by the larger of the `ime` and `navigationBars` insets, both snapshot state, so it rides the show and hide animation instead of jumping at the end. Verified against GBoard docked (row flush above the toolbar) and GBoard floating (inset zero, row on the navigation bar) |
| open | P0-13 | The DoD names GBoard, SwiftKey and Samsung Keyboard. Only GBoard is on the emulator image, and the other two are not distributed as installable APKs for it | Verified against GBoard in docked and floating modes. **SwiftKey and Samsung Keyboard remain unverified** — they need a physical device or a Samsung system image, so this is a hardware gap, not a code one |
| 2026-08-17 | P0-13 | A `uiautomator` dump showed each key's content description on a child node of the clickable one rather than on it | Compose publishes the unmerged tree as well; TalkBack reads the merged node, which carries both. Left as is, and `semanticsLabel` records why. Confirming it with TalkBack itself is P1-08, which exists for that |
| 2026-08-17 | P0-13 | Adding `AC` widened the keypad to five columns, which put a comma decimal key next to a comma argument key: the same glyph twice, typing the wrong one half the time | The argument separator is derived from the decimal separator per `docs/CONVENTIONS.md` §2 — `,` beside a dot decimal, `;` beside a comma one. A test now asserts no two keys on one surface ever show the same label, in either convention. The bug was invisible until the layout changed; the four-column keypad had no argument key at all |
