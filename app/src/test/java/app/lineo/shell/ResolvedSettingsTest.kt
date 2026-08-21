package app.lineo.shell

import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.UnitSystem
import app.lineo.data.settings.UserSettings
import app.lineo.engine.AngleMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * The one translation from stored choices to the values screens use (P1-07).
 *
 * The asserts are mostly about the *locale*, because that is where the subtlety is: a
 * separator override has to be expressed as a reading locale, or the engine and the keypad
 * would disagree about what `1,5` means.
 */
class ResolvedSettingsTest {

    private val indonesia = Locale.forLanguageTag("id-ID")

    @Test
    fun `auto follows the system locale`() {
        val resolved = ResolvedSettings.of(UserSettings(), indonesia)

        assertEquals(indonesia, resolved.locale)
        assertEquals(',', resolved.decimalSeparator)
    }

    @Test
    fun `a locale override wins over the system`() {
        val settings = UserSettings(localeOverride = "de-DE")

        val resolved = ResolvedSettings.of(settings, Locale.US)

        assertEquals(',', resolved.decimalSeparator)
    }

    @Test
    fun `a separator override that the locale already satisfies keeps the locale`() {
        // The point of the check: an id-ID user who chooses "comma" keeps Indonesian grouping
        // rather than being moved to Germany's.
        val settings = UserSettings(separator = SeparatorPreference.COMMA)

        val resolved = ResolvedSettings.of(settings, indonesia)

        assertEquals(indonesia, resolved.locale)
        assertEquals(',', resolved.decimalSeparator)
    }

    @Test
    fun `a separator override the locale contradicts moves the reading locale`() {
        val comma = ResolvedSettings.of(UserSettings(separator = SeparatorPreference.COMMA), Locale.US)
        val dot = ResolvedSettings.of(UserSettings(separator = SeparatorPreference.DOT), indonesia)

        assertEquals(',', comma.decimalSeparator)
        assertEquals('.', dot.decimalSeparator)
    }

    @Test
    fun `the decimal places and the wallpaper choice pass straight through`() {
        val settings = UserSettings(decimalPlaces = 2, dynamicColor = true)

        val resolved = ResolvedSettings.of(settings, Locale.US)

        assertEquals(2, resolved.decimalPlaces)
        assertEquals(true, resolved.dynamicColor)
    }

    @Test
    fun `the angle mode is carried, not resolved`() {
        // No locale or platform value can change what RAD means, so it travels unchanged —
        // but it travels here, so a screen is told everything the settings decide at once.
        val settings = UserSettings(angleMode = AngleMode.RAD)

        assertEquals(AngleMode.RAD, ResolvedSettings.of(settings, indonesia).angleMode)
        assertEquals(AngleMode.DEG, ResolvedSettings.of(UserSettings(), indonesia).angleMode)
    }

    @Test
    fun `an automatic unit system follows the country, and only the country`() {
        val american = ResolvedSettings.of(UserSettings(), Locale.US)
        val indonesian = ResolvedSettings.of(UserSettings(), indonesia)

        assertEquals(UnitSystem.IMPERIAL, american.unitSystem)
        assertEquals(UnitSystem.METRIC, indonesian.unitSystem)
    }

    @Test
    fun `choosing a comma is not a statement about pounds`() {
        // The override moves the *reading* locale to Germany. The user is still American.
        val settings = UserSettings(separator = SeparatorPreference.COMMA)

        val resolved = ResolvedSettings.of(settings, Locale.US)

        assertEquals(UnitSystem.IMPERIAL, resolved.unitSystem)
    }

    @Test
    fun `a chosen unit system is not second-guessed`() {
        val settings = UserSettings(unitSystem = UnitSystem.IMPERIAL)

        assertEquals(UnitSystem.IMPERIAL, ResolvedSettings.of(settings, indonesia).unitSystem)
    }

    @Test
    fun `the default is the platform's own conventions, read when it is asked for`() {
        // What a shell composed outside the app — a preview, a snapshot — renders with.
        assertEquals(Locale.getDefault(), ResolvedSettings.default().locale)
        assertEquals(false, ResolvedSettings.default().dynamicColor)
    }
}
