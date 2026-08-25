package app.lineo.shell

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations for Navigation Compose.
 */
sealed interface Destination {

    @Serializable
    data object Notepad : Destination

    @Serializable
    data object History : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data class Module(val id: String) : Destination
}
