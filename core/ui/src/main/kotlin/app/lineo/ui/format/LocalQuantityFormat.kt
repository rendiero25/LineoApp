package app.lineo.ui.format

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * How a result is written, for every surface below it.
 *
 * The display boundary of `docs/CONVENTIONS.md` §1 has three inputs — the locale, the
 * separator override and the decimal-place ceiling — and all three are settings. Passing a
 * formatter down as a parameter would put it on the signature of every screen, every row and
 * every preview, for a value none of them chooses; the shell resolves the settings once and
 * provides it here.
 *
 * The default is the platform's own locale with no override, so a preview, a snapshot or a
 * screen composed outside the shell still renders numbers rather than crashing.
 */
val LocalQuantityFormat: ProvidableCompositionLocal<QuantityFormat> =
    staticCompositionLocalOf { QuantityFormat(Locale.getDefault()) }
