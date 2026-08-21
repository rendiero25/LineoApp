package app.lineo.ui.editor

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import app.lineo.engine.AngleMode

/**
 * What `sin(30)` means, for every surface below it.
 *
 * A setting rather than a screen's choice: the notepad and a module screen must agree, or
 * the same expression would answer `0.5` on one and `-0.988` on the other — and `AGENTS.md`
 * §1 makes that agreement the point of having one engine.
 *
 * Provided by the shell from the user's settings, next to the reading locale. A screen that
 * built an `EvalContext` without it would evaluate in degrees whatever the user chose.
 *
 * The default is [AngleMode.DEG], which is what `UserSettings` starts on, so a preview or a
 * screen composed outside the shell behaves as a fresh install does.
 */
val LocalAngleMode: ProvidableCompositionLocal<AngleMode> = staticCompositionLocalOf { AngleMode.DEG }
