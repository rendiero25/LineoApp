package app.lineo.registry

import kotlinx.coroutines.flow.Flow

/**
 * What an input surface may ask the editor to do, per `docs/ARCHITECTURE.md` §5.
 *
 * The editor never receives raw key events. Every surface — the keypad, the accessory row,
 * the system keyboard adapter, a suggestion chip — speaks only these commands, which is
 * what makes the Phase 1 → Phase 2 input change additive instead of a rewrite.
 */
sealed interface EditorCommand {

    data class InsertText(val text: String) : EditorCommand

    /** Inserts `name(` with the caret between the parentheses; [arity] drives the separators. */
    data class InsertFunction(val name: String, val arity: Int) : EditorCommand

    data class WrapSelection(val open: String, val close: String) : EditorCommand

    /** Moves the caret by [delta] characters. Negative moves left. */
    data class MoveCursor(val delta: Int) : EditorCommand

    data object Backspace : EditorCommand

    /**
     * Empties the line the caret is on. The `AC` key.
     *
     * The *line*, not the document. A notepad line is one calculation, so clearing it is
     * what a calculator's all-clear has always meant; clearing the document would discard
     * work the user cannot get back, which `docs/SPEC.md` and P1-03 both rule out.
     */
    data object ClearLine : EditorCommand

    /**
     * Flips the sign of the number the caret is in or just after. The `±` key.
     *
     * A command rather than an `InsertText("-")` because it is a toggle, and because only
     * the editor knows where the current number starts — the surface that emits this has no
     * idea what has been typed.
     */
    data object ToggleSign : EditorCommand

    data object NewLine : EditorCommand

    /** Swaps the calculator keypad for the system keyboard, the `Aa` button. */
    data object ToggleTextInput : EditorCommand
}

/**
 * Anything the user can type with.
 *
 * Implementations: accessory row, custom keypad, system keyboard adapter, hardware
 * keyboard, suggestion chips. The editor is agnostic to which one is attached.
 */
interface InputSurface {
    val commands: Flow<EditorCommand>
}
