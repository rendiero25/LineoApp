package app.lineo.engine.eval

/**
 * Nearest-match suggestion for an unknown name, so `sni(1)` can offer `sin`
 * (`docs/GRAMMAR.md` §5, TASKS.md P0-08).
 *
 * Damerau-Levenshtein distance with a small threshold: a suggestion that is not obviously
 * right is worse than none, because it turns into a tap-to-fix chip in the editor.
 *
 * Transposition counts as one edit rather than two. Swapped letters are the commonest typo,
 * and under plain Levenshtein `sni` is as far from `sin` as it is from the unit `s` — which
 * would make the chip offer `s`.
 *
 * **The threshold scales with the name.** A flat two edits is generous for `lenght` and
 * meaningless for `x`: every unit symbol in the registry is a candidate, and at two edits a
 * one-letter name matches most of them. On a device that produced "Unknown name 'x' → Use
 * 'K'", where `K` had won only for sorting first. A name has to be long enough to be worth
 * guessing about — see [maxDistanceFor].
 */
object Suggestions {
    private const val MAX_DISTANCE = 2

    fun nearest(name: String, candidates: Iterable<String>): String? {
        val limit = maxDistanceFor(name)
        if (limit == 0) return null
        return candidates
            .asSequence()
            .filter { it != name }
            .map { candidate -> candidate to distance(name.lowercase(), candidate.lowercase()) }
            .filter { (_, distance) -> distance <= limit }
            // Nearest first; then the candidate closest in length to what was typed, which is
            // what makes `sni` prefer `sin` over `sinh`. Case-insensitive last, so a candidate
            // is never picked for being uppercase — that is what offered `K` for `x`.
            .minWithOrNull(
                compareBy<Pair<String, Int>> { it.second }
                    .thenBy { kotlin.math.abs(it.first.length - name.length) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first },
            )
            ?.first
    }

    /**
     * How far a guess may be from [name] before it stops being a guess about [name].
     *
     * One edit fewer than the name is long, capped at [MAX_DISTANCE]: a single letter gets no
     * suggestion at all, two letters allow one edit, and anything from three letters up keeps
     * the full tolerance that lets `sni` reach `sin`.
     */
    private fun maxDistanceFor(name: String): Int = minOf(MAX_DISTANCE, name.length - 1)

    /** Optimal string alignment: insert, delete, substitute, or swap two adjacent letters. */
    fun distance(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        val rows = Array(left.length + 1) { IntArray(right.length + 1) }
        for (i in 0..left.length) rows[i][0] = i
        for (j in 0..right.length) rows[0][j] = j

        for (i in 1..left.length) {
            for (j in 1..right.length) {
                rows[i][j] = step(rows, left, right, i, j)
            }
        }

        return rows[left.length][right.length]
    }

    private fun step(rows: Array<IntArray>, left: String, right: String, i: Int, j: Int): Int {
        val substitution = rows[i - 1][j - 1] + if (left[i - 1] == right[j - 1]) 0 else 1
        val best = minOf(rows[i - 1][j] + 1, rows[i][j - 1] + 1, substitution)

        val transposed = i > 1 && j > 1 && left[i - 1] == right[j - 2] && left[i - 2] == right[j - 1]
        return if (transposed) minOf(best, rows[i - 2][j - 2] + 1) else best
    }
}
