# STORE.md — what Google Play is told

Everything the Play Console asks for, answered once, here, so the answers can be reviewed
against the code instead of retyped from memory into a web form.

**This file is not the source of truth for what the app does — the code is.** Every claim
below names what enforces it, so a claim that stops being true fails a build or a test rather
than quietly becoming a lie in a store listing.

Owner: P1-11. Revisit at **P2-05**, which adds ads and is the first change that makes any of
§3 false.

---

## 1. Listing

| Field | Value | Limit |
|---|---|---|
| App name | `Lineo: Notepad Calculator` | 30 — this is 25 |
| Short description | `The calculator that works like a notepad. Every line can use the last.` | 80 — this is 70 |
| Category | Tools | |
| Contact email | **TODO — a human decision.** A published listing makes it public forever | |
| Website | Optional. Omit until there is one | |
| Privacy policy URL | **TODO — §4 is the text; it needs hosting** | |

`applicationId` is `app.lineo`, and `AGENTS.md` §1 records that it is provisional *until the
first Play release*. Uploading to the internal track is what makes it permanent — it can
never be changed for this listing afterwards. **Confirm the domain is secured before the
first upload**, not after.

The launcher label stays `Lineo`. `AGENTS.md` §1 asks for the long name in the store and the
short one under the icon, and `app_name` is the short one.

### Full description

> Lineo is a calculator you write in, not one you press buttons at.
>
> Type a calculation on a line and the answer appears beside it. Type another line and it can
> use the first — reference an earlier line, give a value a name, change a number near the
> top and watch everything below it follow. It is the back of an envelope that does the
> arithmetic for you.
>
> **A document, not a display**
> Your work stays where you left it. Nothing is lost when a result scrolls away, because
> nothing scrolls away — every line is still there to read, edit, and reuse.
>
> **Numbers that behave**
> 0.1 + 0.2 is 0.3. Lineo uses decimal arithmetic throughout, so money adds up the way money
> should, without the rounding surprises that floating point brings to most calculators.
>
> **Units that carry through**
> Add 5 km to 300 m. Multiply 2 h by 60 km/h. Convert between length, mass, volume, area,
> speed, temperature, data and time — and use any of it inline, in the middle of a
> calculation, in the same line as everything else.
>
> **A scientific keypad when you want one**
> Trigonometry, logarithms, powers and roots, in degrees or radians. Every function a
> calculator screen offers can also just be typed into a line.
>
> **Reads how you write**
> Lineo follows your locale's decimal separator and digit grouping, and lets you override
> both. It works entirely offline.
>
> **Private by construction**
> Lineo requests no permissions at all. It has no internet access, no accounts, no analytics,
> and no advertising. What you type stays on your phone.

The last paragraph is a promise the manifest keeps — see §3. **It stops being true at
P2-05**, and the description has to change in the same release that makes it false.

---

## 2. Assets

| Asset | Requirement | State |
|---|---|---|
| App icon | 512×512 32-bit PNG | Source in `docs/store-assets/icon-512.svg`; **export needed** |
| Feature graphic | 1024×500 PNG or JPEG | **Not made** |
| Phone screenshots | 2–8, min 320 px, 16:9 or 9:16 | **Not taken** |
| Tablet screenshots | Optional, but the listing is penalised without them | **Not taken** |

`icon-512.svg` is not a separate drawing: it is the adaptive icon in
`app/src/main/res/drawable/` with the launcher mask already applied, so what the store shows
and what the phone shows are the same mark.

Screenshots need a device and a judgement about which four screens sell the product. They are
the part of P1-11 a human drives.

---

## 3. Data safety

**Answer: no data collected, no data shared.**

This is not a claim about intent; it is a claim about capability, and each half is enforced:

| The declaration | What enforces it |
|---|---|
| No data leaves the device | The permission list is **empty** — no `INTERNET`. `ManifestSecurityTest.asksForNoCapabilityAtAll` asserts it against the installed package, not the source manifest |
| No cleartext traffic | `network_security_config.xml`, and `ManifestSecurityTest.refusesCleartextTraffic` |
| No analytics, crash reporting, ads, or HTTP client ships | `config/licenses/allowed-dependencies.txt` is the enforced list of everything in the APK. Every entry is androidx, Kotlin, or Dagger, plus `jsr305`, `listenablefuture`, `okio`, `jakarta.inject` and `jspecify`. There is no SDK to declare on anyone's behalf |
| Expression content is never logged | `AGENTS.md` §2 — expressions hold salaries and debts. There is no analytics or crash SDK to log them to in the first place |
| Nothing goes to cloud backup | `backup_rules.xml` excludes every domain |

