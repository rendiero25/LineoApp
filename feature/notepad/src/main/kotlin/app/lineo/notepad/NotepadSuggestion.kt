package app.lineo.notepad

/**
 * A name the user can tap instead of typing it.
 *
 * Only names the document already contains: a variable defined above the caret, or a unit
 * a line above has already produced. Nothing is invented — a chip that offered `kg` to
 * someone writing a budget would be noise, and the document is the only evidence of what
 * this particular user is working in.
 *
 * @param text what is inserted, which is also what is shown. A variable is inserted as
 *   written; a unit as its ISO 80000 symbol (`docs/CONVENTIONS.md` §5).
 */
data class NotepadSuggestion(val text: String, val kind: Kind) {

    enum class Kind {
        /** A name defined by a line above the caret — the only ones §3.1 puts in scope. */
        Variable,

        /** A unit a line above the caret has already produced a value in. */
        Unit,
    }
}
