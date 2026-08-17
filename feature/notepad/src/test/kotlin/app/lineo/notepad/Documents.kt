package app.lineo.notepad

/**
 * Builds documents the way the editor does: from what the user would type.
 *
 * Two things it takes care of, and both matter to every test that uses it. Lines are written
 * as display text, so `line3` in a test means the third line on screen, the same as it would
 * to a user. And the ids are made not to match the ordinals — a document whose first line has
 * id 1 lets an assertion pass on a model that confuses the two.
 */
internal object Documents {

    fun of(vararg displayLines: String): NotepadDocument {
        val empty = displayLines.fold(offsetIds()) { document, _ -> document.append() }
        return displayLines.foldIndexed(empty) { index, document, typed ->
            document.setDisplayText(document.idAtOrdinal(index + 1)!!, typed)
        }
    }

    /** An empty document that has already handed out — and lost — two ids. */
    private fun offsetIds(): NotepadDocument {
        val padded = NotepadDocument.of("pad", "pad")
        return padded.delete(padded.idAtOrdinal(1)!!).delete(padded.idAtOrdinal(2)!!)
    }
}
