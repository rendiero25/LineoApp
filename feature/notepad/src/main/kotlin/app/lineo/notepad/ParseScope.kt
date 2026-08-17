package app.lineo.notepad

/**
 * The names a line is parsed against: everything defined by the lines above it.
 *
 * Held as the document's definitions plus a depth rather than as a set, so that every line in
 * one document shares one list and two rebuilds of an unchanged document produce equal keys
 * for [ParseCache].
 *
 * @param definitionsByLine the name each line defines, in document order, `null` where a line
 *   defines nothing.
 * @param depth how many lines are above the line being parsed.
 */
data class ParseScope(private val definitionsByLine: List<String?>, private val depth: Int) {

    /** The names in scope. Built only when a parse actually has to happen. */
    fun names(): Set<String> = definitionsByLine.take(depth).filterNotNullTo(LinkedHashSet())

    companion object {
        /** Nothing defined — the scope the definition-finding pass reads every line in. */
        val EMPTY = ParseScope(emptyList(), 0)
    }
}
