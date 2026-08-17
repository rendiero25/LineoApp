package app.lineo.ui.editor

import app.lineo.registry.EditorCommand

/**
 * A line of text and where the caret sits in it.
 *
 * The unit every input surface actually edits. [EditorState] holds one of these as Compose
 * state for the line being typed; a notepad holds one per line and keeps the rest as text.
 */
data class EditedLine(val text: String = "", val caret: Int = text.length) {

    /** The caret, clamped into the text. Nothing outside this file has to remember to do it. */
    val safeCaret: Int get() = caret.coerceIn(0, text.length)
}

/**
 * Applies one command to a line and returns the line that results.
 *
 * Pure, and public, because two callers need exactly this and neither should have its own
 * version of it: the single-line editor of `:core:ui` and the notepad, which applies the
 * same commands to whichever of its lines has focus. A second implementation of `±` alone
 * would be forty lines and ten tests of drift.
 *
 * [EditorCommand.NewLine] and [EditorCommand.ToggleTextInput] are returned unchanged. They
 * are not edits to a line: one asks for another line and the other asks for another
 * keyboard, and only the caller knows what it has. Handle them there.
 *
 * @param decimalSeparator what counts as part of a number when `±` looks for one
 *   (`docs/CONVENTIONS.md` §2).
 */
fun EditedLine.applying(command: EditorCommand, decimalSeparator: Char = '.'): EditedLine = when (command) {
    is EditorCommand.InsertText -> insert(command.text)
    is EditorCommand.InsertFunction -> insert("${command.name}()", caretOffset = -1)
    is EditorCommand.WrapSelection -> insert(command.open + command.close, caretOffset = -command.close.length)
    is EditorCommand.MoveCursor -> copy(caret = (safeCaret + command.delta).coerceIn(0, text.length))
    EditorCommand.Backspace -> backspace()
    EditorCommand.ClearLine -> EditedLine("", 0)
    EditorCommand.ToggleSign -> toggleSign(text, safeCaret, decimalSeparator).let { EditedLine(it.text, it.caret) }
    EditorCommand.NewLine, EditorCommand.ToggleTextInput -> this
}

private fun EditedLine.insert(inserted: String, caretOffset: Int = 0): EditedLine {
    val at = safeCaret
    return EditedLine(
        text = text.substring(0, at) + inserted + text.substring(at),
        caret = at + inserted.length + caretOffset,
    )
}

private fun EditedLine.backspace(): EditedLine {
    val at = safeCaret
    return if (at == 0) this else EditedLine(text.removeRange(at - 1, at), at - 1)
}
