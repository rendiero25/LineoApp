package app.lineo.data.settings

import app.lineo.engine.AngleMode

/**
 * Everything the user can change about how the app reads and shows numbers.
 *
 * One value object rather than four flows, so a screen cannot render a half-applied
 * combination — reading a number with one separator while the keypad renders another.
 *
 * [localeOverride] is a BCP-47 tag, or `null` to follow the system locale. It is a
 * separate choice from the device language: it selects only the *number* conventions
 * (`docs/CONVENTIONS.md` §1).
 */
data class UserSettings(
    val localeOverride: String? = null,
    val angleMode: AngleMode = AngleMode.DEG,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val separator: SeparatorPreference = SeparatorPreference.AUTO,
)
