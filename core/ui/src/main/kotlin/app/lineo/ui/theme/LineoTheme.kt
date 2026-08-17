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
 * - **Dynamic colour on API 31+** is the default. It costs nothing and makes the app feel
 *   native. It also carries the user's contrast setting for free: on Android 14+ the
 *   system rebuilds its colour resources when contrast changes, and because the scheme is
 *   read from the context on each composition rather than cached, Lineo follows it.
 * - **Below API 31**, or when the user turns dynamic colour off, the seeded palette in
 *   [LineoColorSchemes] is used.
 * - **True black** replaces the dark scheme outright, including the dynamic one. Blacking
 *   out the surfaces of a dynamic palette would leave a container ladder that no longer
 *   steps evenly, so the AMOLED variant is its own generated scheme instead.
 *
 * @param darkTheme whether to use the dark scheme. Defaults to the system setting; the
 *   settings screen passes an explicit value once the user has chosen one.
 * @param dynamicColor whether to use the wallpaper-derived palette where the platform
 *   offers one. Ignored below API 31 and when [trueBlack] is on.
 * @param trueBlack the OLED variant. Premium, stubbed until Phase 3 (`docs/SPEC.md` §3).
 */
@Composable
fun LineoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
