package app.lineo.ui.input

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.registry.EditorCommand
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The shared chip row (P1-15-0).
 *
 * `AccessoryRow`'s own snapshots in `KeypadPaparazziTest` are the other half of this test and
 * the more important one: they were recorded before this row existed, and the drawing moved
 * underneath them. If they still pass unchanged, the move cost a user nothing — which is the
 * whole claim P1-15-0 makes.
 *
 * What is new here is the part `AccessoryRow` cannot show: a screen's own chips after the
 * keys, and the undocked form that sits above a keypad rather than above a keyboard.
 */
class ExpressionChipRowPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val suggestions = listOf(
        ExpressionChip(
            label = "subtotal",
            role = LineoRole.SuggestionChip,
            command = EditorCommand.InsertText("subtotal"),
            contentDescription = "insert the variable subtotal",
        ),
        ExpressionChip(
            label = "km",
            role = LineoRole.SuggestionChip,
            command = EditorCommand.InsertText("km"),
            contentDescription = "insert the unit km",
        ),
    )

    @Test
    fun `keys and a screen's own chips after them`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                ExpressionChipRow(
                    keys = accessoryKeys().take(VISIBLE_KEYS),
                    onCommand = {},
                    trailing = suggestions,
                )
            }
        }
    }

    @Test
    fun `keys and a screen's own chips in dark scheme`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = true, dynamicColor = false) {
                ExpressionChipRow(
                    keys = accessoryKeys().take(VISIBLE_KEYS),
                    onCommand = {},
                    trailing = suggestions,
                )
            }
        }
    }

    @Test
    fun `the row above a keypad does not pad for the keyboard`() {
        // `docked = false` is the form P1-15-2 puts above the scientific keypad: the keypad
        // below owns the inset, and a second padding here would stack on it.
        paparazzi.snapshot {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                Column {
                    ExpressionChipRow(keys = accessoryKeys(), onCommand = {}, docked = false)
                    Keypad(state = KeypadState())
                }
            }
        }
    }

    @Test
    fun `a comma decimal locale shows a semicolon argument separator`() {
        // `docs/CONVENTIONS.md` §2 derives one from the other, and P0-13 found the bug this
        // guards: the same glyph twice on one surface, typed wrongly half the time.
        paparazzi.snapshot {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                ExpressionChipRow(keys = accessoryKeys(decimalSeparator = ','), onCommand = {})
            }
        }
    }

    @Test
    fun `right to left`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LineoTheme(darkTheme = false, dynamicColor = false) {
                    ExpressionChipRow(
                        keys = accessoryKeys().take(VISIBLE_KEYS),
                        onCommand = {},
                        trailing = suggestions,
                    )
                }
            }
        }
    }
}

/**
 * Enough keys to prove the order, few enough that the contributed chips are on screen.
 *
 * The row scrolls, and the first attempt at these snapshots passed the full key set — which
 * pushed `subtotal` and `km` past the right edge, so the picture named after them did not
 * contain them. A snapshot that cannot show what it claims is worse than none.
 */
private const val VISIBLE_KEYS = 2
