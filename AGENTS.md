# AGENTS.md

Single source of truth for every AI coding agent working in this repo.
`CLAUDE.md` and `GEMINI.md` are thin pointers to this file. Do not duplicate rules there.

---

## 1. What this project is

**Lineo** is an Android calculator for the global market.

| | |
|---|---|
| Product name | Lineo |
| Play Store title | `Lineo: Notepad Calculator` (25 chars) |
| Launcher label | `Lineo` |
| `applicationId` | `app.lineo` — provisional until the domain is secured. Permanent after first Play release. |
| Namespace root | `app.lineo.<module>` |

The name comes from *linea*. It is not decoration: this is a line-based calculator
where lines reference other lines. Keep that idea visible in naming throughout the
code — `LineId`, `LineRef`, `lineDependencies`.

Two surfaces over one engine:

- **Notepad mode** — free-form multi-line document. Each line is an expression, lines
  can reference each other and define variables. This is the product's differentiator.
- **Module mode** — focused calculators (scientific, converters, finance, date/time),
  each registered as a plugin.

Both surfaces call the same expression engine. A function registered by a module is
automatically available as text in notepad mode. This is not optional — it is the
core design constraint.

**Monetisation:** free tier with unobtrusive ads, one-time premium purchase, optional
tip jar via Play Billing.

---

## 2. Non-negotiables

Never violate these without an explicit human decision recorded in the relevant doc.

| Rule | Why |
|---|---|
| All money/decimal math uses `BigDecimal` with `MathContext.DECIMAL128`. Never `Double` for user-visible arithmetic. | `0.1 + 0.2` must print `0.3`. |
| Every **shipped** dependency must be Apache-2.0, MIT, or BSD. No GPL, LGPL, or dual-licensed-commercial. Test-only dependencies are recorded but not restricted — they are never distributed. | Closed-source paid app. See `docs/ARCHITECTURE.md` §7. |
| The engine never throws for bad user input. It returns `Result<Quantity, CalcError>`. | Errors are UI state, not exceptions. |
| Internal representation is locale-free. Locale formatting happens only at the input and display boundaries. | See `docs/CONVENTIONS.md` §1. |
| No backend server. Currency rates are fetched device-side and cached in Room. | Keeps operating cost at zero. |
| Never log user expression content to analytics or crash reporting. Log error type and position only. | Expressions contain salaries, debts, personal data. |
| Every stored document carries `schemaVersion`. | Format will change; migrations must be possible. |
| No ads in the calculation path. | See `docs/SPEC.md` §5. |
| ViewModels hold no `Context`, `Activity`, `Resources`, or `Application`. Composables never touch a data source directly. | Android architecture guidance. See `docs/ANDROID_STANDARDS.md` §1. |
| No one-shot event channels from ViewModel to UI. Handle the event, update state. | Same. Errors are state, not fired events. |
| Layout branches on window size class, never on device type or raw width. | `docs/ANDROID_STANDARDS.md` §2. |
| Request no permission beyond `INTERNET` and `ACCESS_NETWORK_STATE` without a human decision. | `docs/ANDROID_STANDARDS.md` §5. |

---

## 3. Module map

```
:app                  host, navigation, DI wiring, Play Billing
:core:engine          lexer, Pratt parser, evaluator, Quantity, CalcError
:core:registry        CalculatorModule interface + registry
:core:ui              design system, keypad, editor, shared composables
:core:data            Room, DataStore, currency cache
:core:billing         Play Billing, entitlement gate
:feature:notepad      the multi-line document editor
:feature:scientific   scientific keypad + functions
:feature:converter    unit conversion
:feature:currency     currency conversion (network)
:feature:finance      loans, interest, discount, tax
:feature:datetime     date difference, age, working days
:feature:formula      user-defined formula builder (premium)
:pack:tax-*           regional tax packs, locale-gated
```

Dependency direction is strictly downward. `:feature:*` may depend on `:core:*`.
`:core:*` must never depend on `:feature:*`. Features must never depend on each other.

---

## 4. Build and verify

```bash
./gradlew assembleDebug              # build
./gradlew test                       # unit tests, all modules
./gradlew :core:engine:test          # engine tests only — run these constantly
./gradlew lint detekt                # static analysis
./gradlew verifyPaparazziDebug       # screenshot tests
./gradlew connectedDebugAndroidTest  # instrumented tests
```

