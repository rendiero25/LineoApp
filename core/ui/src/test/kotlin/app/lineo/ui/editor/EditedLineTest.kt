package app.lineo.ui.editor

import app.lineo.registry.EditorCommand
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The line edit itself, without a state holder around it.
 *
 * `EditorStateTest` covers what the single-line editor does with these; what is asserted
 * here is the contract the notepad relies on — that a command applied to a line is a pure
 * function of the line, and that the two commands which are not line edits say so by
 * changing nothing.
 */
class EditedLineTest {

    @Test
    fun `text goes in at the caret, and the caret follows it`() {
        val line = EditedLine("12", caret = 1)

        assertEquals(EditedLine("152", 2), line.applying(EditorCommand.InsertText("5")))
    }

    @Test
    fun `a function lands with the caret between its brackets`() {
        val line = EditedLine("")

        val edited = line.applying(EditorCommand.InsertFunction("sqrt", arity = 1))

        assertEquals(EditedLine("sqrt()", 5), edited)
    }

    @Test
    fun `backspace at the start of a line changes nothing`() {
        val line = EditedLine("5", caret = 0)

        assertEquals(line, line.applying(EditorCommand.Backspace))
    }

    @Test
    fun `all clear empties the line and takes the caret with it`() {
        val line = EditedLine("1 + 2", caret = 5)

        assertEquals(EditedLine("", 0), line.applying(EditorCommand.ClearLine))
    }

    @Test
    fun `the sign key negates the number at the caret, and undoes itself`() {
        val line = EditedLine("5 * 3", caret = 5)

        val negated = line.applying(EditorCommand.ToggleSign)

        assertEquals(EditedLine("5 * -3", 6), negated)
        assertEquals(line, negated.applying(EditorCommand.ToggleSign))
    }

    @Test
    fun `a comma decimal separator is part of the number the sign key finds`() {
        val line = EditedLine("2,5", caret = 3)

        assertEquals(EditedLine("-2,5", 4), line.applying(EditorCommand.ToggleSign, decimalSeparator = ','))
    }

    @Test
    fun `a caret outside the text is brought back inside rather than throwing`() {
        val line = EditedLine("12", caret = 99)

        assertEquals(EditedLine("125", 3), line.applying(EditorCommand.InsertText("5")))
    }

    @Test
    fun `the commands that are not line edits leave the line alone`() {
        val line = EditedLine("1 + 2", caret = 2)

        assertEquals(line, line.applying(EditorCommand.NewLine))
        assertEquals(line, line.applying(EditorCommand.ToggleTextInput))
    }
}
