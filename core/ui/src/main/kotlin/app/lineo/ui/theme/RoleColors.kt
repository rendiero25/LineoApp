package app.lineo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The container and content colour a [LineoRole] resolves to in the current scheme.
 *
 * [container] is [Color.Transparent] for a role that paints no background of its own —
 * [LineoRole.Result] draws on whatever is behind it.
 */
@Immutable
data class RoleColors(val container: Color, val content: Color) {

    companion object {

        /**
         * Resolves [role] against the active `MaterialTheme.colorScheme`.
         *
         * This is the only place in the app allowed to read a colour token by name; the
         * mapping is the table in `docs/CONVENTIONS.md` §10, transcribed row for row.
         */
        @Composable
        @ReadOnlyComposable
        fun of(role: LineoRole): RoleColors {
            val scheme = MaterialTheme.colorScheme
            return when (role) {
                LineoRole.Digit -> RoleColors(scheme.surfaceContainerHigh, scheme.onSurface)
                LineoRole.Operator -> RoleColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
                LineoRole.Equals -> RoleColors(scheme.primary, scheme.onPrimary)
                LineoRole.Clear -> RoleColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
                LineoRole.Function -> RoleColors(scheme.surfaceContainer, scheme.onSurfaceVariant)
                LineoRole.Editor -> RoleColors(scheme.surface, scheme.onSurface)
                LineoRole.Result -> RoleColors(Color.Transparent, scheme.onSurfaceVariant)
                LineoRole.Error -> RoleColors(scheme.error, scheme.onErrorContainer)
                LineoRole.SuggestionChip -> RoleColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
                LineoRole.StaleRateBadge -> RoleColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
            }
        }
    }
}
