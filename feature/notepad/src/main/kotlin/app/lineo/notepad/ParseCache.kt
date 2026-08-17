package app.lineo.notepad

import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.parser.Ast
import java.math.BigDecimal

/**
 * Remembers how a line parsed, so that a keystroke costs one parse rather than one per line.
 *
 * Without it, every rebuild of a [DependencyGraph] re-reads the whole document, and the
 * 50 ms budget of `docs/ARCHITECTURE.md` §9 for a 200-line document would go on parsing text
 * that has not changed since the last keystroke.
 *
 * A cache entry is keyed by the text *and* by the scope it was read in, never by the text
 * alone: `2k` is two thousand until some line above defines `k`, and then it is twice `k`
 * (`docs/GRAMMAR.md` §3.2). Keying by text alone would serve the wrong reading of the same
 * characters to a line further down the same document.
 *
 * Not thread-safe. One document is edited by one editor, and the evaluator that owns the
 * cache runs on one dispatcher.
 */
class ParseCache(private val capacity: Int = DEFAULT_CAPACITY) {

    private val entries = object : LinkedHashMap<Key, CalcResult<Ast>>(INITIAL_BUCKETS, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, CalcResult<Ast>>): Boolean =
            size > capacity
    }

    /** How many parses this cache has actually performed. Read by the tests that prove it works. */
    var misses: Int = 0
        private set

    fun parse(source: String, scope: ParseScope, context: EvalContext): CalcResult<Ast> =
        entries.getOrPut(Key(source, scope)) {
            misses++
            Engine.parse(source, context.copy(variables = scope.names().associateWith { PARSE_PLACEHOLDER }))
        }

    private data class Key(val source: String, val scope: ParseScope)

    private companion object {

        /**
         * Entries kept before the least recently used one is dropped.
         *
         * A document is parsed at two scopes — empty, for the pass that finds the definitions,
         * and its own — so a 500-line document fits with room to spare. The cap exists so that
         * a long editing session cannot grow the map without bound, not to be tuned.
         */
        const val DEFAULT_CAPACITY = 2048

        const val INITIAL_BUCKETS = 64
        const val LOAD_FACTOR = 0.75f

        /**
         * Stands in for a variable's value while a line is only being parsed.
         *
         * The parse reads the *names* in scope, never their values — that is how `2k` is told
         * apart from two thousand. Nothing evaluates this.
         */
        val PARSE_PLACEHOLDER = Quantity(BigDecimal.ZERO)
    }
}
