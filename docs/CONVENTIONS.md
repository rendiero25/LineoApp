# CONVENTIONS.md

Standards and formatting decisions. These exist so that every session — human or
agent — makes the same choice. Changing any of them requires a human decision,
because they affect stored user documents.

Policy: **follow existing standards (CLDR/ICU, ISO). Never invent our own.**

---

## 1. The three-layer rule

```
INPUT (locale-aware)  →  INTERNAL (canonical)  →  DISPLAY (locale-aware)
   "1.234,56"              BigDecimal("1234.56")     "1.234,56" or "1,234.56"
```

**Internal is always a dot and always `BigDecimal`. Never store formatted strings
in the database.** Every separator bug ever filed comes from breaking this rule.

---

## 2. Decimal and argument separators

If the decimal separator is a comma, `max(1,5)` is ambiguous. The standard resolution
(same as spreadsheets and ISO practice) is to switch the argument separator:

| Locale group | Decimal | Grouping | Argument separator |
|---|---|---|---|
| en-US, en-GB, ja, zh, th, he | `.` | `,` | `,` → `max(1, 5)` |
| id, de, es, pt, ru, fr, it, tr, nl, vi | `,` | `.` or space | `;` → `max(1; 5)` |

Rules:

- Read symbols from `DecimalFormatSymbols` for the active locale. Never hardcode.
- The keypad renders whichever separator is active.
- **When parsing, ignore grouping separators entirely.** Do not attempt to infer
  whether `1,234` means one thousand or one point two. Grouping is inserted at
  display time only.
- Settings offers an override: `Auto (system) / Dot / Comma`. Many users have a phone
  locale that differs from how they write numbers.

---

## 3. Digit grouping

| Style | Example | Locales |
|---|---|---|
| Western | 1,234,567 | Most |
| Indian | 12,34,567 | hi, bn, ta, te, mr, gu, kn, ml, pa |
| Chinese 万 | optional display mode | zh, ja |

Indian grouping is a requirement, not an enhancement. India is the largest Android
market. Use ICU `NumberFormat` rather than manual insertion.

Eastern Arabic (٠١٢٣) and Devanagari digits are offered as a display option.

---

## 4. Number semantics

Locked decisions. Golden tests enforce every row.

| Expression | Result | Rationale |
|---|---|---|
| `-2^2` | `-4` | Unary minus binds looser than exponentiation |
| `2^3^2` | `512` | Exponentiation is right-associative |
| `6/2(1+3)` | `12` | Implicit multiplication has the same precedence as explicit |
| `(1+2)(3+4)` | `21` | Implicit multiplication between parentheses is allowed |
| `100 + 10%` | `110` | Percent is contextual in additive position |
| `100 - 10%` | `90` | Same |
| `100 * 10%` | `10` | In multiplicative position, `%` means `/100` |
| `50% of 80` | `40` | `of` is a keyword operator |
| `1/3` display | `0.333333333` + truncation indicator | Full precision on tap |
| `0.1 + 0.2` | `0.3` | `BigDecimal`, never `Double` |

Rounding for display: `RoundingMode.HALF_UP`. Not banker's rounding — it contradicts
lay expectation, and this is a consumer app.

Angle mode: **DEG by default**, persisted, and the indicator is always visible.
Silent angle mode is the single most common source of wrong calculator answers.

---

## 5. Standards to follow

| Domain | Standard | Example |
|---|---|---|
| Currency codes | ISO 4217 | `USD`, `IDR`, `EUR` |
| Dates (internal) | ISO 8601 | `2026-08-12` |
| Dates (display) | CLDR per locale | `12/08/2026` or `08/12/2026` |
| Unit symbols | ISO 80000 / SI | `kg` not `Kg`, `s` not `sec` |
| Binary prefixes | IEC 80000-13 | `KiB` = 1024, `kB` = 1000 |
| Language tags | BCP 47 | `pt-BR` |
| Time zones | IANA tzdb | `Asia/Jakarta` |

Week start, weekend days, and public holidays are locale-derived, never hardcoded.
Saturday–Sunday is not universal.

---

## 6. RTL

- Layout mirrors for Arabic, Hebrew, Persian, Urdu.
- **Mathematical expressions stay LTR.** Force the editor's text direction on
  expression content; do not let bidi reordering apply to operators and operands.
- Test every screen with `android:layoutDirection="rtl"` and pseudo-locale `ar-XB`.
- Paparazzi snapshots include an RTL variant.

---

## 7. Magnitude suffixes

Locale-gated, resolved by the lexer, and always ambiguous with variables — see
`GRAMMAR.md` §3 for the resolution rule.

| Locale | Suffixes |
|---|---|
| en | `k`, `m`, `b`, `t` |
| id | `rb`, `jt`, `m`, `t` |
| hi | `k`, `lakh`, `cr` |
| de, es, pt | `mil`, `mio` |

A suffix is only recognised when it immediately follows a numeric literal with no
space, and no user variable of that name is in scope.

---

## 8. Accessibility

- TalkBack reads expressions semantically: `2^3` → "two to the power of three",
  not "two caret three". Provide `contentDescription` from the AST, not the raw string.
- Support Android 14+ contrast levels (medium, high).
- Minimum touch target 48 dp. Keypad keys should exceed this comfortably.
- Never encode meaning in colour alone — errors carry an icon and text, not just red.
- Tabular figures in the display font so digits do not shift while typing.

---

## 9. Compose and UI conventions

Derived from Android's architecture and design guidance. Full rationale and sources in
`ANDROID_STANDARDS.md` §1–2.

### State

- ViewModels expose one `uiState: StateFlow<XxxUiState>`. Build it with `stateIn(...,
  SharingStarted.WhileSubscribed(5_000), initial)` when it derives from a stream.
- Collect with `collectAsStateWithLifecycle()`. Never `collectAsState()`.
- No one-shot event channels from ViewModel to UI. An error, a snackbar, a navigation
  request — all are fields on `uiState`, cleared by an explicit UI callback.
- Reusable components (`Keypad`, `ExpressionEditor`, `SuggestionChips`) use plain state
  holder classes with hoisted state, never their own ViewModel.

### Composables

- Stateless by default; state hoisted to the screen root.
- Named as nouns in `PascalCase` — `KeypadRow`, not `RenderKeypad`.
- No `Context` lookups deep in the tree; pass what is needed as parameters.
- Every screen has a Paparazzi snapshot in light, dark, RTL, and maximum font scale.

### Layout

- Branch on `WindowSizeClass` (compact / medium / expanded), never on device type,
  smallest-width qualifiers, or raw pixel width.
- Size class can change at runtime on a foldable without activity recreation. Layout is
  driven by state, not by configuration callbacks.
- Do not lock orientation to simplify a screen.
- Edge-to-edge: draw behind system bars, pad content from `WindowInsets`. The keypad
  consumes `WindowInsets.ime` and `WindowInsets.navigationBars` without double-padding.

### Naming

Adopted from Android's architecture recommendations:

| Kind | Rule | Example |
|---|---|---|
| Method | verb phrase | `evaluateLine()` |
| Property | noun phrase | `activeAngleMode` |
| Flow-returning function | `get{Model}Stream` | `getDocumentStream(id)` |
| List variant | plural model | `getDocumentsStream()` |
| Implementation | meaningful, `Default` as last resort | `OfflineFirstRateRepository` |
| Test double | `Fake` prefix | `FakeRateRepository` |
