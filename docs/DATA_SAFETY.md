# Play Console Data Safety Form — Lineo

Use these answers to complete the Data Safety section in the Google Play Console. This declaration matches the behaviour of Lineo as of Phase 1.

---

## Data Collection and Security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** (The app performs calculations locally and does not collect personal data). |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (If any diagnostic data is sent, it uses HTTPS). |
| Do you provide a way for users to request that their data is deleted? | **Yes** (Data is local; clearing app data or uninstalling deletes it). |

---

## Data Types

If you enable ads or analytics later, you may need to update this section. For Phase 1 (No ads, no external analytics):

| Data Type | Collected | Shared | Purpose |
|---|---|---|---|
| **App performance** | | | |
| Crash logs | Yes | No | Analytics / Debugging |
| Diagnostics | Yes | No | Analytics / Debugging |

---

## Data Usage and Handling

### App performance (Crash logs, Diagnostics)

- **Collected?** Yes
- **Shared?** No
- **Processed ephemerally?** No
- **Required?** No (User can opt-out via system settings if they disable usage & diagnostics)
- **Purposes:** Analytics, App functionality (debugging)

---

## Privacy Policy Link

Ensure the Privacy Policy is hosted (e.g., on GitHub Pages) and the link is provided in the Play Console.

**Link:** `https://[your-username].github.io/Lineo/privacy-policy` (Placeholder)
