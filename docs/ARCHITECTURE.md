# ARCHITECTURE.md

Contracts and structural decisions. Changing anything here requires a human decision.

---

## 1. Module graph

```
                    :app
                      │
        ┌─────────────┼──────────────┐
   :feature:*      :pack:*      :core:billing
        │              │              │
        └──────┬───────┴──────────────┘
               │
   :core:ui  :core:data  :core:registry
               │              │
               └──────┬───────┘
                      │
                :core:engine
```

Rules:
- Dependencies point downward only.
- `:core:engine` depends on nothing but the Kotlin stdlib. It is pure logic, no Android.
- `:feature:*` modules never depend on each other. Cross-feature needs go through
  `:core:registry`.
- `:app` is the only module that knows the full list of features.

---

## 2. The engine

`:core:engine` is pure Kotlin (a JVM library, not an Android library). This is
deliberate — it makes tests fast and keeps a Compose Multiplatform port possible.

### Pipeline

```
String → Lexer → Token[] → Parser → Ast → Evaluator → Result<Quantity, CalcError>
```

- **Lexer** is locale-aware: it knows the decimal separator, argument separator,
  grouping characters, and local magnitude suffixes. It emits locale-free tokens.
- **Parser** is hand-written Pratt (precedence climbing). Chosen over a generated
  parser because we need precise source spans for error highlighting.
- **Evaluator** walks the AST with an `EvalContext` holding variables, previous line
  results, angle mode, and the function registry.

### Quantity

Every value is a quantity, not a bare number:

```kotlin
data class Quantity(
    val value: BigDecimal,
    val unit: UnitTerm? = null,   // null = dimensionless
)
```

`UnitTerm` holds base-dimension exponents (length, mass, time, current, temperature,
amount, luminosity, currency) plus a scale factor. This is what allows
`5 km + 300 m` and `2 h * 60 km/h`. Introduce it in Phase 0 even if unit arithmetic
is not exposed until Phase 2 — retrofitting it later means rewriting the evaluator.

Currency is modelled as a dimension with a runtime-variable scale factor, sourced
from the rate cache.

### Precision

- Internal: `MathContext.DECIMAL128`.
- Display: rounded per `CONVENTIONS.md` §4, with full precision available on tap.
- Never `Double` for user-visible arithmetic. `Double` is acceptable only inside
  transcendental function implementations, with the result re-boxed to `BigDecimal`.

---

## 3. Error model

The engine never throws on bad user input.

```kotlin
sealed interface CalcError {
    val span: IntRange?

    data class Syntax(val token: String, override val span: IntRange) : CalcError
    data class UnbalancedParen(val missing: Int, override val span: IntRange?) : CalcError
    data class UnknownIdentifier(
        val name: String,
        val suggestion: String?,
        override val span: IntRange,
    ) : CalcError
    data class UnitMismatch(
        val left: UnitTerm, val right: UnitTerm, override val span: IntRange,
    ) : CalcError
    data class DomainError(
        val fn: String, val reason: DomainReason, override val span: IntRange,
    ) : CalcError
    data class CircularReference(val chain: List<LineId>) : CalcError
    data object DivisionByZero : CalcError { override val span: IntRange? get() = null }
    data class Overflow(override val span: IntRange?) : CalcError
    data class RateUnavailable(val from: String, val to: String) : CalcError
}
```

### Presentation rules

1. **Incomplete input is not an error.** While the user is typing `5 +`, show nothing.
   Validate only when the expression settles — 400 ms debounce, or on `=`.
2. **Inline, never modal.** Red underline at `span`, short message below the line.
   No toasts, no dialogs. `UnknownIdentifier` with a suggestion renders as a
   tap-to-fix chip.
3. **Per-line isolation in notepad.** Build a dependency graph. An error on line 3
   fails only the lines that depend on it. Dependent lines show a muted
   "depends on line 3", not red — it is not their fault.
4. **Infrastructure degrades, never fails.** Currency offline serves the cached rate
   with a visible "rate from 10 Aug" badge. Never an empty state, never a raw error.

---

## 4. Module registry

> Platform conventions for the UI and data layers — UDF, ViewModel rules, state
> collection, naming — live in `ANDROID_STANDARDS.md` §1. They are binding.


