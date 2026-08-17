package app.lineo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * The single theme wrapper for every Lineo surface.
 *
 * Scheme selection follows `docs/CONVENTIONS.md` §10:
 *
 * - **Lineo's own palette is the default**, on every API level. The app is designed
 *   around it, and a default of dynamic colour would mean almost nobody ever saw it.
 * - **Dynamic colour is opt-in** and needs API 31+. When it is on, the user's contrast
 *   setting comes with it for free: Android 14+ rebuilds its colour resources when
 *   contrast changes, and because the scheme is read from the context on each
 *   composition rather than cached, Lineo follows it.
 * - **True black** replaces the dark scheme outright, including the dynamic one. Blacking
 *   out the surfaces of a dynamic palette would leave a container ladder that no longer
 *   steps evenly, so the AMOLED variant is its own generated scheme instead.
 *
 * @param darkTheme whether to use the dark scheme. Defaults to the system setting; the
 *   settings screen passes an explicit value once the user has chosen one.
 * @param dynamicColor whether to follow the wallpaper instead of Lineo's palette. Off
 *   unless the user asks for it in settings; ignored below API 31 and when [trueBlack] is on.
 * @param trueBlack the OLED variant. Premium, stubbed until Phase 3 (`docs/SPEC.md` §3).
 */
@Composable
fun LineoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    trueBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = lineoColorScheme(darkTheme, dynamicColor, trueBlack),
        typography = LineoTypography.Default,
        content = content,
    )
}

/**
 * Resolves the scheme for the current platform and preferences.
 *
 * Not remembered on purpose: reading the dynamic scheme from the context on every
 * composition is what makes an Android 14+ contrast change take effect without the
 * activity being recreated.
 */
@Composable
private fun lineoColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    trueBlack: Boolean,
): ColorScheme {
    if (trueBlack && darkTheme) {
        return LineoColorSchemes.AmoledDark
    }
    val context = LocalContext.current
    val platformSupportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        dynamicColor && platformSupportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && platformSupportsDynamic -> dynamicLightColorScheme(context)
        darkTheme -> LineoColorSchemes.Dark
        else -> LineoColorSchemes.Light
    }
}
