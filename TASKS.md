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

*P0-14 was `L`, and rule 5 says an `L` gets split once it starts. State first, then what the
state looks like — the half that can be tested without a screen, and the half that cannot.*

- [x] **P0-14-1 · Editor state: commands, caret, debounced evaluation** — `M` — `:core:ui`
  - Consumes `EditorCommand`; owns the text and the caret
  - 400 ms debounce; input that is merely unfinished evaluates to nothing rather than to an
    error, because a user mid-keystroke has not made a mistake yet
  - **DoD:** `5 +` produces no error state; one unit test per command; the debounce asserted
    on a test scheduler rather than by waiting

- [x] **P0-14-2 · Editor rendering: result, underline, fix chip** — `M` — `:core:ui` — depends
  on P0-14-1
  - Result rendered inline; underline at `CalcError.span`; message below the line
  - Tap-to-fix chip built from `CalcError.UnknownIdentifier.suggestion`
  - **DoD:** `sni(1)` shows a suggestion chip that replaces the identifier when tapped;
    Paparazzi in light, dark, and RTL

**Phase 0 exit criteria:** engine golden suite and fuzz test green in CI; editor
evaluates a single line end to end.

---

## Phase 1 — First release

- [x] **P1-01 · Notepad document model** — `M` — `:feature:notepad`
  - Stable `LineId`; `ordinal` used only for display
  - Reference rewriting on insert/delete/reorder without repointing
  - **DoD:** inserting a line above line 3 leaves `line3` pointing at the same content

*P1-02 was `L`, and rule 5 says an `L` gets split once it starts. The graph first — what reads
what, in what order, and which lines are in a circle — then the evaluation that walks it.*

- [x] **P1-02-1 · Dependency graph, order, cycle detection** — `M` — `:feature:notepad`
  - Edges from line references and from variables in scope; topological order; Tarjan for the
    circles, every line in one marked
  - **DoD:** a forward reference orders correctly; two lines referencing each other are both
    marked and no third line is; the order is stable for one document

- [x] **P1-02-2 · Incremental evaluation** — `M` — `:feature:notepad` — depends on P1-02-1
  - Only the changed line and its transitive dependents recompute; a parse is reused when the
    line's text and scope are unchanged
  - A line reading a failed line is `Blocked`, not wrong (`docs/ARCHITECTURE.md` §3)
  - **DoD:** 200-line document, keystroke to result under 50 ms on an API 26 emulator
    — *asserted by counting work, not by timing: see the open row below*

*P1-03 was `L` and spanned three modules, which rules 3 and 5 both forbid. Split by module:
the reusable line edit first, then the state, then the screen, then persistence.*

- [x] **P1-03-0 · Reusable line editing** — `S` — `:core:ui`
  - `EditedLine` + `applying(command)`: the pure text-and-caret transform, lifted out of
    `EditorState` so the notepad applies commands through the same code
  - **DoD:** the single-line editor's tests pass unchanged; `±` has one implementation

- [x] **P1-03-1 · Notepad state** — `M` — `:feature:notepad` — depends on P1-03-0, P1-02-2
  - One `uiState`; every surface reaches the document through `EditorCommand`
  - Enter splits a line, backspace at the start joins it to the one above
  - Context auto-switch on focus: a line beginning with a letter raises the keyboard
  - **DoD:** switching surfaces preserves cursor position; a reference keeps its target
    across a split; typing `line12` does not rewrite itself under the user

- [x] **P1-03-2 · Notepad screen** — `M` — `:feature:notepad` — depends on P1-03-1
  - The list of lines, per-line result, the docked input surface, suggestion chips for
    in-scope variables and recent units
  - The chip row must reach `%`, `^`, `√` and the argument separator above *either* surface —
    the gap recorded against P0-13
  - **DoD:** no visible layout jump when the surface changes; Paparazzi in light, dark and RTL

- [x] **P1-03-3 · Persistence and back** — `M` — `:feature:notepad` — depends on P1-03-2
  - Maps `NotepadDocument` to the `documents` / `lines` rows through `:core:data`
  - Back never discards work: with unsaved state, back commits it rather than prompting
    (moved here from P0-11b, which had no document to protect)
  - **DoD:** back with an uncommitted line leaves the line in the document, verified across
    process death

*P1-03-3 stopped at the module boundary: the mapping and the autosave are `:feature:notepad`, but
nothing shows the notepad, and rule 3 keeps an `:app` edit out of that task. P1-03-4 is that edit —
the one that makes both testable on a device.*

- [x] **P1-03-4 · Notepad in the shell** — `S` — `:app` — depends on P1-03-3
  - `NotepadViewModel` opens the document, holds the `NotepadState`, runs the autosave, and
    flushes on stop. No `Context` in it (`docs/ANDROID_STANDARDS.md` §1)
  - `SingleLineScreen` deleted: the Phase 0 harness it replaced is gone, and so is the one
    remaining place where `%`, `^` and `√` cannot be typed
  - **DoD:** typing, then back, then reopening shows the same document, verified across
    process death with `adb shell am kill`

*P1-04 spanned `:core:ui` (the keypad had one hardcoded layout), `:feature:scientific` and
`:app` (nothing can reach a module screen yet), which rule 3 forbids. Split by module: the
layout first, then the module, then the wiring.*

- [x] **P1-04-0 · Keypad layout as a parameter** — `S` — `:core:ui`
  - `KeypadLayout` supplied to `KeypadState`; `basicKeypadRows` public so a module builds on
    the digits rather than restating them
  - `EditorCommand.InsertFunction` finally honours its `arity`: `nCr` inserts `nCr(,)`, with
    the separator the decimal separator implies
  - **DoD:** the existing keypad snapshots are unchanged; a supplied layout reaches the grid
    and its keys still speak `EditorCommand`

- [x] **P1-04-1 · `:feature:scientific`** — `M` — depends on P1-04-0
  - Registers its functions through `CalcFunction`, not in the Screen
  - Only what the engine does not already have: `sec`, `csc`, `cot`, `sign`, `trunc`, `min`,
    `max`, `hypot`. Each composed from built-ins or from `BigDecimal`, never from a `Double`
  - A wider window expands to the full scientific keypad — width, not orientation
  - **DoD:** every function is callable as text in notepad mode

- [x] **P1-04-2 · Scientific in the shell** — `S` — `:app` — depends on P1-04-1
  - `@Provides @IntoSet` contributes `ScientificModule`; a route to its `Screen`, with back
    wired to `ModuleNav.back()` — the module takes no `activity-compose` dependency for it
  - The overflow button opens something for the first time (the defect recorded at P0-11b-2)
  - The notepad evaluates against the module registry, not the built-ins alone
  - **DoD:** a function key pressed on the scientific screen and the same call typed in the
    notepad produce the same result, verified on a device

*The device found what the snapshots could not: the input pane had no ceiling, so a taller
keypad took the window and left the expression nowhere to be drawn. That is `:core:ui`, and
its own task.*

- [x] **P1-04-3 · The input pane gets a ceiling** — `S` — `:core:ui`
  - `AdaptivePane` keeps 170 dp for the document — one expression line and its result — and
    gives the input pane the rest; `Keypad` sizes a key from the scarcer of width and height,
    so a grid too tall for its pane shrinks its keys rather than pushing the document out
  - **DoD:** the four-column keypad is the same size it has always been, on a device and in
    every snapshot; a six-row grid on a compact phone still shows the expression and its
    result — both verified on a device

*P1-05 spanned three modules as well: a module's units never reached the evaluator, data had
no dimension, and nothing in `:app` bound the module. Split the same way as P1-04.*

- [x] **P1-05-0 · Units an evaluation is given** — `M` — `:core:engine`
  - `UnitRegistry` becomes a class with `BUILTIN` and `with()`, mirroring `FunctionRegistry`;
    `EvalContext` carries one, so `ModuleRegistry.units()` finally reaches an expression
  - `Dimensions.information`, so a byte is not a plain number
  - **DoD:** the golden files pass untouched; a contributed unit is nameable and convertible

- [x] **P1-05-0b · `in` after a conversion keyword** — `S` — `:core:engine`
  - `5 cm to in` parses as inches: a conversion target cannot itself be a conversion, so the
    ambiguity §3.3 exists to resolve does not arise there. `5 in 3` is unchanged
  - **DoD:** two golden cases, and `to in^2` works for the same reason

- [x] **P1-05-1 · `:feature:converter`** — `M` — depends on P1-05-0
  - Length, mass, volume, area, speed, temperature, data, time, pressure, energy
  - Registers `UnitDefinition`s so `5 km to mi` works in notepad
  - The screen composes `<amount> <from> to <to>` and hands it to the engine — no arithmetic
  - **DoD:** ISO 80000 symbols; IEC binary prefixes correct (`KiB` = 1024)

- [x] **P1-05-2 · The converter in the shell** — `S` — `:app` — depends on P1-05-1
  - `@Provides @IntoSet`; the notepad is given the module units as well as the functions
  - **DoD:** a conversion done on the converter screen and the same one typed in the notepad
    agree, verified on a device

- [x] **P1-06 · History** — `S` — `:app`
  - Tape view, tap to reuse, capped at 50 for free tier
  - **DoD:** survives process death

- [x] **P1-07 · Settings** — `S` — `:app`
  - Separator override, angle mode, theme, unit system, decimal places
  - "Follow wallpaper colours" toggle, off by default — the opt-in that pays for Lineo's
    palette being the default (`docs/CONVENTIONS.md` §10)
  - Open-source licences entry: renders `attributedDependencies` and the licence texts in
    `res/raw`. The list is already generated and tested; only the screen is left
  - **DoD:** changing separator updates the keypad immediately; the licences screen lists
    every shipped dependency and can open the full text of each licence it names

*P1-08 spans every module, the same shape as P1-04 and P1-05. Split: the reader and the
shared chip in `:core:ui`, then one task per surface, then the device pass that needs all
of them present.*

- [x] **P1-08-0 · What a screen reader is given** — `M` — `:core:ui`
  - `ExpressionSpeech` walks the AST: `2^3` is "2 to the power of 3", brackets are spoken
    only where they change the answer, and `5 km` stays a quantity
  - `ChoiceChip`: selection is a bolder label as well as a colour, and `selectable` says so
  - `keySize` floored at 48 dp — it had no floor, and a short pane produced a smaller key
  - **DoD:** every case goes through the real parser; the floor has a test in the windows
    no snapshot covers

