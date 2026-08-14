package app.lineo.engine.eval

/**
 * Nearest-match suggestion for an unknown name, so `sni(1)` can offer `sin`
 * (`docs/GRAMMAR.md` §5, TASKS.md P0-08).
 *
 * Plain Levenshtein distance with a small threshold: a suggestion that is not obviously
 * right is worse than none, because it turns into a tap-to-fix chip in the editor.
 */
object Suggestions {
    private const val MAX_DISTANCE = 2

    fun nearest(name: String, candidates: Iterable<String>): String? = candidates
        .asSequence()
        .filter { it != name }
        .map { candidate -> candidate to distance(name.lowercase(), candidate.lowercase()) }
        .filter { (_, distance) -> distance <= MAX_DISTANCE }
        .minWithOrNull(compareBy({ it.second }, { it.first.length }, { it.first }))
        ?.first

    /** Levenshtein distance, two rows at a time. */
    fun distance(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)

        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val substitution = previous[j] + if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }

        return previous[right.length]
    }
}
