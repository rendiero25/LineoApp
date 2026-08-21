package app.lineo.ui.input

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The character the decimal key types, for every surface below it.
 *
 * A composition local rather than a parameter on each screen, for the same reason
 * `LocalWindowWidthClass` is one: the separator is a property of the *window* the user is
 * looking at, every input surface needs it, and threading it through four screens would mean
 * a new parameter on each one for a value none of them decides.
 *
 * Where it comes from is `docs/CONVENTIONS.md` §1 and §2: the locale by default, overridden
 * by the setting P1-07 owns. The shell provides it once; a keypad, an accessory row or a line
 * editor that is given nothing reads it from here, and P1-07's promise — the keypad follows
 * the setting immediately — is then a recomposition rather than a rebuild.
 *
 * Static, because it changes about once a year per user: a settings change re-composes
 * everything below the provider, which is exactly what should happen and is cheaper than
 * tracking reads.
 */
val LocalDecimalSeparator: ProvidableCompositionLocal<Char> = staticCompositionLocalOf { '.' }
