package app.lineo.notepad

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.EvalContext
import app.lineo.engine.LineId
import app.lineo.engine.Quantity
import app.lineo.engine.eval.Evaluator
import app.lineo.engine.parser.Ast

/**
 * Evaluates a document, and re-evaluates only what a change actually reached.
 *
 * `docs/ARCHITECTURE.md` §6 and §9 set the shape of this: a change to line N re-evaluates N
 * and its transitive dependents, and a 200-line document must answer a keystroke in under
 * 50 ms. Both come from the same idea — the work a keystroke costs is the size of what it
 * invalidated, not the size of the document.
 *
 * Stateful only in its caches, and every result it returns is a value. Hold one per open
 * document; the [ParseCache] inside it is what makes a keystroke cost one parse.
 *
 * @param context the locale, angle mode and functions every line is evaluated against.
 * @param evaluateAst seam for tests, and the reason the evaluations can be counted. Defaults
 *   to the engine's own evaluator, called with an already-parsed line.
 */
class NotepadEvaluator(
    private val context: EvalContext = EvalContext(),
    private val cache: ParseCache = ParseCache(),
    private val evaluateAst: (Ast, EvalContext) -> CalcResult<Quantity> = { ast, evalContext ->
        Evaluator(evalContext).evaluate(ast)
    },
) {

    /** Evaluates every line. Use [reevaluate] once there is a previous answer to build on. */
    fun evaluate(document: NotepadDocument): DocumentEvaluation = recompute(document, previous = null)

    /**
     * Evaluates the lines a change reached, and keeps the rest of [previous].
     *
     * The change does not have to be described: what is stale is derived by comparing the
     * document with the one [previous] was computed from. That is deliberate — an editor
     * that had to report its own edits correctly would be one more thing that can be wrong,
     * and the failure would be a stale number on screen rather than a crash.
     */
    fun reevaluate(previous: DocumentEvaluation, document: NotepadDocument): DocumentEvaluation =
        recompute(document, previous)

    private fun recompute(document: NotepadDocument, previous: DocumentEvaluation?): DocumentEvaluation {
        val graph = DependencyGraph.of(document, context, cache)
        val sources = document.lines.associate { it.id to it.source }
        val results = LinkedHashMap<LineId, LineEvaluation>()
        val values = LinkedHashMap<LineId, Quantity>()

        previous?.let { reusable(it, document, graph) }?.forEach { (id, evaluation) ->
            results[id] = evaluation
            (evaluation as? LineEvaluation.Value)?.let { values[id] = it.value }
        }

        graph.order.forEach { id ->
            if (id in results) return@forEach
            val evaluation = evaluateLine(id, document, graph, results, values)
            results[id] = evaluation
            when (evaluation) {
                is LineEvaluation.Value -> values[id] = evaluation.value
                // A line that lost its value must not leave the old one behind for the lines
                // that read it: they would be told a number that is no longer true.
                else -> values.remove(id)
            }
        }

        return DocumentEvaluation(
            results = document.lines.associate { it.id to results.getValue(it.id) },
            graph = graph,
            sources = sources,
        )
    }

    /**
     * The results of [previous] that are still true.
     *
     * A line is stale when its text changed, when what it reads changed, when it entered or
     * left a circle, or when it is new. So is everything that reads a stale line, however far
     * down the chain — that transitive step is the whole reason the graph exists.
     */
    private fun reusable(
        previous: DocumentEvaluation,
        document: NotepadDocument,
        graph: DependencyGraph,
    ): Map<LineId, LineEvaluation> {
        val stale = mutableSetOf<LineId>()
        document.lines.forEach { line ->
            val changed = line.id !in previous.results ||
                previous.sources[line.id] != line.source ||
                previous.graph.dependenciesOf(line.id) != graph.dependenciesOf(line.id) ||
                previous.graph.cycleOf(line.id) != graph.cycleOf(line.id)
            if (changed) {
                stale += line.id
                stale += graph.transitiveDependentsOf(line.id)
            }
        }
        return previous.results.filterKeys { it !in stale && document.line(it) != null }
    }

    private fun evaluateLine(
        id: LineId,
        document: NotepadDocument,
        graph: DependencyGraph,
        results: Map<LineId, LineEvaluation>,
        values: Map<LineId, Quantity>,
    ): LineEvaluation {
        val line = document.line(id)
        val dependencies = graph.dependenciesOf(id)
        // Every line of a circle is marked, carrying the whole chain, so the editor can say
        // which lines are involved rather than which one the walk happened to close it on.
        val cycle = graph.cycleOf(id)
        val blocking = dependencies.firstOrNull { results[it] !is LineEvaluation.Value }

        return when {
            line == null || line.source.isBlank() -> LineEvaluation.Empty
            cycle != null -> LineEvaluation.Failed(CalcError.CircularReference(cycle))
            blocking != null -> LineEvaluation.Blocked(blocking)
            else -> evaluateParsed(id, graph, dependencies, values)
        }
    }

    private fun evaluateParsed(
        id: LineId,
        graph: DependencyGraph,
        dependencies: Set<LineId>,
        values: Map<LineId, Quantity>,
    ): LineEvaluation {
        // Only the variables this line actually reads. A document-wide map would be the same
        // answer at O(lines) cost per line, and this one is already known: a variable it
        // reads is, by construction, one of its dependencies.
        val variables = dependencies.mapNotNull { dependency ->
            graph.definitions[dependency]?.let { name -> name to values.getValue(dependency) }
        }.toMap()

        return when (val parsed = graph.parsed.getValue(id)) {
            is CalcResult.Err -> LineEvaluation.Failed(parsed.error)
            is CalcResult.Ok -> when (
                val result = evaluateAst(
                    parsed.value,
                    context.copy(variables = variables, lineResults = values),
                )
            ) {
                is CalcResult.Ok -> LineEvaluation.Value(result.value)
                is CalcResult.Err -> LineEvaluation.Failed(result.error)
            }
        }
    }
}