```kotlin
enum class Tier { FREE, PREMIUM }

interface CalculatorModule {
    val id: String
    val titleRes: Int
    val iconRes: Int
    val tier: Tier
    val locales: LocaleGate                      // ALL, or a specific set
    fun functions(): List<CalcFunction>          // injected into the engine
    fun units(): List<UnitDefinition> = emptyList()
    @Composable fun Screen(nav: ModuleNav)
}

data class CalcFunction(
    val name: String,
    val aliases: List<String> = emptyList(),
    val arity: IntRange,
    val signature: List<ParamSpec>,
    val evaluate: (args: List<Quantity>, ctx: EvalContext) -> Result<Quantity, CalcError>,
)
```

Modules are contributed via Hilt multibinding into a `Set<CalculatorModule>`.
`:app` binds them; nothing else knows the full set.

**A module's `Screen` must not contain calculation logic.** It collects inputs and
calls its own registered `CalcFunction`. If logic exists only in the Screen, notepad
mode cannot reach it, and the design constraint is broken.

---

## 5. Input abstraction

The editor never receives raw key events. Every input surface emits commands:

```kotlin
sealed interface EditorCommand {
    data class InsertText(val text: String) : EditorCommand
    data class InsertFunction(val name: String, val arity: Int) : EditorCommand
    data class WrapSelection(val open: String, val close: String) : EditorCommand
    data class MoveCursor(val delta: Int) : EditorCommand
    data object Backspace : EditorCommand
    data object NewLine : EditorCommand
    data object ToggleTextInput : EditorCommand
}

interface InputSurface {
    val commands: Flow<EditorCommand>
}
```

Implementations: accessory row, custom keypad, system keyboard adapter, hardware
keyboard, suggestion chips. The editor is agnostic.

This is what makes the Phase 1 → Phase 2 input change additive rather than a rewrite.

### Hybrid input behaviour (decided)

- Calculator keypad is the default surface.
- An `Aa` button raises the system keyboard, replacing the keypad.
- **Context auto-switch:** a new line starting with a digit → keypad; starting with a
  letter → system keyboard. The user should rarely need to press `Aa` consciously.
- A suggestion chip row sits above either surface: known variables, recent units.
- Tablet/foldable/hardware keyboard: keypad becomes a persistent side panel.

---

## 6. Storage

Room, with a schema version pinned per entity and exported schemas committed to the repo.

```
documents(id, title, schemaVersion, createdAt, updatedAt, sortIndex)
lines(id, documentId, ordinal, source, label)      -- id is stable, ordinal is display
history(id, expression, resultText, createdAt, moduleId)
formulas(id, name, source, params, schemaVersion)  -- premium
rate_cache(base, quote, rate, fetchedAt, source)
```

**Line references bind to `lines.id`, never to `ordinal`.** Inserting a line must not
silently repoint an existing reference. `ordinal` exists only for display and ordering.

Evaluation is incremental: a change to line N re-evaluates only N and its transitive
dependents. A 200-line document must not fully recompute on each keystroke.

Migrations are numbered and tested with `MigrationTestHelper`. A failed migration
enters recovery mode; it never crashes.

---

## 7. Dependency and licensing policy

Every **shipped** dependency must be **Apache-2.0, MIT, or BSD**. No exceptions without a
recorded human decision.

### Shipped versus test-only

The rule exists because copyleft obligations attach to *distribution*. A library that only
ever runs on a developer's machine or in CI is never distributed, so those obligations
never trigger. The two are therefore held to different standards, and the build enforces
the difference rather than leaving it to a reviewer's judgement:

| | Reaches the APK | Rule | Recorded in |
|---|---|---|---|
| Shipped | yes | Apache-2.0, MIT, BSD only. The recorded licence is checked, not just the presence of the entry | `config/licenses/allowed-dependencies.txt` |
| Test-only | no | Any licence, copyleft included | `config/licenses/allowed-test-dependencies.txt` |

A dependency counts as shipped when it is reachable from a runtime classpath that is not a
unit-test, instrumented-test, screenshot-test, or test-fixture one. `checkDependencyLicenses`
computes both sets from the resolved graph, so the classification cannot drift from reality.

Both lists still require a human decision to grow — that part of §7 is unchanged. What
changed is that a copyleft test transitive no longer looks identical to a copyleft library
that ships. Before the split the single list held fifteen EPL and LGPL rows, every one of
them harmless, and each new one asked for the same approval as a shipped library. A rule
that raises fifteen false alarms stops being read by the time the real one arrives.

### Approved

