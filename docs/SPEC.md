# SPEC.md — product specification

Status: draft, Phase 0. Changes to §4 (tiers) and §5 (ads) require a human decision.

---

## 1. Positioning

A calculator that handles anything, built on two surfaces over one engine.

**Notepad mode** is the differentiator. The user writes a free-form document; every
line evaluates live, lines define variables and reference each other:

```
price     = 250,000
discount  = price * 30%     → 75,000
total     = price - discount → 175,000
total / 3                    → 58,333.33
```

**Module mode** is the familiar surface: focused calculators with form inputs.

The rule that ties them together: **every module registers its functions into the
shared engine.** A module's form UI is a thin wrapper over a function that is also
callable as text in notepad mode. Logic is written once.

ASO angle: compete on "notepad calculator", not on "calculator".

---

## 2. Target market

Global. English is the base locale. Consequences:

- Regional tax logic (VAT, sales tax, GST, PPh, zakat) lives in `:pack:tax-*` modules,
  auto-enabled by locale, manually toggleable. Never in core.
- Metric default, imperial for US/UK/LR/MM by locale.
- Indian digit grouping (lakh/crore) is a first-class requirement, not an afterthought.
- RTL layouts must mirror; mathematical expressions must not. See `CONVENTIONS.md` §6.
- Localisation order after English: ID, ES, PT-BR, HI, DE, RU, JA.

---

## 3. Roadmap

| Phase | Contents | Ships? |
|---|---|---|
| **0** | `:core:engine`, `:core:registry`, `:core:data`, `:core:ui`. Golden tests, fuzzing. | No |
| **1** | Notepad, scientific, unit converter, history. Accessory-row input. | **First release** |
| **2** | Currency, finance, date/time. Custom keypad. Premium + ads go live. | Yes |
| **3** | Custom formula builder, graphing, matrices, statistics, themes. | Yes |
| **4** | Widgets, floating bubble, export, OCR, symbolic derivatives. | Yes |

Do not begin a phase before the previous one has shipped and has two weeks of
crash-free data.

---

## 4. Feature tiers

Principle: **free must feel complete, not crippled.** Gate power and convenience,
never basic correctness.

### Free

- Full basic and scientific calculation
- Notepad: 1 document, soft cap ~20 lines
- Unit conversion (all categories)
- History: last 50 entries
- Both light and dark theme, dynamic color
- Full accessibility support

### Premium — one-time purchase

- Notepad: unlimited documents and lines, folders, search
- Custom formula builder
- Currency conversion with auto-refresh and offline cache
- Full finance module (amortisation schedules, tax packs)
- Graphing, matrices, statistics
- Custom theme seed picker, true-black AMOLED variant
- Home screen widget, floating bubble
- CSV/PDF export, Drive backup
- No ads, no tip prompts

**Strongest purchase trigger:** the custom formula builder. A user who has built their
own working formulas will not switch apps.

### Tip jar

Play Billing consumable products at three levels, plus a "Supporter" badge.
Never link out to external payment services — Play policy violation.

Regional pricing must be configured in Play Console. One-time purchase converts far
better than subscription in price-sensitive markets.

---

## 5. Advertising rules

**Hard rule: no ads in the calculation path.** A user opens a calculator for eight
seconds. Stealing five of them ends the relationship.

### Allowed

| Placement | Format | Frequency |
|---|---|---|
| Below history list, after scroll | Native card | 1 |
| Module browser grid | Native | 1 per 8 cards |
| Settings screen | Small banner | 1 |
| User-initiated unlock | Rewarded | Unlimited, opt-in |

### Forbidden

- Interstitial after any calculation
- App Open Ads on cold start
- Any banner adjacent to the keypad or result
- Any ad inside notepad mode
- Any ad in the first 3 days after install
- Any ad in the first 5 seconds of a session

### Rewarded ads as value exchange

"Watch an ad → unlock premium themes for 24 hours" or "→ +20 notepad lines today".
This doubles as a premium demo and is the healthiest ad format for this product.

### Compliance

- Google UMP (consent) SDK is mandatory before any ad request in EEA/UK.
- Play Console Data Safety form must match actual SDK behaviour.
- Privacy policy is required and must be live before first release.

---

## 6. Release requirements

- Personal Play accounts require closed testing with 12 testers for 14 days. Plan for it.
- Privacy policy hosted (GitHub Pages is sufficient).
- Data Safety form completed.
- Keystore backed up in two separate locations. Losing it means never updating again.
- Cold start target: under 500 ms. A calculator is judged on how fast it opens.

---

## 7. Success metrics

Phase 1: crash-free sessions > 99.5%, D1 retention, % of sessions that open notepad.
Phase 2: premium conversion rate, ad ARPDAU, rewarded-ad opt-in rate.

Use these to prioritise Phase 3. Do not build the whole Phase 3 list — build what
the data says is used.

---

## 8. Quality gates

Google evaluates technical quality across all users of an app, and poor metrics can
limit store discoverability and surface a warning on the listing. Quality here is a
distribution concern, not only craft. Sources and detail in `ANDROID_STANDARDS.md` §3.

| Gate | Threshold | Checked |
|---|---|---|
| Crash-free sessions | > 99.5% | Android vitals, daily during rollout |
| User-perceived ANR rate | Below Play's bad-behaviour threshold | Android vitals |
| Cold start to interactive | < 500 ms on API 26 | Macrobenchmark in CI |
| Frame rendering | 60 fps, no dropped frames while typing | Macrobenchmark, JankStats in production |
| Base APK size | < 12 MB | CI check on every release build |
| Currency network use | One fetch per day, unmetered preferred | WorkManager constraints |

### Release process

1. Ship as an Android App Bundle, never a bare APK.
2. Staged rollout 5% → 20% → 50% → 100%, watching vitals at each step before advancing.
3. Gate every ad placement and every new feature behind Firebase Remote Config, so a bad
   change can be disabled without shipping a build. Ad placements especially — they are
   the change most likely to need an emergency rollback.
4. Adopt in-app updates from Phase 2 so users on stale versions can move forward.

### Permissions declared

`INTERNET` and `ACCESS_NETWORK_STATE` only, plus the advertising ID when ads are enabled.
Camera is added at Phase 4 and requested in context. Nothing else without a human
decision. A calculator requesting storage, location, or contacts loses user trust
instantly and invites reviewer scrutiny.
