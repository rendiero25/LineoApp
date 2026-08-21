package app.lineo.ui.format

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * The locale numbers are *read* in, for every surface below it.
 *
 * The input half of `docs/CONVENTIONS.md` §1, and the other end of [LocalQuantityFormat]:
 * one says how a result is written, this one says how what the user typed is parsed. They
 * are provided together and resolved from the same settings, because a screen that read
 * `1,5` in `en-US` while its keypad typed a comma would report a syntax error on a key it
 * had just offered.
 *
 * Not `LocalConfiguration.current.locales[0]`: that is the *device* locale, and the
 * separator setting of §2 can override it. A screen that reached for the configuration
 * would quietly ignore the setting.
 *
 * The default is the platform's own locale, so a preview or a screen composed outside the
 * shell still reads numbers the way the device does.
 */
val LocalNumberLocale: ProvidableCompositionLocal<Locale> =
    staticCompositionLocalOf { Locale.getDefault() }
