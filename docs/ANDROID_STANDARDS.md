# ANDROID_STANDARDS.md

Official Android guidance, mapped to Lineo decisions.

This file exists so agents follow platform conventions instead of inventing them.
When Google's guidance and this repo disagree, the repo wins only where a deviation
is recorded below with a reason. Everything else: follow the platform.

Sources are linked per section. Re-check them when bumping major library versions —
Android guidance moves, and stale conventions are worse than none.

---

## 1. Architecture

Source: [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations),
[Guide to app architecture](https://developer.android.com/topic/architecture),
[Modularization patterns](https://developer.android.com/topic/modularization/patterns)

Google grades its advice as strongly recommended / recommended / optional. Everything
in this table is **strongly recommended** unless noted, so treat it as binding here.

| Platform guidance | What it means in Lineo |
|---|---|
| Separate data layer and UI layer; expose data through repositories | `:core:data` exposes `DocumentRepository`, `HistoryRepository`, `RateRepository`. Create the repository even when it wraps a single DAO |
| ViewModels and composables never touch a data source directly | No Room DAO, DataStore, or Retrofit service injected into a ViewModel or composable. Ever |
| Communicate between layers with coroutines and Flow | Suspend functions for actions, `Flow` for streams |
| Unidirectional data flow | ViewModel exposes state; UI sends actions as method calls. Never the reverse |
| Collect state lifecycle-aware | `collectAsStateWithLifecycle()`, not `collectAsState()` |
| **Do not send events from ViewModel to UI** | No `Channel` / `SharedFlow` of one-shot events. Handle the event in the ViewModel and reflect the outcome in state. An error is a field on `uiState`, not a fired event |
| Single-activity app | One `MainActivity`. Screens are navigation destinations |
| ViewModel holds no lifecycle references | No `Context`, `Activity`, `Resources`, or `Application` in a ViewModel. Needing one means the logic is in the wrong layer. Do not use `AndroidViewModel` |
| ViewModels at screen level only | `NotepadViewModel` yes. A ViewModel inside `Keypad` or `ExpressionEditor` no — those get plain state holder classes with hoisted state |
| Expose a single `uiState: StateFlow` | `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` when fed by a stream; `MutableStateFlow` exposed as immutable otherwise |
| Constructor injection, Hilt for non-trivial apps | Lineo qualifies: many screens, WorkManager, multibound modules |
| Do not override activity lifecycle callbacks for UI work | Use `LifecycleStartEffect`, `LifecycleResumeEffect`, `repeatOnLifecycle` |
| Domain layer when logic is shared across ViewModels *(recommended, not strong)* | Add use cases only when a second consumer appears. Do not pre-build a `:domain` module |

### Naming conventions

From the same source, adopted verbatim:

- Methods are verb phrases — `evaluateLine()`, not `lineEvaluation()`
- Properties are noun phrases — `activeAngleMode`
- Flow-returning functions end in `Stream` — `getDocumentStream(id): Flow<Document>`,
  plural for lists — `getDocumentsStream()`
- Interface implementations get meaningful names — `RoomDocumentRepository`,
  `OfflineFirstRateRepository`. Use `Default` only when nothing better fits
- Test doubles are prefixed `Fake` — `FakeRateRepository`

### Open decision — Navigation

Google now points multi-screen apps at [Navigation 3](https://developer.android.com/guide/navigation/navigation-3).
`ARCHITECTURE.md` currently assumes Navigation Compose (type-safe). **This is an open
decision** — do not pick one silently. Whoever resolves it records the outcome in the
decisions log in `TASKS.md` and updates `ARCHITECTURE.md` §1.

---

## 2. UI and UX

Source: [Design for mobile](https://developer.android.com/design/ui/mobile),
[Material Design 3](https://m3.material.io),
[Window size classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/window-size-classes),
[Accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)

### Adaptive layout

Do not branch on device type or raw pixel width. Branch on **window size class**:

| Class | Lineo layout |
|---|---|
| Compact | Single pane. Keypad or system keyboard docked at the bottom |
| Medium | Notepad with a persistent keypad panel; wider result column |
| Expanded | Two pane — document left, keypad and module output right |

The window size class can change at runtime on a foldable without the activity being
recreated. Layout must be driven by state, not by configuration callbacks.

Do not rely on orientation locking to simplify layout. Design every screen as
resizable, and verify in split-screen and desktop windowing.

### Edge-to-edge and insets

Edge-to-edge is the platform default expectation. This matters more for Lineo than for
most apps because the input surface lives against the bottom of the screen.

- Draw behind system bars; apply `WindowInsets` padding to content
- The keypad and accessory row must consume `WindowInsets.ime` and
  `WindowInsets.navigationBars`, and must not double-pad when both apply
- Verify with gesture navigation and three-button navigation, on a device with a
  display cutout, and on API 26 where inset behaviour differs

### Predictive back

Source: [Predictive back](https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back)

Opt in and support the animation. Specific to Lineo: **back in the notepad must never
discard user work.** If a document has unsaved state, back commits it rather than
prompting. A calculator that loses a computation on a stray back gesture is unusable.

### Theming

- Material 3 with dynamic color on API 31+, generated seed palette below. Full colour
  strategy, token mapping, typography, and motion rules are in `CONVENTIONS.md` §10
- Build the fallback palette with the [Material Theme Builder](https://m3.material.io/theme-builder)
  rather than hand-picking hex values
- The [Android UI kit](https://goo.gle/android-ui-kit) provides Figma components that
  match the real Material 3 specs — use it instead of redrawing components

### Accessibility

Google's framing is that accessible design benefits roughly 15% of the world's
population. For a global-market app that is a market-size argument, not just an ethical
one. Requirements already in `CONVENTIONS.md` §8 stand; add:

- Every interactive element has a content description or is explicitly decorative
- Touch targets at least 48 dp
- Support system font scaling — the display and keypad must not clip at large font
  sizes. Test at maximum scale
- Support Android 14+ contrast levels

---

## 3. Quality bar

Source: [Technical quality](https://developer.android.com/quality/technical),
[Android vitals](https://developer.android.com/topic/performance/vitals)

Google's framing to internalise: **poor vitals reduce your store discoverability**, and
a warning can appear on the listing on affected devices. Quality is not only craft here;
it is distribution.

| Area | Guidance | Lineo target |
|---|---|---|
| Stability | Minimise crashes, ANRs, and low-memory kills; monitor per device | Crash-free sessions > 99.5% |
| Startup | Minimise launch to first interaction; ship a Baseline Profile; call `reportFullyDrawn()` | Cold start < 500 ms on API 26 |
| Rendering | Most apps should hold 60 fps with no dropped frames; jank is user-visible | No dropped frames while typing |
| App size | Smaller installs reach more users and are uninstalled less | Base APK < 12 MB; ship as App Bundle |
| Battery and network | Use limited resources deliberately | One currency fetch per day via WorkManager, on unmetered where possible |
| Freshness | Regular updates; keep update size small | Adopt in-app updates in Phase 2 |
| Healthy releases | Prevent issues reaching production rather than fixing after; phase the rollout | 5% → 20% → 50% → 100%, watch vitals at each step |

Kotlin's null safety is itself listed as a stability practice. Reinforces the existing
ban on `!!`.

Decouple binary releases from feature releases with Firebase Remote Config, so a bad
feature can be turned off without shipping a build. Applies especially to ad placements.

---

## 4. Performance practice

Source: [App performance guide](https://developer.android.com/topic/performance/overview),
[Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview),
[R8 optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization),
[ANRs](https://developer.android.com/topic/performance/anrs/keep-your-app-responsive),
[SQLite best practices](https://developer.android.com/topic/performance/sqlite-performance-best-practices)

- **Baseline Profile plus Startup Profile.** Generate from a Macrobenchmark journey that
  covers cold start and typing a document
- **R8 in full mode**, with keep rules kept minimal and reviewed. Test the optimised
  build — R8 problems only appear in release
- **Macrobenchmark in CI** for startup and scroll; Microbenchmark for the engine's hot
  path. Engine benchmarks are cheap because `:core:engine` is a JVM module
- **ANR discipline.** Parsing and evaluation never run on the main thread for large
  documents. The 1-second fuzz timeout in `GRAMMAR.md` §6 exists partly for this
- **Room/SQLite.** Index `lines.documentId`, avoid reading whole documents per keystroke,
  use transactions for bulk updates, and never do a synchronous DB read on the main thread
- **JankStats** to find real-device jank once there are users

---

## 5. Security and privacy

Source: [Design for safety](https://developer.android.com/quality/privacy-and-security),
[App security best practices](https://developer.android.com/topic/security/best-practices),
[Privacy best practices](https://developer.android.com/privacy/best-practices),
[SDK best practices](https://developer.android.com/guide/practices/sdk-best-practices)

The platform principle is **minimisation** — minimise permissions, minimise data,
minimise visibility. Lineo is in a strong position: it is a calculator, and needs
almost nothing.

### Permissions

| Permission | Needed? |
|---|---|
| `INTERNET` | Yes — currency rates and ads only |
| `ACCESS_NETWORK_STATE` | Yes — offline detection for the rate cache |
| AdMob advertising ID | Only if ads are enabled. Declare it honestly |
| Storage | **No.** Use app-specific storage; use the Storage Access Framework for export, which needs no permission |
| Camera | Only at Phase 4 OCR, requested in context at the moment of use, never at startup |
| Location, contacts, phone, anything else | **Never.** A calculator asking for these is a red flag to users and reviewers |

If a task seems to need a new permission, that is a decision requiring a human — see
`AGENTS.md` §7.

### Data handling

Notepad documents are the sensitive surface. People compute salaries, debts, rent, and
medical costs in a calculator. Treat document content as private data:

- Never send expression content to analytics or crash reporting. Already a
  non-negotiable in `AGENTS.md` §2 — this section is its justification
- Consider encrypting the Room database at rest, since a stolen device otherwise exposes
  the notepad. Evaluate before Phase 3, when documents become long-lived
- Exclude app data from cloud auto-backup unless the user opts into backup explicitly
- Use no non-resettable device identifiers
- Declare data collection accurately in the Play Console Data Safety form. It must match
  what the ad SDK actually does, not what you intend

### Network

- HTTPS only. No cleartext traffic — set a network security config that forbids it
- Validate the rate API response defensively; a malformed or hostile payload must produce
  `RateUnavailable`, never a crash and never a wrong exchange rate silently applied

### Third-party SDKs

The ad SDK is the largest external attack and privacy surface in this app. Check it in
the [Play SDK Index](https://play.google.com/sdks) before adoption, keep it updated, and
re-verify the Data Safety declaration whenever it is upgraded.

Play Integrity is available for verifying premium entitlement server-side. Not needed at
Phase 2 with a purely local one-time purchase, but note it if entitlement is ever
challenged by piracy.

---

## 6. Large screens and foldables

Source: [Phones, tablets, foldables](https://developer.android.com/phones-tablets-foldables),
[Large screens gallery](https://developer.android.com/large-screens/gallery)

- Layout from window size classes, never device type
- Support continuity across form factors: state survives fold, unfold, rotation, and
  window resize. `rememberSaveable` for editor state, and the document is persisted
  continuously rather than on exit
- Support hardware keyboards on tablets and ChromeOS — full expression entry, arrow key
  navigation between lines, and `Enter` for a new line. This is nearly free once
  `EditorCommand` exists, and it is a genuine differentiator for a notepad calculator
- Test on a foldable emulator with a fold posture, in split-screen, and in desktop
  windowing

---

## 7. Review checklist

Before any release, verify against these. Each maps to a section above.

- [ ] No ViewModel holds a `Context`; no composable touches a data source
- [ ] All state collected with `collectAsStateWithLifecycle`
- [ ] No one-shot event channels from ViewModel to UI
- [ ] Layout driven by window size class; verified compact, medium, expanded
- [ ] Edge-to-edge correct with gesture and three-button navigation, and with the IME open
- [ ] Predictive back supported; back never loses user work
- [ ] Font scaling at maximum causes no clipping
- [ ] TalkBack completes the full flow; all targets ≥ 48 dp
- [ ] Baseline Profile present; Macrobenchmark startup within target
- [ ] R8 full mode enabled; release build tested, not just debug
- [ ] Permission list is exactly the minimum above
- [ ] Data Safety form matches actual SDK behaviour
- [ ] No cleartext traffic; hostile rate payload handled
- [ ] Phased rollout configured; vitals watched at each step