Kotlin, Coroutines, kotlinx.serialization, Jetpack Compose, Material 3, Navigation,
Hilt, Room, DataStore, WorkManager, Glance, Ktor Client or Retrofit/OkHttp,
Hipparchus (numerics), EJML (matrices), Vico (charts), JUnit5, Turbine, Robolectric,
Paparazzi, Play Billing, Google Mobile Ads, UMP, Firebase Crashlytics/Analytics.

### Rejected, with reasons

| Library | Reason |
|---|---|
| mXparser | Dual-licensed; commercial use requires a paid licence |
| Symja / matheclipse | GPL-3.0 with LGPL parts and GPL transitive deps |
| JAS, Jasymca | GPL family |
| JEP | Commercial licence |
| Chaquopy + SymPy | APK size and cold-start cost break the 500 ms target |
| RevenueCat | Unnecessary for a single-platform one-time purchase |

### Replacing the rejected CAS

| Need | Solution |
|---|---|
| Derivative at a point | Hipparchus `Gradient`, or dual numbers in-house |
| Definite integral | Hipparchus adaptive quadrature |
| Symbolic derivative | In-house AST rewrite rules (~250 lines) + simplifier |
| Polynomial roots | Analytic for degree ≤ 3, Hipparchus `LaguerreSolver` above |
| Symbolic integral | Deferred to Phase 4: rule table + pattern matching |

The in-house symbolic differentiator is a feature advantage, not just a licence
workaround — it lets us show derivation steps, which no library provides.

---

## 8. Privacy and telemetry

- Crashlytics logs `CalcError` **type and span only**. Never the expression text.
- Analytics logs feature usage events. Never content.
- Currency requests carry no user identifier.
- No backend. If one is ever proposed, it needs a human decision — it changes the
  cost model permanently.

---

## 9. Performance targets

| Metric | Target |
|---|---|
| Cold start to interactive | < 500 ms |
| Keystroke to result, single line | < 16 ms |
| Keystroke to result, 200-line document | < 50 ms (incremental) |
| APK size (base, no dynamic features) | < 12 MB |
| Crash-free sessions | > 99.5% |

---

## 10. Platform targets and build configuration

| Setting | Value | Locked? |
|---|---|---|
| `minSdk` | **26** (Android 8.0) | Yes — human decision required to change |
| `targetSdk` | Latest stable | Follows Play requirements |
| `compileSdk` | Latest stable | Free to bump |
| JVM target | 17 | |
| Language | Kotlin, official style | |

### Why minSdk 26

Not an arbitrary round number. Three reasons specific to this product:

1. **`java.time` is native from API 26.** The date/time module depends on it heavily.
   Below 26 we would need core library desugaring — slower builds, divergent edge-case
   behaviour, and an extra layer that can fail, for no proportional gain.
2. **`BigDecimal` and `MathContext` behave consistently from API 26 up.** Precision is
   this app's reputation; we do not want to argue with old-device quirks.
3. Adaptive icons require 26, and `WindowInsets` — load-bearing for the hybrid input
   surface — is far cleaner above it.

Devices below API 26 are under 1% of active Android devices and are not the segment
that buys premium.

**Raising `minSdk` later is allowed and easy. Lowering it is not** — it strands
existing users on a version that can no longer receive updates. Treat 26 as a floor,
not a default.

### API level guarding

A high `compileSdk` does not require users to run that version. Newer APIs are called
behind explicit version checks, with a fallback for the 8.0–11 range:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    dynamicLightColorScheme(context)   // Android 12+
} else {
    LineoLightScheme                   // generated seed palette
}
```

Lint must fail the build on an unguarded call above API 26. Never suppress that
warning — add the fallback.

### Module types

- `:core:engine` is a **JVM library**, not an Android library. No `android.*` imports,
  ever. This keeps its tests in the millisecond range and keeps a Compose Multiplatform
  port open.
- Everything else is an Android library. `:app` is the only application module.

### Convention plugins

Module build files must stay under ~5 lines. Shared configuration lives in
`build-logic`:

`lineo.android.application`, `lineo.android.library`, `lineo.android.compose`,
`lineo.jvm.library`

All dependency versions live in `gradle/libs.versions.toml`. No inline version
strings anywhere in the repo.

### Testing on the floor

CI and manual QA must include an API 26 device or emulator. The 500 ms cold-start and
50 ms incremental-evaluation targets are measured there, not on a flagship.