- [x] **P1-08-1 · The notepad reads its lines** — `S` — `:feature:notepad`
  - A line that is not being edited is described from the tree the evaluation already
    parsed. The focused line keeps its raw text — a field must read what is in it
  - **DoD:** the tree reaches the screen, and a line that does not parse has none

- [x] **P1-08-2 · The other surfaces say what they do** — `S` — `:feature:converter`, `:app`
  - Category, separator, theme and unit-system rows are selectable groups of chips; a tape
    row is one node with a labelled tap; a licence row says which way it is facing
  - **DoD:** no action is announced as "activate" alone, and no chip is read as a bare glyph

- [~] **P1-08-3 · The device pass** — `S` — all — **one line left, see the log**
  - `ShellSemanticsTest` runs on a device: a chip publishes its selected state and reads as
    an answer to its title, a tape row is one node with a labelled tap, a licence row says
    which way it is facing. `ar-XB` is covered by the pseudo-locale snapshots P1-09 added
  - **Left:** a human listening to TalkBack walk the whole flow. Nothing else asserts that
    the order is sensible or that the result is bearable to listen to
  - **DoD:** full task flow completable with TalkBack only

- [x] **P1-09 · Localisation plumbing** — `S`
  - `checkHardcodedText` fails the build on user-facing text written into Kotlin. Android's
    own `HardcodedText` reads layouts, and there are none here
  - `isPseudoLocalesEnabled` on the debug build, `android:localeConfig` declared, and
    snapshots in `en-XA` (expanded) and `ar-XB` (expanded and mirrored)
  - `ABC` and `123` moved to `strings.xml` — they name a script and a number system. Every
    other key label is notation and stays in code
  - **DoD:** the check fails on a hardcoded string — verified by writing one; no truncation
    in `en-XA`

- [x] **P1-08b · Font scaling and large-screen pass** — `S` — all
  - Snapshots at 2× for the keypad, the notepad and the shell screens, and one of the
    notepad on a tablet. The `ABC` key clipped to `AB` and is a minimum size now
  - The notepad takes the keys no field wanted: typing with no line focused, the arrows
    between lines and along one, Enter for a new line
  - **DoD:** verified on a foldable — typed `12+3`, Enter, `7*6`, walked back up with the
    arrows and typed into the line above, all from the keyboard alone

*P1-10 splits into the harness and the numbers. The harness is code and is done; the numbers
need hardware this machine does not have, and are the part still open.*

- [x] **P1-10-0 · The release build, and what it weighs** — `S` — `:app`, `build-logic`
  - R8 in full mode with resource shrinking; `proguard-rules.pro` keeps line numbers and
    nothing else, because a keep rule without a reason is one nobody can ever delete
  - `ReportDrawnWhen { notepad != null }`: startup ends when the document is on screen, not
    when the first frame is
  - CI builds the release APK and fails over 12 MB rather than remembering the number
  - **DoD:** the release build is 1.6 MB, R8 clean, and the ceiling is enforced

- [x] **P1-10-1 · The instrument** — `M` — `:benchmark`
  - `StartupBenchmark` (cold, with and without a baseline profile), `TypingBenchmark`
    (frame timing) and `BaselineProfileGenerator`, all driving one shared journey
  - `:app` consumes the generated profile and ships `profileinstaller`, which installs it
  - CI compiles both, so a benchmark cannot rot while nobody runs it
  - **DoD:** `:benchmark:assembleBenchmark` and `:app:assembleRelease` both build

- [x] **P1-10-2 · The database was already right** — `S` — `:core:data`
  - `lines.documentId` has been indexed since P0-11, together with `(documentId, ordinal)`
  - Every DAO method is `suspend` or returns a `Flow`, and nothing calls
    `allowMainThreadQueries`, so a main-thread read is not expressible
  - **DoD:** verified by reading, and recorded rather than re-done

- [~] **P1-10-3 · The numbers** — `S` — **blocked on hardware, see the log**
  - Cold start on an API 26 device, frame timings while typing, and a generated profile
    committed to `app/src/release/generated/baselineProfiles`
  - **DoD:** cold start under 500 ms on an API 26 device; no dropped frames while typing

- [x] **P1-10b · Security and privacy pass** — `M` — `:app`
  - The permission list is *empty*: neither `INTERNET` nor `ACCESS_NETWORK_STATE` has
    anything to serve until currency lands, and each is declared by the change that needs it
  - `network_security_config.xml` forbids cleartext everywhere, written before there is any
    network code to forbid it for. The rate-payload half belongs to P2-03 — see the log
  - Cloud backup excludes everything; device transfer keeps the documents, which is a
    different journey and a different threat
  - `collectAsState` in the notepad became `collectAsStateWithLifecycle`, the one §1 line
    the checklist found still broken
  - **DoD:** `ManifestSecurityTest` asserts the shipped package on a device — the permission
    list, the cleartext flag and the backup flag. The §7 rows still open are owned by open
    tasks: TalkBack (P1-08-3), the baseline profile (P1-10-3), Data Safety (P1-11), the
    staged rollout (P1-13)

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

*P1-15 closes a defect that ships: on a compact phone the scientific module cannot type a
bracket at all. `(`, `)`, `^`, `√`, `%` and the argument separator are not on the keypad, and
the row that carries them appears only with the text keyboard. The notepad solved this with a
chip row above **both** surfaces, but that row lives in `:feature:notepad` and a feature may
not read another's code. Three modules, so three tasks — rule 3.*

- [x] **P1-15-0 · One chip row, in `:core:ui`** — `S` — `:core:ui`
  - `ExpressionChipRow` — the expression keys, plus `ExpressionChip`s the calling screen
    contributes. Resolved strings in the chip, not resource ids: `:core:ui` has no business
    owning a string about a variable in scope
  - There were already **two** near-identical rows — `AccessoryRow` here and `NotepadChipRow`
    in the notepad, same chip, same 16 dp corner, same 48 dp floor. Adding a third would have
    been the wrong fix, so `AccessoryRow` is now a five-line call to this one
  - `AccessoryRowState.press` takes the `EditorCommand` rather than the `KeypadKey` it used
    to unwrap — a contributed chip has a command and no key, and the command is what both have
  - **DoD:** the existing `AccessoryRow` snapshots pass **unchanged** ✓ — that, not the new
    snapshots, is the proof the move cost a user nothing

- [x] **P1-15-1 · The notepad uses it** — `S` — `:feature:notepad` — depends on P1-15-0
  - `NotepadChipRow` deleted — 130 lines, and the module is one file smaller rather than one
    file moved. The mapping from `NotepadSuggestion` to `ExpressionChip` stayed here, because
    the strings did: what a suggestion *is* — a name a line above the caret defined, a unit
    one has produced — is this screen's knowledge and not `:core:ui`'s
  - **DoD:** all 10 notepad snapshots pass **unchanged** ✓, and no PNG was rewritten

