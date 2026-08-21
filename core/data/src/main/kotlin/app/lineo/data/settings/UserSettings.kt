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
 *
 * [dynamicColor] is off by default and deliberately so: Lineo's palette is the product's,
 * and wallpaper colours are the opt-in (`docs/CONVENTIONS.md` §10).
 */
data class UserSettings(
    val localeOverride: String? = null,
    val angleMode: AngleMode = AngleMode.DEG,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val separator: SeparatorPreference = SeparatorPreference.AUTO,
    val dynamicColor: Boolean = false,
    val unitSystem: UnitSystem = UnitSystem.AUTO,
    val decimalPlaces: Int = DEFAULT_DECIMAL_PLACES,
)

/**
 * How many decimal places a result may show before it is cut and marked with an ellipsis.
 *
 * Nine, which is what `docs/CONVENTIONS.md` §4 spells out and about as many digits as anyone
 * reads without counting. It is a **ceiling**, not a fixed width: `0.5` stays `0.5` rather
 * than becoming `0.500000000`, because a calculator that padded every answer would be lying
 * about how precise it was.
 */
const val DEFAULT_DECIMAL_PLACES: Int = 9

/** What the user may choose from. Zero is a real choice: whole numbers only. */
val DECIMAL_PLACES_RANGE: IntRange = 0..9