Before declaring any task done, run at minimum `./gradlew :core:engine:test lint`.
If you touched UI, also run Paparazzi.

---

## 5. Code style

- Kotlin official style. `ktlint` via detekt enforces it.
- Compose: stateless composables, state hoisted to a `ViewModel`. No `ViewModel`
  reference inside a composable below the screen root.
- One public class per file, named after the file.
- Public API of every `:core:*` module gets KDoc. Internals do not need it.
- Prefer `sealed interface` over enums when variants carry data.
- No `!!`. No `runCatching` swallowing errors silently.
- Strings: never hardcode user-facing text. Always `stringResource`. Base locale is `en`.
- Immutable data classes. `val` unless there is a measured reason.

### Naming

- Composables: `PascalCase`, noun-first — `KeypadRow`, not `RenderKeypad`.
- ViewModels expose a single `uiState: StateFlow<XxxUiState>`.
- Test names: backtick sentences — ``fun `division by zero returns DivisionByZero`()``.

---

## 6. Testing expectations

- **Engine changes require golden tests.** Add cases to
  `core/engine/src/test/resources/golden/*.txt` (format: `input | expected`).
  Never change an existing golden line to make a test pass — that is a behaviour
  change and needs a human decision.
- **Parser must survive fuzzing.** `EngineFuzzTest` feeds random strings; it must
  never throw an uncaught exception. If your change makes it throw, the change is wrong.
- New public engine function → at least one golden case per argument arity, plus
  one error case.
- UI changes → Paparazzi snapshot in light, dark, and RTL.

---

## 7. Working agreement for agents

**Scope.** Work on one module per task. If a task seems to require editing
`:core:engine` *and* a `:feature:*`, stop and split it into two.

**Before writing code**, read the relevant doc:

| Task touches | Read first |
|---|---|
| Parser, evaluator, operators, number formats | `docs/GRAMMAR.md` + `docs/CONVENTIONS.md` |
| Module contracts, error model, storage | `docs/ARCHITECTURE.md` |
| Features, tiers, ads, pricing | `docs/SPEC.md` |
| Locale, separators, units, ISO standards | `docs/CONVENTIONS.md` |
| ViewModels, Compose, navigation, insets, permissions, performance | `docs/ANDROID_STANDARDS.md` |

**Do not decide these on your own.** If a task seems to require one, stop and ask:

- Changing operator precedence, associativity, or percent semantics
- Changing the decimal/argument separator policy
- Adding a dependency
- Changing the document storage schema
- Moving a feature between free and premium
- Adding a new ad placement
- Changing anything in §2 of this file

**Commits.** Conventional Commits, scoped by module:
`feat(engine): support unit-aware addition`, `fix(notepad): stable line references`.
One logical change per commit.

**When you finish**, state which tests you ran and their result. Do not claim a
task is complete without having run the tests.

---

## 8. Current phase

**Phase 0 — foundations.** Nothing ships yet.

Order of work:
1. `:core:engine` — lexer, Pratt parser, `Quantity`, `CalcError`, golden test harness
2. `:core:registry` — `CalculatorModule`, `EditorCommand`
3. `:core:data` — Room schema with `schemaVersion`
4. `:core:ui` — design tokens, keypad, editor with hybrid input
5. `:feature:notepad`, `:feature:scientific`, `:feature:converter`

Phase 1 ships items 1–5. Do not start Phase 2 features until Phase 1 is released.
Full roadmap in `docs/SPEC.md` §3.

---

## 9. Docs index

- `TASKS.md` — ordered work breakdown; pick up work from here
- `docs/SPEC.md` — features, phases, free vs premium, monetisation rules
- `docs/ANDROID_STANDARDS.md` — official Android guidance mapped to Lineo, plus the pre-release review checklist
- `docs/ARCHITECTURE.md` — module contracts, error model, storage, licensing policy
- `docs/CONVENTIONS.md` — locale, separators, standards, rounding
- `docs/GRAMMAR.md` — formal expression grammar and every ambiguity decision
- `docs/STORE.md` — the Play listing, the Data Safety answers and the privacy policy, each
  claim tied to what enforces it in the code
