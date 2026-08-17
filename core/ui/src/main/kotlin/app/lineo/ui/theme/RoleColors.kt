package app.lineo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The container and content colour a [LineoRole] resolves to in a given scheme.
 *
 * [container] is [Color.Transparent] for a role that paints no background of its own —
 * [LineoRole.Result] draws on whatever is behind it. [content] is transparent for a role
 * that is a shape rather than text, which is the case for [LineoRole.ErrorUnderline].
 */
@Immutable
data class RoleColors(val container: Color, val content: Color) {

    companion object {

        /**
         * Resolves [role] against the active `MaterialTheme.colorScheme`.
         *
         * This is the only place in the app allowed to read a colour token by name.
         */
        @Composable
        @ReadOnlyComposable
        fun of(role: LineoRole): RoleColors = of(MaterialTheme.colorScheme, role)

        /**
         * Resolves [role] against an explicit [scheme].
         *
         * The mapping is the table in `docs/CONVENTIONS.md` §10, transcribed row for row.
         * It is a plain function and not only a composable so that `RoleContrastTest` can
         * measure every pair without standing up a composition — an untestable mapping is
         * how the error row came to ship at 1.31:1 in the first place.
         */
        @Suppress("CyclomaticComplexMethod")
        fun of(scheme: ColorScheme, role: LineoRole): RoleColors = when (role) {
            LineoRole.Digit -> RoleColors(scheme.surfaceContainerLowest, scheme.onSurface)
            LineoRole.Operator -> RoleColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
            // Fixed rather than primary: the accent keeps the same yellow in both schemes,
            // which is what the reference design shows and what the role exists for.
            LineoRole.Equals -> RoleColors(scheme.primaryFixed, scheme.onPrimaryFixed)
            // Clear shares its colour with Equals, per §10, and they are told apart by
            // position — opposite corners of the grid — never by hue.
            LineoRole.Clear -> RoleColors(scheme.primaryFixed, scheme.onPrimaryFixed)
            LineoRole.Function -> RoleColors(scheme.surfaceContainer, scheme.onSurfaceVariant)
            LineoRole.Editor -> RoleColors(scheme.surface, scheme.onSurface)
            LineoRole.Result -> RoleColors(Color.Transparent, scheme.onSurfaceVariant)
            LineoRole.ErrorUnderline -> RoleColors(scheme.error, Color.Transparent)
            LineoRole.ErrorMessage -> RoleColors(scheme.errorContainer, scheme.onErrorContainer)
            LineoRole.SuggestionChip -> RoleColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
            LineoRole.StaleRateBadge -> RoleColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        }
    }
}
