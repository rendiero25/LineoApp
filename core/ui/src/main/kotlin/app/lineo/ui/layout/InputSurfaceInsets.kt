package app.lineo.ui.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.max

/**
 * The bottom inset a surface docked to the bottom of the window has to clear.
 *
 * `docs/ANDROID_STANDARDS.md` §2: the keypad and the accessory row must consume both
 * `ime` and `navigationBars` **without double padding**. The two overlap — when the
 * keyboard is up it is drawn over the navigation bar, so its inset already contains the
 * bar's. Adding them lifts the keypad by roughly a navigation bar too far and leaves a
 * band of empty surface under the keyboard.
 *
 * The rule is therefore the larger of the two, never the sum. This is what
 * [combineDockedBottomInset] exists to state, and what its test exists to defend: the
 * function is one line, and the one line is the whole requirement.
 */
internal fun combineDockedBottomInset(imeBottom: Int, navigationBarBottom: Int): Int =
    max(imeBottom, navigationBarBottom)

/**
 * The bottom padding for a surface docked at the bottom of the window, in dp.
 *
 * Recomposes as the keyboard animates, because [WindowInsets] are snapshot state.
 */
@Composable
fun dockedBottomInset(): Dp {
    val density = LocalDensity.current
    val ime = WindowInsets.ime.getBottom(density)
    val navigationBars = WindowInsets.navigationBars.getBottom(density)
    return with(density) { combineDockedBottomInset(ime, navigationBars).toDp() }
}

/**
 * Pads a bottom-docked input surface clear of the keyboard and the navigation bar.
 *
 * Apply to the keypad, the accessory row, or anything else that sits against the bottom
 * edge under edge-to-edge. Do not combine it with `navigationBarsPadding()` or
 * `imePadding()` — this modifier already accounts for both, and stacking them is exactly
 * the double padding it exists to prevent.
 */
@Composable
fun Modifier.dockedBottomPadding(): Modifier = padding(bottom = dockedBottomInset())
