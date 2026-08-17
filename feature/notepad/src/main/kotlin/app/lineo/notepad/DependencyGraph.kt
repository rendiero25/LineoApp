package app.lineo.notepad

import app.lineo.engine.CalcResult
import app.lineo.engine.EvalContext
import app.lineo.engine.LineId
import app.lineo.engine.parser.Ast

/**
 * What each line of a document reads, in what order the lines can be evaluated, and which of
 * them refer to each other in a circle.
 *
 * Two kinds of edge, and they behave differently on purpose:
 *
 * - **A line reference** (`line3`, `@3`) may point anywhere, above or below. Nothing in
 *   `docs/GRAMMAR.md` §3.7 restricts the direction, and a document that totals its lines at
 *   the top is a real way to write one. Because they may point downwards, they are also the
 *   only way to build a circle.
 * - **A variable** is visible only *below* its definition — §3.1 says "if the user wrote
 *   `m = 5` earlier". So variable edges always point backwards, and a name used above its
 *   definition is not a dependency at all: it is an unknown identifier, which is what the
 *   user sees.
 *
 * Where a name is defined more than once, the nearest definition above wins. `x = 1` at the
 * top and `x = 5` further down means the lines in between read 1 and the lines after read 5,
 * which is how a document is read aloud and how a re-derivation should behave.
 *
 * The graph is a pure function of the document. It holds no results and no caching; keeping
 * results in step with it is P1-02-2's job.
 */