**Form answers:**

- *Does your app collect or share any of the required user data types?* — **No**
- *Data types collected* — none
- *Data shared with third parties* — none
- *Is data encrypted in transit?* — not applicable; there is no transit
- *Can users request data deletion?* — not applicable; nothing is collected. Data lives in
  app-private storage, and uninstalling removes it
- *Third-party SDK data collection* — none, per the allowlist above

**Device transfer is not collection.** `data_extraction_rules.xml` lets the database travel to
a new phone during setup. That transfer is device-to-device and end-to-end encrypted; Play's
Data Safety form asks about data reaching *the developer or a third party*, and this reaches
neither.

### What changes at P2-05

Ads make most of this false at once: an ad SDK collects a device identifier and shares it,
`INTERNET` gets declared, and the "no advertising" line in §1 becomes a misrepresentation.
`TASKS.md` P2-05 already carries the re-verification. It must happen in the release that adds
the SDK, not the one after.

---

## 4. Privacy policy

Play requires this hosted at a public URL, reachable without a login, and stable. The text:

> ### Lineo — Privacy Policy
>
> *Last updated: 2026-08-23*
>
> **The short version: Lineo does not collect anything.**
>
> Lineo is a calculator that runs entirely on your device. It has no user accounts, no
> servers, and no internet access — the app requests no Android permissions whatsoever,
> including the permission that would be required to send anything anywhere.
>
> **What Lineo stores, and where**
>
> Lineo saves your notepad documents, your calculation history, any formulas you define, and
> your settings. All of it is written to private storage on your own device, readable only by
> Lineo. None of it is transmitted, because the app has no means to transmit it.
>
> **Backup and transfer**
>
> Lineo excludes all of its data from Android's cloud backup, so your documents are not
> copied to any cloud service.
>
> Lineo does allow its data to be included when you transfer to a new device during setup.
> That transfer happens directly between your two devices and is end-to-end encrypted by
> Android; the data is not readable by the developer, by Google, or by anyone in between.
>
> **Analytics, crash reporting, and advertising**
>
> Lineo contains no analytics, no crash reporting, and no advertising. It includes no
> third-party software development kits that collect data.
>
> **Children**
>
> Lineo collects no personal information from anyone, including children under 13.
>
> **Deleting your data**
>
> Uninstalling Lineo removes everything it has stored. There is nothing held elsewhere to
> delete.
>
> **Changes**
>
> If a future version of Lineo adds a feature that needs the internet — currency exchange
> rates, for example — this policy will be updated before that version is released, and the
> change will be described here.
>
> **Contact**
>
> *TODO: a contact address. Play requires one, and it becomes public.*

Two TODOs, both human decisions: the contact address, and where this is hosted.

---

## 5. Release configuration

- **Signing.** Play App Signing holds the app signing key. What this repo needs is the
  *upload* key — see `keystore.properties.template`. The release build signs itself only when
  those credentials are present, so CI can still build an unsigned APK for the size check.
- **Back up the upload key in two places before the first upload.** P1-11 says two on
  purpose: one is not a backup. Replacing a lost upload key costs a support round trip.
- **Content rating.** The questionnaire's answers are all "no" — no violence, no user
  interaction, no location sharing, no purchases in Phase 1. Expected: Everyone / PEGI 3.
- **Target audience.** Not directed at children. Declaring otherwise would pull the listing
  into Families policy for no benefit.
- **Ads declaration.** "No ads" for the 1.0 listing, and it must flip at P2-05.
- **Version.** `versionCode 1`, `versionName "1.0"`, set in
  `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`.

---

## 6. What P1-11 still owes a human

1. Secure the `app.lineo` domain — the `applicationId` becomes permanent at first upload.
2. Generate the upload keystore and back it up twice.
3. Decide the public contact address.
4. Host the policy in §4 and paste the URL into the listing.
5. Export `icon-512.svg` to PNG; make the feature graphic; take the screenshots.
6. Upload to the internal testing track — the half of the DoD only the Console can answer.
