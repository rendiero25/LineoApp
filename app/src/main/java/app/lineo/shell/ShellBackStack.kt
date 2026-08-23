package app.lineo.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Where the shell is, and how it got there (P1-14).
 *
 * A list the shell owns, not a library's. That is a deliberate shape: `TASKS.md` records
 * Navigation 3 as the chosen library and defers adding it until a destination needs a real
 * back stack or a deep link, and Nav 3's whole model is a back stack the caller holds. When
 * that day comes this class is what it replaces, and `ModuleNav` means no feature notices
 * (`docs/ARCHITECTURE.md` §4).
 *
 * **The rules, and why each exists:**
 *
 * - [Destination.Notepad] is the root and is never popped. The document is what the app is;
 *   there is nowhere behind it, and back there belongs to the activity.
 * - The notepad and a [Destination.Module] are **work surfaces**; history and settings are
 *   **side trips**. Opening a work surface ends any side trip and clears the stack down to
 *   the root, because choosing a calculator from the menu is a decision to go and work
 *   somewhere, not one more screen to come back through. Opening the converter from the
 *   scientific keypad is the same move sideways.
 * - A side trip pushes. This is what keeps "leave settings, return to the module you opened
 *   them from" — behaviour the three booleans had by accident, because `openModuleId`
 *   survived underneath them, and the one thing about them worth keeping.
 * - Opening something already in the stack pops back to it instead of pushing again.
 *   `ModuleMenu` is reachable from every destination, so without this a user cycling between
 *   settings and history would build a stack with no bound and a back gesture with no end.
 */
@Stable
internal class ShellBackStack(initial: List<Destination> = listOf(Destination.Notepad)) {

    private val entries = mutableStateListOf<Destination>().apply {
        addAll(initial.ifEmpty { listOf(Destination.Notepad) })
    }

    /** The destination showing now. Never null: the notepad is always underneath. */
    val current: Destination get() = entries.last()

    /** Goes to [destination], by the rules above. */
    fun open(destination: Destination) {
        if (destination == current) return

        if (destination is Destination.Notepad) {
            // Not a push. Reuse from the tape lands here, and it means "start again with the
            // document", not "one more screen to come back through".
            entries.removeAll { it != Destination.Notepad }
            return
        }

        val existing = entries.indexOf(destination)
        if (existing >= 0) {
            entries.removeRange(existing + 1, entries.size)
            return
        }

        // A module is a work surface, not a side trip. Opening one ends whatever detour the
        // user was on and sits beside the notepad rather than on top of it — including the
        // detour it was opened *from*, since `ModuleMenu` is reachable from settings too.
        if (destination is Destination.Module) {
            entries.removeAll { it != Destination.Notepad }
        }
        entries.add(destination)
    }

    /**
     * Pops one destination.
     *
     * @return false at the root, so the caller can hand back to the system rather than
     *   swallowing the gesture and stranding the user in an app they cannot leave.
     */
    fun back(): Boolean {
        if (entries.size <= 1) return false
        entries.removeAt(entries.lastIndex)
        return true
    }

    internal companion object {

        /**
         * The saved form: one string per entry.
         *
         * Strings rather than `@Parcelize`, which is a Gradle plugin and so a dependency
         * decision `AGENTS.md` §7 reserves for a human. A module id may itself contain the
         * separator, so [MODULE] is stripped as a prefix rather than the value being split.
         */
        fun save(stack: ShellBackStack): List<String> = stack.entries.map { destination ->
            when (destination) {
                Destination.Notepad -> NOTEPAD
                Destination.History -> HISTORY
                Destination.Settings -> SETTINGS
                is Destination.Module -> MODULE + destination.id
            }
        }

        /**
         * Rebuilds a stack from [saved].
         *
         * An entry this build does not recognise drops the whole stack back to the notepad.
         * The saved state outlives the build that wrote it — an upgrade, or a downgrade —
         * and landing on the document is always safe, where guessing is not.
         */
        fun restore(saved: List<String>): ShellBackStack {
            val entries = saved.map { entry ->
                when {
                    entry == NOTEPAD -> Destination.Notepad
                    entry == HISTORY -> Destination.History
                    entry == SETTINGS -> Destination.Settings
                    entry.startsWith(MODULE) -> Destination.Module(entry.removePrefix(MODULE))
                    else -> return ShellBackStack()
                }
            }
            return ShellBackStack(entries)
        }

        val Saver: Saver<ShellBackStack, List<String>> = Saver(
            save = { save(it) },
            restore = { restore(it) },
        )

        private const val NOTEPAD = "notepad"
        private const val HISTORY = "history"
        private const val SETTINGS = "settings"
        private const val MODULE = "module:"
    }
}

/** The shell's back stack, surviving process death (P1-14). */
@Composable
internal fun rememberShellBackStack(): ShellBackStack =
    rememberSaveable(saver = ShellBackStack.Saver) { ShellBackStack() }