class DependencyGraph private constructor(
    /**
     * Every line, dependencies before dependents.
     *
     * Not document order: a line reference that points downwards makes the two differ. Lines
     * that take part in a circle are still here — in document order among themselves — so
     * that a caller walking [order] visits every line exactly once and can decide what to do
     * with them from [cycles].
     */
    val order: List<LineId>,

    /** Each circle of references, every line in it, in document order. */
    val cycles: List<List<LineId>>,

    /** The name each line defines, for the lines that define one. */
    val definitions: Map<LineId, String>,

    /**
     * Each line as the parser read it, in the scope it sits in.
     *
     * Handed on rather than thrown away: building the graph already parsed every line, and
     * the evaluator would otherwise parse them all a second time on the way to a result.
     */
    val parsed: Map<LineId, CalcResult<Ast>>,

    private val reads: Map<LineId, Set<LineId>>,
    private val readBy: Map<LineId, Set<LineId>>,
) {

    /** Lines that take part in any circle. */
    val inCycle: Set<LineId> = cycles.flatten().toSet()

    /** The lines this one reads directly. */
    fun dependenciesOf(id: LineId): Set<LineId> = reads[id].orEmpty()

    /** The lines that read this one directly. */
    fun dependentsOf(id: LineId): Set<LineId> = readBy[id].orEmpty()

    /**
     * Every line that reads this one, directly or through others — exactly the set a change
     * to it makes stale, and nothing more. This is what keeps a keystroke from recomputing
     * the document (`docs/ARCHITECTURE.md` §6).
     */
    fun transitiveDependentsOf(id: LineId): Set<LineId> {
        val found = LinkedHashSet<LineId>()
        val pending = ArrayDeque(dependentsOf(id))
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            if (found.add(next)) pending += dependentsOf(next)
        }
        return found
    }

    /** The circle this line takes part in, or `null` when it is not in one. */
    fun cycleOf(id: LineId): List<LineId>? = cycles.firstOrNull { id in it }

    companion object {

        /**
         * Builds the graph for a document.
         *
         * Two passes over the lines, because the first one cannot be avoided: whether `2k` is
         * two thousand or twice `k` depends on which names are defined (§3.2), so the names
         * have to be known before the lines that use them are parsed. An assignment's own
         * name does not depend on that — it is the head of the line — so pass one can read
         * every definition with nothing defined.
         *
         * @param context the locale, angle mode and functions the lines are parsed against.
         *   Its variables are replaced per line by the names in scope there.
         * @param cache reuses the parse of a line whose text and scope have not changed.
         *   Pass one that outlives the document version — that is what makes a keystroke cost
         *   one parse instead of one per line. A fresh cache is correct, only slower.
         */
        fun of(
            document: NotepadDocument,
            context: EvalContext = EvalContext(),
            cache: ParseCache = ParseCache(),
        ): DependencyGraph {
            val definitions = definitionsOf(document, context, cache)
            val parsed = parsedBy(document, context, definitions, cache)
            val reads = readsOf(document, definitions, parsed)
            val readBy = reads.entries
                .flatMap { (reader, targets) -> targets.map { it to reader } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, readers) -> readers.toSet() }
            val position = document.lines.withIndex().associate { (index, line) -> line.id to index }
            val components = stronglyConnectedComponents(document.lines.map { it.id }) { id ->
                reads.getValue(id).sortedBy { position.getValue(it) }
            }

            return DependencyGraph(
                order = components.flatMap { component -> component.sortedBy { position.getValue(it) } },
                cycles = components
                    .filter { it.size > 1 || it.single() in reads.getValue(it.single()) }
                    .map { component -> component.sortedBy { position.getValue(it) } },
                definitions = definitions,
                parsed = parsed,
                reads = reads,
                readBy = readBy,
            )
        }

        /** Pass one: the name each line defines, read from the head of the line. */
        private fun definitionsOf(
            document: NotepadDocument,
            context: EvalContext,
            cache: ParseCache,
        ): Map<LineId, String> =
            document.lines.mapNotNull { line ->
                val parsed = cache.parse(line.source, ParseScope.EMPTY, context)
                val name = ((parsed as? CalcResult.Ok)?.value as? Ast.Assignment)?.name
                name?.let { line.id to it }
            }.toMap()

        /** Pass two: each line as read in its own scope — the names defined above it. */
        private fun parsedBy(
            document: NotepadDocument,
            context: EvalContext,
            definitions: Map<LineId, String>,
            cache: ParseCache,
        ): Map<LineId, CalcResult<Ast>> {
            val definedByLine = document.lines.map { definitions[it.id] }
            return document.lines.withIndex().associate { (index, line) ->
                // Depth `index`: what the lines above define, never what this line defines.
                line.id to cache.parse(line.source, ParseScope(definedByLine, index), context)
            }
        }

        /** What each line reads, from the parse and from the names in scope at that point. */
        private fun readsOf(
            document: NotepadDocument,
            definitions: Map<LineId, String>,
            parsed: Map<LineId, CalcResult<Ast>>,
        ): Map<LineId, Set<LineId>> {
            val visible = mutableMapOf<String, LineId>()
            return document.lines.associate { line ->
                val read = LinkedHashSet<LineId>()
                (parsed.getValue(line.id) as? CalcResult.Ok)?.let { collect(it.value, read, visible) }
                // Only now: a line's own definition is visible below it, never to itself.
                definitions[line.id]?.let { visible[it] = line.id }
                // A reference to a line that is not in the document is not an edge. It is a
                // broken reference, and `NotepadDocument.danglingReferences` is where it shows.
                line.id to read.filterTo(LinkedHashSet()) { document.line(it) != null }
            }
        }

        /**
         * Walks a parsed line, collecting the lines it reads.
         *
         * A conversion target is walked like anything else. It looks like a unit, but the
         * evaluator resolves it through the same order as any identifier, so `5 km to mi`
         * really does read a line that defines `mi` — and then reports that the target is
         * not a unit. The graph agrees with the evaluator rather than with the grammar's
         * intent, because the graph exists to decide what to recompute.
         */
        private fun collect(node: Ast, into: MutableSet<LineId>, visible: Map<String, LineId>) {
            when (node) {
                is Ast.LineReference -> into += LineId(node.line.toLong())
                is Ast.Identifier -> visible[node.name]?.let { into += it }
                is Ast.NumberLiteral -> Unit
                is Ast.Unary -> collect(node.operand, into, visible)
                is Ast.Postfix -> collect(node.operand, into, visible)
                is Ast.Binary -> {
                    collect(node.left, into, visible)
                    collect(node.right, into, visible)
                }
                is Ast.Conversion -> {
                    collect(node.value, into, visible)
                    collect(node.target, into, visible)
                }
                is Ast.Call -> node.arguments.forEach { collect(it, into, visible) }
                is Ast.Assignment -> collect(node.value, into, visible)
            }
        }

        /**
         * Tarjan's algorithm, with an explicit stack.
         *
         * Iterative rather than recursive because the depth is the length of a reference
         * chain, and a premium user's document has no line limit (`docs/SPEC.md` §4).
         *
         * Components come out with every component a node reaches emitted before the node's
         * own — which, since an edge means "reads", is dependencies before dependents.
         */
        private fun stronglyConnectedComponents(
            nodes: List<LineId>,
            edges: (LineId) -> List<LineId>,
        ): List<List<LineId>> {
            val walk = TarjanWalk(edges)
            nodes.forEach(walk::visit)
            return walk.components
        }
    }

    /** One node's state while [TarjanWalk] walks it: the node, its successors, how far along. */
    private class Frame(val node: LineId, private val successors: List<LineId>) {
        private var visited = 0

        fun nextSuccessor(): LineId? = successors.getOrNull(visited)?.also { visited++ }
    }

    /** The bookkeeping of Tarjan's algorithm, kept out of the loop that drives it. */
    private class TarjanWalk(private val edges: (LineId) -> List<LineId>) {

        val components = mutableListOf<List<LineId>>()

        private val index = mutableMapOf<LineId, Int>()
        private val low = mutableMapOf<LineId, Int>()
        private val onStack = mutableSetOf<LineId>()
        private val stack = ArrayDeque<LineId>()
        private val work = ArrayDeque<Frame>()
        private var next = 0

        fun visit(root: LineId) {
            if (root in index) return
            work.addLast(open(root))
            while (work.isNotEmpty()) {
                advance(work.last())
            }
        }

        private fun advance(frame: Frame) {
            when (val successor = frame.nextSuccessor()) {
                null -> close(frame)
                !in index -> work.addLast(open(successor))
                in onStack -> low[frame.node] = minOf(low.getValue(frame.node), index.getValue(successor))
                else -> Unit
            }
        }

        private fun open(node: LineId): Frame {
            index[node] = next
            low[node] = next
            next++
            stack.addLast(node)
            onStack += node
            return Frame(node, edges(node))
        }

        private fun close(frame: Frame) {
            work.removeLast()
            if (low.getValue(frame.node) == index.getValue(frame.node)) {
                components += popComponent(frame.node)
            }
            work.lastOrNull()?.let { parent ->
                low[parent.node] = minOf(low.getValue(parent.node), low.getValue(frame.node))
            }
        }

        private fun popComponent(root: LineId): List<LineId> {
            val component = mutableListOf<LineId>()
            do {
                val popped = stack.removeLast()
                onStack -= popped
                component += popped
            } while (popped != root)
            return component
        }
    }
}