- [ ] **P1-15-2 · The scientific screen gets its brackets back** — `S` — `:feature:scientific`
  — depends on P1-15-0
  - The row goes above *both* surfaces, `docked = false` when the keypad is below it, as the
    notepad already does. `AccessoryRow` then has no caller and can go
  - **DoD:** `(2+3)*4` is typable in keypad mode on a compact window — the defect this task
    exists for

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
| 2026-08-17 | P0-12 | The seed colour for the fallback palette had never been chosen | `#4C5FD5`, an indigo. Neutral enough that the equals key reads as the accent without competing with the numbers, and it is the hue the whole tonal scheme is derived from Superseded on the same day: the palette is now a Material Theme Builder export supplied by the product owner, olive and chartreuse, kept in `docs/LineoCP/`. The generator built for this row did its job — it produced a committed, non-hand-picked palette until a chosen one existed |
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
| 2026-08-17 | P0-14 | The task is `L`, and rule 5 says an `L` is split once it starts | P0-14-1 is the state — commands, caret, debounce — which is testable without a screen. P0-14-2 is what that state looks like: inline result, underline at the span, message, fix chip. The split falls where the test strategy changes, not at an arbitrary halfway point |
| 2026-08-17 | P0-14-1 | `CalcError.Syntax` covers both `5 +`, which the next keystroke fixes, and `5 + + 3`, which it does not. The error type alone cannot tell the editor whether to stay quiet | Position decides. A syntax error at the end of the trimmed line is unfinished; one inside it is a mistake. An unbalanced bracket is always unfinished — there is no way to have typed a closing bracket that is still missing. `EditorEvaluation` has a distinct `Unfinished` state so a user mid-keystroke is never shown a red underline |
| 2026-08-17 | P0-14-1 | The debounce was built on `snapshotFlow { text }`, and the restart-the-timer test failed: it saw one evaluation where it expected none | `snapshotFlow` only observes a change once the recomposer sends apply notifications, so the evaluation pipeline silently depended on something being composed. The text is now mirrored into a `MutableStateFlow` written in the same place as the Compose state. The test that caught it asserts a burst of six keystrokes costs one parse, not six |
| 2026-08-17 | P0-14-2 | The error underline is a rule in §10, but Compose cannot colour an underline separately from the text it sits under | The span takes the `error` colour *and* the underline. Colour alone would break §10's ban on colour-only meaning; the rule alone is invisible to anyone who cannot see a two-pixel line. `error` on `surface` measures 6.13:1, above the AA minimum for text, so the coloured span is legible in its own right |
| open | P0-14-2 | The result is rendered with `Quantity.canonicalString()`, which is locale-free. `docs/CONVENTIONS.md` §1 puts grouping and the locale separator at exactly this boundary, and no formatter exists yet | Left locale-free rather than improvised, and recorded instead of hidden behind a comment. **Needs its own task** — it belongs with P1-07, which already owns the separator setting, or as a small task before it. A user in `id-ID` currently sees `8500000` where they expect `8.500.000` |
| 2026-08-17 | P0-14-2 | Phase 0 exit criterion — "editor evaluates a single line end to end" | Met on `Pixel_10`: `0.1+0.2` typed on the keypad renders `0.3`, which is also the `BigDecimal` guarantee of `AGENTS.md` §2 made visible. `AC` clears the line. `Phase0InputHarness` deleted; `SingleLineScreen` replaces it and goes at P1-03 |
| 2026-08-17 | P0-12 | The product owner supplied the intended palette as a Material Theme Builder export, `docs/LineoCP/` | Copied verbatim into `LineoColorSchemes`, replacing the seed-generated indigo. `RoleContrastTest` passed unchanged against it, which is the whole reason that test was written before a palette anyone cared about existed. `AmoledDark` regenerated from the new dark ladder; all 15 snapshots re-recorded |
| 2026-08-17 | P0-13 | The reference design in `docs/LineoCP/preview.webp` shows circular keys in four columns, with `AC` and `=` the same yellow in *both* schemes | Adopted. Keys are circles sized by the column width, so a narrow phone gets smaller keys rather than an overflowing grid. `AC` and `=` use `primaryFixed`/`onPrimaryFixed` — `primary` inverts between schemes and would have made them dark olive in light mode, while "fixed" is the M3 role for an accent that does not flip. §10 updated for both, plus the rule that they are told apart by corner and never by hue |
| 2026-08-17 | P0-13 | Four columns hold twenty keys, and the keypad needs twenty-three | The bracket key became one key emitting `WrapSelection`, which is what that command was for and what freed a slot. `⌫` stayed on the top row away from `=` (the owner's call). `%`, `^`, `√` and the argument separator moved off the keypad entirely |
| 2026-08-17 | P0-13 | `%`, `^`, `√` and the argument separator are now reachable only from the accessory row, which appears only with the text keyboard — so they cannot be typed in keypad mode at all | `docs/ARCHITECTURE.md` §5 already says the chip row belongs above *either* surface. Closed by P1-03-2, which puts one chip row above both — see the row dated 2026-08-18. The single-line screen of `:app` still has the gap, and goes away when the notepad replaces it |
| 2026-08-17 | P0-12 | The palette was adopted but the emulator still showed wallpaper colours. §10 made dynamic colour the default on API 31+, which is almost every device — so the palette the product is designed around would have been seen by almost nobody | Reversed by the product owner: Lineo's palette is the default on every API level, and dynamic colour becomes an opt-in toggle in settings (added to P1-07). §10 rewritten, including its code sketch. The snapshots never changed, because they always passed `dynamicColor = false` — which is why the discrepancy only showed up on a device |
| 2026-08-17 | P0-13 | The owner asked for darker operator keys. The obvious move — a `fixed` secondary role — was rendered and rejected: it made dark-mode operators light khaki, the opposite of the reference design | Operators keep `secondaryContainer`, which is already right in dark. The separation the owner wanted comes from the other side: digits moved to `surfaceContainerLowest`, so they read as near-white in light and near-black in dark. One token changed, correct in both schemes, and it matches the reference in both. §10 updated |
| 2026-08-17 | P0-13 | Keypad labels were `titleMedium`, too small for a key read at a glance | `headlineSmall`. §10 typography updated with the reason, so it is not read as an accident |
| 2026-08-17 | P0-14-2 | Expression and result were aligned to the start; the reference design aligns them to the end | Aligned to the end, and the message and fix chip follow the same edge so the block reads as one thing. Digits of the expression and of the result now line up column for column, which is the point — the eye compares them without moving. §10 says so explicitly now |
| 2026-08-17 | P0-13 | Keys sat too close to the window edge, and the digit fill at `surfaceContainerLowest` read as stark white rather than the tinted grey of the reference | `LineoDimens.KeypadEdge` (16 dp) pads the leading and trailing edges; digits moved to `surfaceContainerLow`. Both are one-token changes and both are in §10 |
| 2026-08-17 | P0-11b-2 | The overflow button was overlaid on the content and collided with the expression: both want the top trailing corner, and a right-aligned expression grows towards it | Given a reserved row of its own. It costs the height of one button and cannot collide with anything. The shell also paints `surface` *before* the inset padding, so the colour runs behind the status bar instead of leaving the window's default white showing above the editor |
| open | P0-11b-2 | The overflow button opens nothing. History is P1-06, settings is P1-07, and there is no menu to show until one exists | Drawn from three circles rather than an icon dependency, with a tap target and a spoken label, so the layout around it is real. **A control that does nothing is a defect if it ships** — wire it, or remove it, before P1-11 |
| 2026-08-17 | P0-13 | Key labels and the display panel were both asked to grow by about 30% | Moved up the type scale rather than multiplying a size: keypad `headlineSmall` → `headlineLarge` (+33%), expression `displayMedium` → `displayLarge` (+27%), result `displaySmall` → `displayMedium` (+25%). Staying on the scale keeps the ratios between them intact and keeps §10 describable in role names |
| 2026-08-17 | P0-13 | The digit fill went white, then pale cream, then back to `surfaceContainerHigh` across three rounds of matching the reference | `surfaceContainerHigh` is the answer: neutral grey circles on a cream surface in light, lighter circles on near-black in dark. It reads as distinct from the olive operators once the keys are large — the earlier attempts were compensating for a font that was too small to let the hue difference register |
| 2026-08-17 | P0-12 | The yellow was asked to be more yellow and the operators deeper. Neither was reachable from the export: TonalSpot builds primary at chroma 36 and secondary at 16, so the palette's most saturated yellow is a pale olive | The accent families are regenerated from the same hue, 108.671°, at chroma 64 and 32, with the same HCT port that reproduced Material's baseline earlier. Surfaces, neutrals, tertiary and error are untouched, so the change is confined to the two families that were asked about |
| 2026-08-17 | P0-12 | Raising secondary chroma alone made the operators *yellow* rather than deeper, and they collided with `AC` and `=` — rendered, seen, rejected | The light `secondaryContainer` moves to T80, one step below Material's T90 for that slot. It is the only deviation from Material's tone assignments in the whole scheme, and it buys three levels the eye separates without effort: bright yellow to end a calculation, khaki for the operators, neutral grey for the digits. Recorded in the file's KDoc as well, since that is where the next reader will look |
| 2026-08-17 | P0-13 | `( )` was painted as a function key; it belongs with the operators | Moved. The reason is what the key does to a reading eye, not to the parser: brackets change the shape of an expression, which is the operators' job, while `sin` and `log` only name a value. §10's operator row now says so |
| 2026-08-17 | P0-13 | `Aa` needed to contrast, and had been sharing the neutral grey of the function keys | New role, `InputSwitch`, painted from `tertiaryContainer` — the only key on either surface drawn from the tertiary family. It is also the only key that enters nothing: everything else changes the expression, this changes what you are typing with. A third hue says that before the label is read, which matters most to whoever presses it by accident. `123` on the accessory row shares the role, being the same key going the other way |
| 2026-08-17 | P0-13 | `Aa` was asked to be black, from the palette | `inverseSurface` / `inverseOnSurface` — the M3 role that means "the opposite of the surface". In light it is the near-black the request asked for; in dark it inverts to cream, because a black key on a near-black surface would disappear. A fixed black would have satisfied the words and broken the dark scheme |
| 2026-08-17 | P0-13 | The digit grey was asked to be a little deeper again | One step: `surfaceContainerHigh` → `surfaceContainerHighest` |
| 2026-08-17 | P0-13 | Type was asked to grow again, and `displayLarge` is the top of the Material scale — there is no role above it | `LineoTypography` gains three named styles at 1.2× the Material ones, with line height scaled to match so nothing clips: `Expression` 68 sp, `Result` 54 sp, `KeypadLabel` 38 sp. Named rather than written at the call sites, so §10 still has something to point at and there is one place to change them |
| open | P0-14-2 | At 68 sp a long expression will run off the leading edge. §10 says the display "shrinks by step as the line grows", and nothing implements that yet | Harmless while lines are short, wrong the first time someone types a real one. **Close it with the shrink-to-fit step §10 already promises** — it belongs with P1-03, which owns the notepad line |
| 2026-08-17 | P0-13 | `%` is typed far more often than brackets, and four columns hold twenty keys | `( )` gives up its cell to `%`. Brackets survive only on the accessory row, which appears only with the text keyboard, so keypad mode can no longer type one at all — the same gap `^`, `√` and the argument separator are already in, and the same fix closes all four |
| 2026-08-17 | P0-13 | `Aa` sat in the grid looking like a key, when it is a mode | Lifted out of the grid entirely: a chip above `AC`, chip-sized rather than key-sized, so it is read as a mode before its label is. It lines up with `AC` beneath it — the other control that acts on the line as a whole. `KeypadState.modeKey` is separate from `rows` |
| 2026-08-17 | P0-13 | The freed cell became `±`, which needed a new `EditorCommand` variant | `ToggleSign`. Not an `InsertText("-")`, because pressing twice has to leave the line as it was and because only the editor knows where the current number starts. `toggleSign` is its own file with ten tests, the hardest being that the `-` in `15-2` is a subtraction and must not be stolen: `±` there gives `15--2`, which is 17. Confirmed on device |
| 2026-08-17 | P0-13 | `±` looked like a function key when it edits a number | Given the `Digit` role. An operator combines two numbers; `±` changes one, the same as typing another digit into it, and two keys that edit the same thing should not look like different kinds of key |
| 2026-08-17 | P0-13 | Keys were asked to grow by closing the gap between them | `KeyGap` 8 dp → 6 dp. The grid divides what the gap leaves, so a smaller gap is a bigger target with no other change |
| 2026-08-17 | P0-13 | `Aa` did not say where it goes | `ABC`, the label every software keyboard on the platform uses for this exact journey. It names the destination rather than the mechanism, needs no icon dependency, and pairs with the `123` already on the accessory row. An icon was considered and rejected: a keyboard glyph says "keyboard" but not "tap here to get letters", and it would have cost a dependency to say less |
| 2026-08-17 | P0-13 | Brackets were wanted back "in landscape", but `docs/ANDROID_STANDARDS.md` §2 forbids branching on orientation | Bound to width instead: a fifth column appears whenever the window is wider than compact, carrying `(`, `)`, `^`, `√` and the argument separator. A phone in landscape gets it, and so do a tablet held upright and an unfolded foldable — all for the reason that actually applies, which is room |
| 2026-08-17 | P0-13 | The fifth column overflowed the landscape pane, and then its labels clipped — `AC` rendered as a triangle, `±` as a plus | Two fixes, both found on device and neither visible to a snapshot. A key is square from the *scarce* dimension: width when stacked under a document, the row's height when in a side pane with a ceiling. And the label is now 42% of the key diameter rather than a fixed 38 sp, so it scales with whatever size the key ends up |
| 2026-08-17 | P1-01 | A reference has to be two things at once: an id, so that it survives every structural edit, and an ordinal, so that the user can read it. Storing either one alone loses the other | Stored text holds the id, displayed text holds the ordinal, and `LineReferences` is the only place that converts — the same shape as the locale boundary of `docs/CONVENTIONS.md` §1. References are found by lexing rather than by matching text, so which tokens count is decided by the grammar and not restated in the feature. One consequence is deliberate: the lexer stops at `//`, so a reference written inside a comment is left as typed, and it is not evaluated either |
| 2026-08-17 | P1-01 | Deleting a line that other lines reference. Refusing the deletion, silently dropping the reference, and repointing it at whatever takes the ordinal were all on the table | The reference is left pointing at the id, which is now gone: `LineReferences.MISSING_TARGET` (`line0`, valid as neither an id nor an ordinal) on screen, `UnknownIdentifier` from the engine, `danglingReferences()` for the editor to mark. `nextLineId` never goes back, so the id cannot be handed to a new line and quietly make an old reference resolve to something the user never meant. Repointing was the dangerous option: it produces a different number with no error at all |
| 2026-08-17 | P1-01 | The first version of the test suite passed with the id-to-ordinal conversion **deleted from the model** | In a freshly built document the id and the ordinal are the same number, so every assertion held on a model that stored ordinals — the design §3.7 rules out. Tests now build documents whose ids deliberately differ from their ordinals (two lines added and deleted first). Re-running the mutation with the stronger suite fails 6 of 11 tests. The weakness, and why the helper exists, is recorded in the test class KDoc |
| 2026-08-17 | P1-01 | Nothing maps `NotepadDocument` to the `documents` / `lines` rows of `docs/ARCHITECTURE.md` §6 yet, so a document cannot be loaded or saved | Closed by P1-03-3. `NotepadStore` does the mapping from `:feature:notepad`, against the `DocumentRepository` contract only, so `:core:data` was not edited and rule 3 held |
| 2026-08-17 | P1-02 | The task is `L`, and rule 5 says an `L` is split once it starts | P1-02-1 is the graph — edges, order, circles — which is a pure function of the document and testable on its own. P1-02-2 is the evaluation that walks it and the incremental step. The split falls where the thing under test changes from *shape* to *work done* |
| 2026-08-17 | P1-02-1 | How far does a variable reach? `docs/GRAMMAR.md` §3.1 says "if the user wrote `m = 5` **earlier**", and nothing says what happens when the definition is below | **A variable is visible only to the lines below its definition**, and where a name is defined twice the nearest definition above wins. It matches §3.1's wording, it matches how every notepad calculator on the market reads, and it makes variable edges strictly backwards — so a circle can only be built with a line reference, which is the case the user can see. **Worth a second opinion from the product owner**: the alternative, whole-document scope, would let a line use a total defined at the bottom |
| 2026-08-17 | P1-02-1 | May a line reference point downwards? §3.7 does not say | Yes. Evaluation follows the topological order rather than the document order, so `line3` on line 1 works — a document that puts its total at the top is a real way to write one. It is also the only way to make a circle, which is why circles are detected rather than prevented |
| 2026-08-17 | P1-02-1 | Cycle detection has to name *every* line in the circle, and a recursive walk would be as deep as the longest reference chain | Tarjan's algorithm with an explicit stack. A strongly connected component is exactly "the lines that reach each other", so the chain reported to `CalcError.CircularReference` is every line involved and not merely the one the walk closed on. The components also come out dependencies-first, so the evaluation order is a by-product rather than a second pass |
| 2026-08-17 | P1-02-2 | A line that reads a failed line: red, or something else? | `LineEvaluation.Blocked(cause)`, per `docs/ARCHITECTURE.md` §3 — "depends on line 3", not red. In a long document one mistake would otherwise paint most of the lines as errors, and none of them would be the user's |
| 2026-08-17 | P1-02-2 | Rebuilding the graph re-parses the document, which would put an O(lines) parse on every keystroke — the cost the incremental evaluator exists to avoid | `ParseCache`, keyed by the line's text **and** the scope it is read in. Never by the text alone: `2k` is two thousand until a line above defines `k` (§3.2), so a text-only key would serve the wrong reading of the same characters further down the same document. A keystroke on an ordinary line now costs two parses — the definition pass and the line's own — instead of two per line, and the graph hands its parses to the evaluator so nothing is parsed twice |
| 2026-08-17 | P1-02-2 | `LineEvaluation` repeats most of `EditorEvaluation` in `:core:ui` | Deliberate, and not shared. The document has two states the editor does not (`Blocked`, and a circle) and the editor has one the document must not (`Unfinished`, which belongs to the line being typed). Merging them would mean a module that owns neither. P1-03 maps one to the other, which is the right place for a translation between a model and a screen |
| open | P1-02-2 | The DoD figure — 50 ms for a keystroke on a 200-line document on an API 26 emulator — has **not** been measured on a device | What is asserted instead is the work: one evaluation per line the change actually reached, two parses for a keystroke on an ordinary line, and zero evaluations when nothing changed. On the JVM the worst case (a 200-line chain changed at the top, every line invalidated) takes **0.69 ms**, with a 25 ms ceiling in the test to catch an algorithmic regression. A build agent's JVM is not an API 26 device. **Close it with the Macrobenchmark of P1-10**, which owns device measurement, or on a device once P1-03 puts the notepad on screen |
| 2026-08-17 | P1-03 | The task is `L` and it spans `:core:ui`, `:feature:notepad` and storage, which rule 3 forbids in one task | Four: P1-03-0 (`:core:ui`, the reusable line edit), P1-03-1 (state), P1-03-2 (screen), P1-03-3 (persistence and back). The first exists because the notepad needs the same text edit the single-line editor already has, and the alternative was a second `±` |
| 2026-08-17 | P1-03-0 | `toggleSign` and the insert/backspace logic were `private` inside `EditorState`, so the notepad could only have copied them | Lifted into `EditedLine` and `applying(command)` — pure, public, `:core:ui`'s. `EditorState` delegates, and its eleven tests passed unchanged, which is what says the extraction did not change behaviour. `±` alone would have been forty lines and ten tests of drift |
| 2026-08-17 | P1-03-1 | Deriving the line's display text from the document on every keystroke rewrites references while they are being typed: `line12` passes through `line1`, which binds to a real line, so the next render would show whatever *that* line's ordinal is | The focused line keeps a draft of exactly what the user typed; the document is still written on every keystroke, so nothing is lost, but the display text is re-derived only when focus moves. Unfocused lines always render from what is stored, which is how a reference follows its target when a line is inserted |
| 2026-08-17 | P1-03-1 | Enter at the very start of a line: does the text move down to a new line, or does a blank line appear above it? | A blank line appears above and the line keeps its text **and its id**. Identity follows content, so a reference to it still reads what the user can see it pointing at — the guarantee P1-01 exists for. Moving the text to a new id would have quietly repointed every reference to that line, which is the exact failure the whole model is built to prevent |
| 2026-08-17 | P1-03-1 | The single-line editor debounces evaluation by 400 ms (`docs/SPEC.md`). Should the notepad? | No. That debounce paid for a full parse per keystroke; the notepad parses one line and evaluates only what the change reached, which measures 0.69 ms on a 200-line worst case. Waiting 400 ms to show a number that is already computed would be slower for no saving. Revisit if the device measurement owed by P1-02-2 says otherwise |
| 2026-08-18 | P1-03-2 | `%`, `^`, `√` and the argument separator were reachable only from the accessory row, which appears only with the text keyboard — the gap left open against P0-13 | Closed. The notepad has one chip row that sits above *either* surface, which is what `docs/ARCHITECTURE.md` §5 asked for. The keys themselves still come from `:core:ui` (`AccessoryRowState.keys`), so what belongs on that row stays that module's decision; the notepad only decides where it goes and drops the `123` switch when the keypad is up, since the keypad already carries `ABC` and two keys doing one thing is two places to look for it |
| 2026-08-18 | P1-03-2 | A notepad line cannot use the single-line editor's type: `LineoTypography.Expression` is 68 sp, and three of those fill a phone | Lines use `headlineMedium` for the expression and `headlineSmall` for the result — the same one-step-apart relationship, four steps down the scale, so the digits still line up column for column. A long line wraps rather than shrinking, which also answers the open P0-14-2 row for the notepad: at 28 sp there is no leading edge to run off. **The single-line display still has no shrink-to-fit** and that row stays open |
| 2026-08-18 | P1-03-2 | Which composable asks for focus when the keyboard comes up. The screen knows the surface changed; only the line knows whether its field exists | The line asks. A `FocusRequester` that is not attached throws when asked, and in a long document the focused line may be scrolled out of the viewport entirely — a screen-level request would have crashed exactly when the document was too long to see. The screen keeps the show-and-hide of the keyboard, which is not attached to anything |
| 2026-08-18 | P1-03-2 | A line was given both `focused: Boolean` and `caret: Int`, which can be set to disagree | One nullable `caret`. A line either has the caret, at a position, or it does not. It also brought the row under detekt's parameter limit, which is what surfaced it |
| 2026-08-18 | P1-03-2 | The DoD asks for no visible layout jump when the surface changes, and that cannot be seen in a snapshot — Paparazzi has no keyboard to raise. The notepad was also not reachable from the app: the shell still showed `SingleLineScreen`, and rule 3 keeps an `:app` edit out of a `:feature:notepad` task | Closed by P1-03-4, which puts the notepad in the shell and verified the swap on `Pixel_10`: the chip row is above both surfaces at one height, so only the keypad below it comes and goes, and the document does not move |
| 2026-08-18 | P1-03-3 | Who allocates a line id: the document, or the `lines` table? A reference binds to `lines.id` (§6), and the id has to exist the moment Enter is pressed — long before anything reaches disk | The document. Rows are written with the ids it already holds, through `updateLine`, and `appendLine` is never called. Asking Room would make every new line a suspending call; rewriting ids at save time would repoint references, which is the one thing P1-01 exists to prevent |
| 2026-08-18 | P1-03-3 | `lines.id` is one key space for the whole table, so a document whose counter was seeded from its own rows would hand out an id another document already owns | `NotepadStore.open` seeds the counter above the highest id **anywhere in the store**. It costs a read of every line, once, when a document is opened. A `MAX(id)` query would be cheaper and needs `:core:data`, which rule 3 keeps out of this task — worth doing when P1-06 or P1-07 next opens that module. The fake repository in the tests refuses a duplicate id exactly as the primary key would, so the constraint is asserted and not merely described |
| 2026-08-18 | P1-03-3 | "Back never discards work" — prompt to save, or save without asking? | Save without asking. The document is written as it is typed, debounced by 500 ms, and a stop flushes whatever the debounce was still holding. There is nothing for a prompt to protect: a dialog would only ask the user to confirm work they had already done. `NotepadAutosave.flush` is what a back press and a stop both call |
| 2026-08-18 | P1-03-3 | Only rows that changed are written, which is a claim a test has to make rather than a comment | The fake repository records every write. A keystroke on one line of three writes one row; saving an unchanged document writes none. Without that, a save that rewrote the whole document on every keystroke would look identical from the outside |
| 2026-08-18 | P1-03-3 | The task could not reach its own DoD — "verified across process death" — because nothing shows the notepad | Split. The mapping and the autosave are `:feature:notepad` and are done; the shell wiring is **P1-03-4**, a new `:app` task, which is also where `SingleLineScreen` finally goes |
| 2026-08-18 | P1-03-4 | `hiltViewModel()` lives in `hilt-navigation-compose`, which is not on the classpath, and adding a dependency is a §7 decision | The activity owns the view model, taken with `by viewModels()`. There is one screen, so nothing is lost; when navigation arrives it will bring its own decision about that library. `lifecycle-runtime-compose` and `lifecycle-viewmodel-ktx` are now **declared** rather than inherited — both were already on the release classpath and in the allowlist, the same situation as `window-core` at P0-11b-1 |
| 2026-08-18 | P1-03-4 | The final write was launched on `viewModelScope`, which a back press cancels in the same breath as it asks for the write | Flush goes to an application-scoped `CoroutineScope`, injected under an `@ApplicationScope` qualifier so it cannot be reached by type alone. The autosave stays on `viewModelScope`, where cancellation is correct |
| 2026-08-18 | P1-03-4 | On a device, the keyboard's return key wrote a newline *into* a line: the document then held one line with two expressions, evaluated the first, and the keyboard's composing buffer — still holding `x\n` — duplicated the line on the next keystroke | The field **refuses** a newline rather than accepting one and splitting afterwards: text that is the previous text with exactly one newline dropped in is the return key, and becomes `EditorCommand.NewLine`. Anything else carrying newlines is a paste, and `NotepadState.setText` makes one line of each. `singleLine = true` says this more directly and cannot be used — see the row below |
| 2026-08-18 | P1-03-4 | `singleLine = true` on the line's field crashes Paparazzi: `NoSuchMethodError: Thread.setPosixNicenessInternal`, from a handler thread its scroll path starts, which layoutlib has no implementation for | Not used. The newline rule is enforced in `onValueChange` instead, which is testable without a renderer. Two related guards came out of the same hunt: the line does not request focus and the screen does not raise the keyboard while `LocalInspectionMode` is true — a picture has no keyboard, and asking for one crashed the render rather than failing a comparison |
| 2026-08-18 | P1-03-4 | Verified on `Pixel_10`, which is what P1-03-2 and P1-03-3 were both left owing | Typing survives `am force-stop` with no user action at all (the autosave), and survives back-then-`am kill` (the flush). The return key splits the line, and the line typed after the split survives both. The surface swap moves nothing above the chip row: the document sits at the same offset with the keypad up and with GBoard up, docked and floating |
| open | P0-13 | **Pressing `ABC` does not raise the keyboard on the owner's machine.** On mine the system reports `mInputShown=true` and the field reports `focused="true"`, and after `pm clear` of GBoard the full keyboard appears — so the app's request is reaching the IME here. It does not reproduce for me, and it does reproduce for the owner | **Deferred by the owner, not resolved.** Next steps when picked up: confirm which IME is selected on the failing device (`adb shell settings get secure default_input_method`), check whether `mInputShown` is true there too, and if it is, the fault is in what the IME does with the request rather than in making it. Worth adding regardless: keep the keypad on screen until the `ime` inset is actually non-zero, so a failed switch never leaves the user with an accessory row and no keyboard |
| 2026-08-19 | P1-04 | The task spanned three modules: the keypad had one hardcoded layout in `:core:ui`, the module is `:feature:scientific`, and nothing in `:app` can reach a module screen | Split as P1-04-0 (`:core:ui` — the layout becomes a parameter), P1-04-1 (`:feature:scientific` — functions, keypad rows, screen) and P1-04-2 (`:app` — multibinding, route, back). The same shape as the P1-03 split, and for the same rule |
| 2026-08-19 | P1-04-1 | `:core:engine` already registers the entire scientific set (P0-09), and `ModuleRegistry` silently drops a module function whose name is taken. So what does `:feature:scientific` register? | The gap, and only the gap: `sec`, `csc`/`cosec`, `cot`/`cotan`, `sign`/`signum`, `trunc`, `min`, `max`, `hypot`. Re-registering `sin` would be dead code pretending to be a contribution, and moving the built-ins out of the engine into the module was rejected — it would edit three modules and leave a build without the module unable to evaluate `sin(30)`. A test asserts that none of these names is already a built-in, so the gap cannot silently close |
| 2026-08-19 | P1-04-1 | May a feature module do `Double` arithmetic? `atan2`, `sec` and friends all want one | No. The engine's `Precision` re-boxes every transcendental result at 15 significant digits and answers exact quarter turns exactly, and a second copy of that policy in a feature would drift from it. Every function here composes built-ins or works in `BigDecimal`. `atan2` is therefore **not** registered: it needs a `Double`, so it belongs in `:core:engine` if it is ever wanted |
| 2026-08-19 | P1-04-1 | `cot(45)` came out as `1.000000000000001414213562373096098`: `cos(45°)` and `sin(45°)` are the same number but arrive from different `Double`s, and DECIMAL128 division exposes the difference | A quotient is rounded to the significant digits of the *operands* — read from the values themselves, not restated as `15` here, so it follows the engine if the engine ever keeps one more digit. `cot` is also `cos / sin` rather than `1 / tan`, which is what makes `cot(90)` answer `0` where `1 / tan(90°)` would report the pole of a function the user did not write |
| 2026-08-19 | P1-04-1 | A reciprocal at its pole — `csc(0)` — is literally a division by zero, and `DivisionByZero` carries no span | Reported as `DomainError(csc, UNDEFINED)` instead, so the message names the function the user typed rather than an arithmetic step they never wrote, and the error points at the call site |
| 2026-08-19 | P1-04-1 | P1-04 was written as "landscape expands to the full scientific keypad", which `docs/ANDROID_STANDARDS.md` §2 forbids | Width class, exactly as the fifth column of P0-13 already is. Narrow gets two function rows, wide gets three and the fifth column with them. The task text is corrected above rather than left to be re-litigated |
| 2026-08-19 | P1-04-1 | Two `Paparazzi` rules in one test class fail with "Acquiring different scenes from same thread without releases" | The wide snapshot lives in its own class with its own device. The width class is provided to match the canvas — forcing `Medium` onto a 393 dp phone draws five columns in the space of four and asserts the opposite of what it claims |
| 2026-08-19 | P1-04-1 | Back inside a module screen needs `activity-compose`, which `:feature:scientific` does not have and §7 makes a human decision | The module does not handle back at all. The host wires the system back to `ModuleNav.back()` where it places the screen, which is P1-04-2 — `:app` already has the dependency |
| open | P1-03-3 | `QuantityFormatTest > Indian grouping is the platform's, not ours` fails: `hi-IN` groups as `1,234,567` on the JVM. `java.text.DecimalFormat` takes the *rightmost* grouping interval from a pattern, so `#,##,##0` gives it 3 and the lakh-crore grouping never happens — Android's ICU supports the secondary interval, the JVM the tests run on does not | **Open, and not caused by P1-04** — it fails on the working tree as it stood before. Either the grouping is inserted for these languages rather than delegated (and the file's "the platform's, not ours" claim is retired), or the assertion moves to an instrumented test where the platform really is ICU. Needs the owner: §3 makes Indian grouping a requirement, so the choice is where it is implemented, not whether |
| 2026-08-19 | P1-04-2 | A second destination with no navigation library, and P0-01 (Nav Compose or Nav 3) is still open until P1-10 | One `rememberSaveable` module id in `MainActivity`, and nothing else. It survives process death, a module still never sees a controller, and whichever library wins later replaces one `if` in one file. Building a nav graph now would be answering P0-01 by accident |
| 2026-08-19 | P1-04-2 | The notepad evaluated against `FunctionRegistry.BUILTIN`, so `sec(60)` typed there was an unknown name while the same call worked on the scientific screen — the exact split `AGENTS.md` §1 forbids | `NotepadViewModel.open` takes the registry, the same way it takes the title: filtering it needs the locale, and a `ViewModel` may not read one. `ModuleWiringTest` asserts both halves agree, and asserts the old behaviour fails, so the wiring cannot quietly come undone |
| 2026-08-19 | P1-04-2 | The overflow button opened nothing — the defect recorded against P0-11b-2 as something that must not ship | It opens the module list now, built from `ModuleRegistry.visible(...)` rather than from a list in the menu. History (P1-06) and settings (P1-07) join the same menu without it being edited again |
| 2026-08-19 | P1-04-2 | `?attr/colorControlNormal` in the module icon failed resource linking: it is an AppCompat attribute and the app theme is Material 3 | The vector carries no tint of its own. It is drawn in white and tinted by whatever composes it, which is what a Compose icon expects anyway |
| 2026-08-19 | P1-04-2 | Verified on the emulator (`sdk_gphone16k_x86_64`, API 37, 1080×2424, portrait and landscape) | The overflow menu lists `Scientific`; opening it, typing `sin(30)` on the function keys and pressing `=` renders `0.5`; back returns to the notepad; `sec(60)` typed into a notepad line renders `2`. Two defects were found doing it, and both are fixed below |
| 2026-08-19 | P1-04-1 | On a compact phone the expression was **invisible**: `uiautomator` reported the field at 42 dp of height where the text is 68 sp. Two extra keypad rows had taken the window | The narrow layout is one function row, not two — a key is a quarter of the width, so a row costs a quarter of the width in height. The real fix is P1-04-3; this is what a compact window can afford even with it |
| 2026-08-19 | P1-04-1 | Rotating the phone emptied the line: `EditorState` is remembered, not saved, and a rotation recreates the activity | The text and caret are `rememberSaveable`, mirrored out of the editor with `snapshotFlow` and restored into a new one. A `ViewModel` would have been the other answer and costs a dependency the module does not have (`hilt-navigation-compose`, `lifecycle-viewmodel-compose`) |
| 2026-08-19 | P1-04-3 | `AdaptivePane` documented that the input pane "must size itself to its content", and a keypad's content is rows × key width. Nothing stopped that from exceeding the window — a six-row grid asked for more height than a phone has, and the document was measured, drawn and clipped to nothing | The document pane is guaranteed a minimum height — one expression line plus its result, 170 dp — and the input pane may have everything else. `Keypad` derives one key size from the scarcer of what the width allows and what the height allows, so a grid that would have overflowed shrinks its keys. A seven-tenths share was tried first and rejected by the product owner: it shrank the four-column keypad too, which nobody had asked for. A height is also the honest unit here, since what must fit is a line of text and not a fraction of a window |
| 2026-08-19 | P1-04-3 | The keypad had two sizing paths — `weight` per row in a side pane, `weight` per key stacked — and neither knew the pane's height | One path. `BoxWithConstraints` gives the grid its box, `keySize` picks the binding constraint, and every key is that size. An unbounded height (a preview, or a scrollable parent) falls back to the width, which is what every recorded snapshot was rendered with |
| open | P1-04-1 | The compact scientific keypad cannot reach `(`, `)`, `^`, `√` or the argument separator: they live in the fifth column, which only a wider window has, and on the accessory row, which only appears with the text keyboard | The same gap P0-13 recorded and the notepad closed with a chip row above **both** surfaces. That row is `NotepadChipRow` in `:feature:notepad`, and a feature may not read another's code. **Lift it into `:core:ui` and use it from both screens** — a `:core:ui` task, worth doing before P1-05 adds a third surface |
| 2026-08-19 | P1-04-3 | The chip row began 6 dp from the edge and the keypad 16 dp, so `(` did not line up with `ABC` or `AC` below it; and once keys shrank, centring the leftover moved the whole grid inwards and broke the same alignment from the other side | One edge for everything docked: `KeypadEdge` for the accessory row and the notepad chip row, and the keypad's spare width goes into the gaps rather than into a centred margin. The leading key keeps the pane's edge, the trailing one keeps the other, and the column under `ABC` is `AC` again — which is what `KeypadState` had always claimed |
| 2026-08-19 | P1-05 | The task spans `:core:engine` (module units never reached the evaluator, and data has no dimension), `:feature:converter` and `:app`, which rule 3 forbids | Split as P1-05-0 (engine — `UnitRegistry` per evaluation, `Dimensions.information`), P1-05-1 (the module) and P1-05-2 (the wiring), the same shape as P1-03 and P1-04 |
| 2026-08-19 | P1-05-0 | `ModuleRegistry.units()` collected `UnitDefinition`s that nothing ever read: `Evaluator` resolved symbols through `object UnitRegistry`, so a module's catalogue could not be named in an expression | `UnitRegistry` is a class with a `BUILTIN` instance and `with()`, exactly like `FunctionRegistry`, and `EvalContext` carries one. Rejected: putting all ten categories in the engine's static set — it would leave `units()` dead for `:pack:tax-*` and for currency in Phase 2, which need the same road |
| 2026-08-19 | P1-05-0 | Angle units and temperature deltas are looked up by name from inside `Angles` and `QuantityArithmetic`, neither of which is given an `EvalContext` | Both read `UnitRegistry.BUILTIN` explicitly. They are the grammar's own units — `docs/GRAMMAR.md` §3.8 names them, and no module may replace them — so the fixed set is the right one, and arithmetic stays free of a context parameter it has no other use for |
| 2026-08-19 | P1-05-0 | Data has no dimension: `Dimensions` holds the seven SI base quantities plus currency | `information` added, for the same reason currency is there. Without it `5 KiB + 3 kg` would be accepted, and ISO 80000-13 treats information as a quantity of its own |
| 2026-08-19 | P1-05-0 | `1 KiB / 2 s` evaluated to `0.5 KiB·s`, which looked like a unit-exponent bug | Not a bug. `docs/GRAMMAR.md` §2 puts implicit multiplication at the same level as `/` — `6/2(1+3)` is `12` — so it reads `(1 KiB / 2) × s`. The golden file already parenthesises `100 m / (10 s)` for this reason, and the new test does too |
| 2026-08-19 | P1-05-0b | `5 cm to in` was a syntax error, so the converter could not offer inches at all: §3.3 makes a bare `in` the conversion keyword and only the inch directly after a number. Writing the target as `to 1 in` fails too — a conversion target must be a bare unit expression | The parser reads `in` as the inch when it opens a conversion target. A target cannot itself be a conversion, so the ambiguity the rule exists to resolve cannot arise there. `5 in 3` still reports `Syntax` on the `3`, and `to in^2` works because everything after the symbol parses as usual. Two golden cases added; §3.3 updated |
| 2026-08-19 | P1-05-1 | Ten categories, but `m²` and `km/h` are not lexable symbols — `Char.isLetter()` rejects `²`, and `m2` is neither ISO nor a unit | A unit on the screen is a label and an expression: `m²` shows, `m^2` is typed. Registering `m2` would have put a fake symbol in the engine to save the screen a string |
| 2026-08-19 | P1-05-1 | ISO 80000 gives `a` to both the are and the year | Neither is registered under it. Area gets `ha` and `acre`, time gets `wk` and `yr`, and a calculator that guessed between them would be silently wrong half the time |
| 2026-08-19 | P1-05-1 | `gal`, `qt`, `pt`, `floz` and `cup` differ between the US and the imperial systems | US customary, since that is the market this ships to first, recorded in the file. The imperial gallon is a different unit, not a different spelling; if it is ever wanted it needs its own symbol |
| 2026-08-19 | P1-05-2 | On a device the from/to pickers rendered at `[0,0][0,0]`: the converter's document is four rows tall, and `AdaptivePane` guarantees only one expression line and its result, so the last child was measured into nothing | `minDocumentHeight` becomes a parameter of `AdaptivePane`, defaulting to what it already promised. A screen knows how tall its document is; the pane does not, and guessing for everyone would shrink the keypad on screens that never needed it |
| 2026-08-19 | P1-05-2 | Verified on the emulator (`sdk_gphone16k_x86_64`, API 37) | Converter: category chips, `cm → km` picker, `52 cm` renders `0.00052 km`. Notepad: `2 GiB to MB` renders `2147.483648 MB` — the module's catalogue reaching the other surface, which is the definition of done |
| 2026-08-19 | P1-06 | Nothing called `HistoryRepository.record`, and `:app` cannot see a module screen's evaluations: `CalculatorModule.Screen(nav)` has no history hook | The notepad first, modules later. Adding a recorder to the module contract would touch `:core:registry` and both features in one task — rule 3 — and would fix the shape of a hook before there are two real users of it. **Open**: `=` on the scientific screen and a conversion on the converter screen still record nothing |
| 2026-08-19 | P1-06 | When is a notepad line "done"? There is no commit — every keystroke is evaluated, so recording each one would put `1`, `1+`, `1+2` on the tape and fill 50 entries with one calculation | **When the caret leaves it.** Pressing `=`, tapping another line and closing the screen are the same act, and all three reach it. The same line is never recorded twice in a row; editing it and leaving again records the new text, because that is a different calculation |
| 2026-08-19 | P1-06 | What does "tap to reuse" reuse — the expression or the result? | The expression. It is what can be recalculated and edited, and the result is already on screen beside it. It lands on a **new** line rather than in the line the caret is in: half-typed work is not something to overwrite. A blank line is written into rather than pushed down |
| 2026-08-19 | P1-06 | The tape stores `resultText`, and the notepad shows a locale-formatted result | Recorded formatted, through `QuantityFormat` with the locale the line was read in — history is a record of what the user saw, and re-evaluating it later under another angle mode or locale would rewrite the past. The view model gets the locale the same way it gets the title |
| 2026-08-19 | P1-06 | Verified on the emulator (`sdk_gphone16k_x86_64`, API 37) | A line committed with the return key appears on the tape with its grouped result (`71,136`); the entry survives `am force-stop` and is there on the next launch; tapping it closes the tape and writes the expression onto a new notepad line; an empty tape says so rather than showing a blank screen |
| open | P1-06 | A line the caret is still in is lost from the tape when the process is killed outright — `am force-stop` fires no `ON_STOP`, so the flush never runs. The document survives, because the autosave writes as you type; the tape does not | Left as is. The tape is a record of finished calculations and a killed process leaves the last one unfinished, but this is worth revisiting if the periodic autosave ever gains a partner for history. **Not** a reason to record on every evaluation |
| open | P1-06 | The history screen has no Paparazzi snapshot: `:app` does not apply the plugin, and adding it there is a build decision rather than a drive-by | Covered by unit tests and a device run instead. Worth applying the plugin to `:app` when the next screen lands there (P1-07 settings) |
| 2026-08-21 | P1-07 | `QuantityFormatTest` asserted `12,34,567.89` for `hi-IN` and failed on this machine's toolchain — a failure that predates P1-07 and is not caused by it | `java.text.DecimalFormat` keeps a single grouping size, so `applyPattern("#,##,##0")` is read as plain groups of three and the lakh pattern cannot be expressed at all. The integer part is regrouped by hand for the `docs/CONVENTIONS.md` §3 languages; the separators are still the locale's, read back from the formatter, so a separator override moves them with it. Rejected: changing the expectation — §3 makes Indian grouping a requirement — and adding ICU, which is a dependency decision under §7 |
| 2026-08-21 | P1-07 | A setting is only real if it reaches every surface, and only the notepad read one: the converter and the scientific screen built their own formatter and their own `EvalContext` from `LocalConfiguration`, so a comma keypad typed into a screen that read `en-US` | Four values are resolved once by the shell and provided: `LocalNumberLocale`, `LocalAngleMode`, `LocalDecimalSeparator` and `LocalQuantityFormat`. No screen may read the device configuration for numbers. `ResolvedSettings` carries the angle mode unresolved, so a screen is told everything the settings decide in one object |
| 2026-08-21 | P1-07 | The task is listed as `:app`, but the plumbing above touches `:core:ui`, `:feature:converter`, `:feature:scientific` and `:feature:notepad` — rule 3 | Done as one task rather than split. What crosses the modules is a single value type per boundary and no logic: `:core:ui` declares the four locals, and each screen replaces a `LocalConfiguration` read with one of them. Splitting it would have left a release where the keypad and the engine disagree on every module screen, which is worse than the rule it protects |
| 2026-08-21 | P1-07 | The tape formats with the settings, but the settings can change while a notepad is open | `NotepadHistory.format` is a `var` the route updates through `NotepadViewModel.apply`. What is already on the tape is left alone — it says what the user saw at the time — and a line finished after the change is recorded the new way. `NotepadState.evaluateWith` carries the new separator with the new evaluator for the same reason: `±` has to look for the character the keypad now types |
| open | P1-07 | The settings and licences screens have no Paparazzi snapshot, for the reason recorded against P1-06: `:app` does not apply the plugin | Still not applied. It is a build decision — a test-only plugin on a module that has none — and `AGENTS.md` §7 reserves it. **Ask before P1-08**, which is the accessibility pass over exactly these screens |
| 2026-08-21 | P1-07 | On the emulator a comma-separator user's first line read `Unexpected ','`, and correcting itself only after something else changed | The stored settings arrive from DataStore *while* the first document read is in flight, so `open` had already captured the defaults and `apply` had no state to re-read. The view model now keeps the newest context and separator in fields and builds the state from those when the document lands. The test that fails without the fix is `settings that arrive during the first read still reach the document` |
| 2026-08-21 | P1-07 | With Imperial chosen, the converter still opened on `m → km` — the unit-system setting reached the screen and did nothing | A class-initialisation cycle. Every `ConverterCategory` constant is built by `converterUnits`, a top-level function in the same file, so constructing the constants runs the file's initialiser and `IMPERIAL_PAIRS` was built while every constant was still null — eight entries keyed on null, every lookup a miss, no error anywhere. It is `by lazy` now, and three tests cover the pairs. **The trap is the file layout**, and the same file already carries a comment about it for the companion object |
| 2026-08-21 | P1-07 | `AUTO` for the unit system was resolved inside `MainActivity`, where no test can reach it | Moved to `ResolvedSettings`, which is where every other locale question is answered. It resolves against the *chosen* locale rather than the reading one: a separator override can move the reading locale to Germany, and choosing a comma is not a statement about whether the user weighs things in pounds |
| 2026-08-21 | P1-07 | Verified on the emulator (`Pixel_10`, cold boot) | The separator setting changes the decimal key and the argument separator on return from settings, and the reading locale with them — the comma line that parsed before now reports `Unexpected ','`, which is the two agreeing rather than disagreeing. Imperial opens the converter on `ft → mi`. `sin(30)` is `0.5` in DEG and `-1…` in RAD, cut at the one decimal place the settings ask for. The licences screen lists `androidx.activity:activity` as Apache-2.0 and opens the full text |
| 2026-08-21 | P1-08 | The pass touches `:core:ui` and every surface at once — rule 3 again | Split as P1-08-0 (the reader and the shared chip), P1-08-1 (notepad), P1-08-2 (the other surfaces) and P1-08-3 (the device pass, which needs all of them present). The same shape as P1-04 and P1-05 |
| 2026-08-21 | P1-08-0 | What TalkBack says has to come from the AST, and the AST lives in `:core:engine` — a module `CLAUDE.md` puts behind plan mode | Nothing in the engine changed. `Engine.parse` is already public and `Ast` is already a sealed interface with every node exposed, so the reader is a walk over a tree the engine hands out, written in `:core:ui` where the words are. The engine stays a library that knows no locale and no screen |
| 2026-08-21 | P1-08-0 | Does the editable line get a spoken description too? | No. In a text field TalkBack reads what is actually there, character by character, and a description would fight the caret — the two would stop agreeing mid-edit. Only a line that is *not* being edited is spoken from its tree, which is also the only line a reader is reading rather than writing |
| 2026-08-21 | P1-08-0 | A selected chip differed from an unselected one by container colour alone, which §8 forbids outright | `ChoiceChip` in `:core:ui`: bolder label plus the accent, and `Modifier.selectable` so the state is spoken. Three screens were drawing the same box; one shared chip means the next fix reaches all of them. Converter snapshots re-recorded for the bolder label |
| 2026-08-21 | P1-08-0 | `keySize` took the smaller of what width and height allowed, with no floor — a landscape pane with the keyboard up produced a 34 dp key | Floored at 48 dp. A grid that cannot fit overflows its pane instead, which is visible; a key too small to hit is not. The snapshots could not see this, so the function is `internal` now and `KeySizeTest` asserts it in the windows no snapshot covers |
| open | P1-08-3 | TalkBack's spoken output cannot be captured from the shell, and `uiautomator dump` never returns on this emulator — `UiAutomation` times out connecting, on a wedged snapshot and on a cold boot alike | **Unresolved.** The labels are asserted in code and in unit tests; what is *not* asserted is that TalkBack reaches every one of them in order. Either instrumented Compose tests (`ui-test-junit4` plus an `androidTest` source set — a dependency decision under §7, and the two libraries are already in the version catalog unused) or a human with a real device. **Needs a human decision** |
| open | P1-08-3 | `ar-XB` could not be applied: `cmd locale set-app-locales` needs `android:localeConfig`, which the manifest does not declare, and the system locale needs root, which a Play system image does not give | RTL layout itself is covered by Paparazzi in `:core:ui`, `:feature:notepad` and `:feature:converter`, which render right-to-left. What is missing is the pseudo-locale's text expansion and real bidi. **Declaring `localeConfig` belongs to P1-09**, which owns localisation plumbing — do the `ar-XB` run there, on the manifest P1-09 leaves behind |
| 2026-08-21 | P1-08-3 | Instrumented tests and Paparazzi on `:app` — the two decisions §7 reserves | Both approved by the product owner. `ui-test-junit4`, `espresso-core` and `androidx.test:junit` were already declared in `:app` from the P0-01 template and unused; Paparazzi is already applied in two modules and is test-only. Neither reaches the APK |
| 2026-08-21 | P1-08-3 | TalkBack could not be driven from the shell: `uiautomator dump` never returns on this emulator, and swipe gestures injected with `input swipe` do not reach TalkBack's gesture detector | `ShellSemanticsTest` asserts the tree TalkBack walks instead — labels, selected state, click labels, state descriptions. **Still open**: a human listening to it. A test can prove a label exists; it cannot prove the order makes sense out loud |
| 2026-08-21 | P1-09 | Nothing fails the build on a hardcoded string: Android's `HardcodedText` reads layout XML, and every screen here is Compose, where a literal is an ordinary argument | `checkHardcodedText` in `build-logic`, hung off `check`. It reads seven argument names — `text`, `label`, `title`, `subtitle`, `description`, `contentDescription`, `onClickLabel`, `stateDescription` — and flags a literal only when it still contains two letters in a row once interpolations are removed, so `"$title: ${label(option)}"` passes and `"Clear history"` does not. A line may opt out with `not-translatable: <why>`. Verified by writing a violation and watching the build fail |
| 2026-08-21 | P1-09 | `ABC` on the keypad and `123` on the accessory row are words, not notation — a Cyrillic user should see their own three letters | `KeypadKey.labelRes`, resolved by `keyLabel` where the key is drawn, so a layout stays a pure function with no `Context`. Every other label — `×`, `√`, `sin⁻¹`, `log₂` — is ISO notation and stays in code, as named constants so the distinction is stated once rather than argued with per line |
| 2026-08-21 | P1-09 | `TokenShowcase` lives in `src/main` but is reachable only from its own snapshot test, and its sample text tripped the new check | Moved to `src/test`. It is a fixture that exists to be photographed; nothing in the app has ever composed it, and calling it main source was the thing that was untrue |
| 2026-08-21 | P1-09 | `ar-XB` on a device needs root on a Play image, and `cmd locale set-app-locales` needs a `localeConfig` | Both fixed the way P1-09 owns: `isPseudoLocalesEnabled` generates `en-XA` and `ar-XB` for the debug build, and `android:localeConfig` is declared with the one locale that actually has strings. The pseudo-locales are then rendered as snapshots — `DeviceConfig.copy(locale = "b+ar+XB", layoutDirection = RTL)` — which is where a truncation can be looked at rather than glimpsed. This closes the `ar-XB` half of P1-08-3 |
| 2026-08-21 | P1-09 | A Paparazzi device config in `ar-XB` mirrors the *resources* but leaves the layout left-to-right: layoutlib does not derive `LocalLayoutDirection` from the configured locale | The mirrored snapshots override `LocalLayoutDirection` as well, so the picture shows what a device shows — both flips at once. Recorded because the next person to write one will otherwise think the override is redundant |
| 2026-08-21 | P1-09 | Paparazzi refuses two `@Rule` instances in one class — "Acquiring different scenes from same thread without releases", and every test in the class fails, not just the second | One device per class. The pseudo-locale snapshots live in `ShellExpandedLocalePaparazziTest` and `ShellMirroredLocalePaparazziTest`, with the fixtures they share in `ShellFixtures.kt` |
| 2026-08-21 | P1-08b | With a hardware keyboard and no line focused, typing `12+3` opened the settings screen: the keys reached whatever had view focus, which was the overflow button | The notepad screen is focusable and takes what no field wanted through `onKeyEvent` — *after* the focused field rather than before it, so a line being edited still gets its own keys first. Each character becomes the same `EditorCommand` the equivalent keypad key sends, so a keyboard and a thumb reach the document by one path (`docs/ARCHITECTURE.md` §5) |
| 2026-08-21 | P1-08b | An arrow at the first line or the last was handed back to the platform, which looked polite and moved view focus onto the overflow button — after which the next character typed landed on a button | Up and down are consumed whatever they find. While a notepad is on screen they are the notepad's; Tab is how a keyboard leaves it. `NotepadState.moveFocusBy` still reports whether it moved, because that is a different question from whether the key was ours |
| 2026-08-21 | P1-08b | The `ABC` key showed `AB` at the maximum accessibility font size | It was pinned to a 48 dp square while its label scaled — the one key whose label is a word rather than notation. A minimum size now, so it becomes a pill. The grid keys were never at risk: their labels are sized from the key, not from the system font |
| 2026-08-21 | P1-08b | Verified on the emulator (`Pixel_Fold`, inner display, 2076×2152) | `12+3` typed with nothing focused lands on line 1 and answers 15; Enter opens line 2 and `7*6` answers 42; down then up returns to line 1 and `0` is inserted at the caret; two rights and `9` give `0129+3`. A screencap on this AVD needs `-d <display-id>`, since a foldable reports two |
| open | P1-08b | The tablet snapshot shows the keypad taking the upper half of a 1000 dp-tall pane, with the lower half empty | Left alone. `AdaptivePane` gives the input pane what it asks for and the keypad asks for its grid; stretching keys to fill a tablet would make them targets the thumb cannot reach anyway. **Worth a design decision before P1-11** — a docked keypad at the bottom of the pane is the obvious alternative |
| 2026-08-21 | P1-10 | The task is a harness and a set of numbers, and only one of them is code | Split: P1-10-0 (the release build), P1-10-1 (the benchmark module), P1-10-2 (the database, which was already right) and P1-10-3 (the measurements, which need hardware). The first three are done; the last is what "performance pass" still owes |
| 2026-08-21 | P1-10-0 | R8 was never on: the convention plugin configured `debug` and left `release` at AGP's defaults, so nothing had ever been shrunk or tested | `isMinifyEnabled` and `isShrinkResources` on release, with `proguard-android-optimize.txt` and a rules file that keeps line numbers and nothing else. The build is clean at 1.6 MB — an eighth of the 12 MB ceiling — and CI now fails on the ceiling rather than trusting it |
| 2026-08-21 | P1-10-0 | Where does `reportFullyDrawn()` belong? The activity has no idea when the document has been read | `ReportDrawnWhen { notepad != null }` in `NotepadRoute`, which is the composable that knows. Startup then measures to the moment the app is *usable* rather than to the first frame, which is an empty notepad — the difference Macrobenchmark reports as `timeToFullDisplay` |
| 2026-08-21 | P1-10-1 | `androidx.baselineprofile` 1.4.1, the stable line, cannot apply on AGP 9.3.1: it asks for the `TestExtension` type AGP 9 replaced | Raised to `1.5.0-rc01`, the line that targets AGP 9 — the same trade P0-12 made for Paparazzi, and for the same reason: the plugin and the macrobenchmark library are build-time and test-only, so nothing pre-release reaches the APK. `profileinstaller`, the half that *does* ship, stays on stable 1.4.1 and was already in the allowlist as a transitive of Compose |
| 2026-08-21 | P1-10-1 | The benchmark module needs `minSdk 28`, while the app ships to 26 | Macrobenchmark reads the system traces it measures from, and those start at Android 9. It is the instrument that needs the newer device, not the app — but it means **the DoD's API 26 number cannot come from Macrobenchmark at all**. That measurement is `am start -W` on an API 26 device, and it is part of P1-10-3 |
| 2026-08-21 | P1-10-1 | The journey presses keys by their *spoken* label — `By.desc("Add")` | The accessibility work of P1-08 is what makes the keypad drivable at all: a circle with a glyph has nothing else to find it by. Worth knowing before someone "tidies up" a content description |
| open | P1-10-3 | No numbers were taken. The only system image on this machine is API 37 Google Play, which is not rooted, and the emulator is software-rendered | Three consequences, all hardware: `BaselineProfileRule` cannot generate a profile on a Play image (it needs root or a userdebug build); a Macrobenchmark run on a software-rendered emulator did not get past setup in twenty minutes; and API 26 is not installed, so the cold-start target has nothing to be measured on. **Needs a physical device**, or a decision to download a `google_apis` image and accept emulator numbers for the trend rather than the target |
| open | P1-10-1 | `:benchmark` applies `com.android.test` directly rather than a convention plugin, so it is outside `detekt`, `lint` and the licence gate | Accepted for now: it ships nothing, and its dependencies landed in the test allowlist through `:app`. A `lineo.android.test` convention plugin would fix it and is worth writing when a second test module appears |
| open | P0-01 | Navigation Compose or Navigation 3 — the row above says "decide before P1-10 (app shell)", and P1-10 has now been done without touching navigation | Still unresolved, and still not blocking: the shell is one `when` over `rememberSaveable` flags. The next thing that forces it is a screen that needs a back stack of its own. **Decide before P1-11**, which is where the store listing fixes what the app is |
| 2026-08-21 | P1-10b | Both backup files were the untouched Android Studio template, TODO comment and all — so every document a user had written was going to Google's cloud backup by default | Cloud backup now excludes every domain; device transfer keeps the database and files. The distinction is the point: a transfer copies to the new phone during setup, end-to-end encrypted and never on a server, which is what someone replacing a phone expects of their notepad. `allowBackup` stays true because turning it off would take the transfer with it and protect against neither |
| 2026-08-21 | P1-10b | The permission list is *empty*, where the DoD says "exactly `INTERNET` + `ACCESS_NETWORK_STATE`" | Empty is the honest state: both are for currency rates and ads, and neither exists yet. `docs/ANDROID_STANDARDS.md` §5 is about minimisation, so a permission arrives with the change that needs it — P2-03. The one entry in the merged manifest is androidx.core's own signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which it grants to itself to keep its runtime receivers unexported |
| 2026-08-21 | P1-10b | A network security config with no network code looked like paperwork | Written anyway, and deliberately *first*: a config added before the first request cannot be forgotten by the change that makes the first request, and an `http://` URL then fails at the socket instead of quietly working in development. There are no per-domain exceptions and adding one is a §7 decision |
| 2026-08-21 | P1-10b | "A hostile or malformed rate payload must yield `RateUnavailable`" cannot be satisfied: there is no rate fetcher, no parser and no `RateUnavailable` — only the Room cache tables | Deferred to **P2-03**, where the fetcher is written, and restated there rather than left as a tick nobody can defend. What P1-10b *can* do for it — refusing cleartext before the first request exists — is done |
| 2026-08-21 | P1-10b | The §7 checklist found one row genuinely broken: `NotepadScreen` collected its state with `collectAsState` | Now `collectAsStateWithLifecycle`, which §1 requires without exception. `lifecycle-runtime-compose` was already on the release classpath and in the allowlist, so the module declares what it uses and nothing new ships |
| 2026-08-21 | P1-10b | Verified on the emulator — `ManifestSecurityTest`, 3 tests, against the installed package rather than the source manifest | The manifest in `src/main` is not what ships: every library merges into it, so "is the permission list exactly this?" is only answerable from `PackageManager`. A permission that appears without a line in that test is the test doing its job |
| 2026-08-23 | P1-15-0 | The task was "lift `NotepadChipRow` into `:core:ui`", and `:core:ui` turned out to already have `AccessoryRow` — the same row, drawn the same way, differing only in that it always docks and cannot take a screen's own chips | Lifting as asked would have left three near-identical rows. `AccessoryRow` is now a five-line call to `ExpressionChipRow`, so there is one implementation of a chip strip and two names for uses of it. Its single caller — the scientific screen, which is the one with the defect — did not change |
| 2026-08-23 | P1-15-0 | How to prove a refactor of drawing code changed no pixel | Not by looking. `AccessoryRow`'s snapshots were recorded before this row existed and the drawing moved underneath them, so `verifyPaparazziDebug` answers the question directly: 122 existing snapshots passed, and `git status` showed the new PNGs added and **no existing one modified**. That is the DoD, and the new snapshots are only the part it cannot cover |
| 2026-08-23 | P1-15-0 | The first snapshot named "keys and a screen's own chips after them" did not contain the chips: the row scrolls, and the full key set pushed `subtotal` and `km` past the right edge | Caught by opening the picture rather than by trusting a green test — a passing snapshot proves only that the pixels have not changed since they were recorded, never that they show what the name says. The test now takes two keys, and a constant records why |
| 2026-08-23 | P1-15-0 | `AccessoryRowState.press(key: KeypadKey)` did not fit a row that also draws chips a screen contributes, which have a command and no key | Changed to take the `EditorCommand`, which is all `press` ever did with the key. Two assertions in `InputSurfaceTest` gained a `.command`; nothing else called it, and it is `:core:ui`'s own API, so no feature noticed |
| 2026-08-23 | P1-15-1 | Where the `NotepadSuggestion` → `ExpressionChip` mapping belongs, once the row is shared | In the notepad, with the strings. `:core:ui` draws a chip and knows nothing about why it is being offered; "insert the variable subtotal" is a sentence about the document, and `ExpressionChip` therefore carries resolved text rather than a resource id. The alternative — moving the strings into `:core:ui` so it could format them — would have put a feature's vocabulary in a module every feature shares |
| 2026-08-23 | P1-15-1 | How to know the notepad still draws what it drew, after its chip row was replaced by a different implementation | The 10 `NotepadScreenPaparazziTest` snapshots, unchanged, plus `git status` showing no PNG rewritten. `NotepadChipRow` never had a test of its own — it was only ever covered through the screen — which is exactly why the screen's snapshots are the right guard here |
