package app.lineo.notepad

import app.lineo.engine.CalcResult
import app.lineo.engine.LineId
import app.lineo.engine.Quantity
import app.lineo.engine.parser.Ast

/**
 * Every line of a document and what it currently evaluates to.
 *
 * Also the input to the next evaluation: it carries the graph and the text it was computed
 * from, which is how [NotepadEvaluator] works out what a change actually invalidated instead
 * of recomputing the document (`docs/ARCHITECTURE.md` §6).
 */
class DocumentEvaluation internal constructor(
    /** One entry per line, in document order. */
    val results: Map<LineId, LineEvaluation>,
    internal val graph: DependencyGraph,
    internal val sources: Map<LineId, String>,
) {

    /** What this line evaluated to. [LineEvaluation.Empty] for a line that is not here at all. */
    operator fun get(id: LineId): LineEvaluation = results[id] ?: LineEvaluation.Empty

    /** The lines that produced a value, and what it was. */
    val values: Map<LineId, Quantity>
        get() = results.mapNotNull { (id, evaluation) ->
            (evaluation as? LineEvaluation.Value)?.let { id to it.value }
        }.toMap()

    /** The circles of references in the document, every line of each one (`docs/GRAMMAR.md` §3.7). */
    val cycles: List<List<LineId>> get() = graph.cycles

    /**
     * How the parser read this line, or `null` for a line that does not parse.
     *
     * Handed out for the screen reader (`docs/CONVENTIONS.md` §8): what TalkBack says about
     * an expression comes from the tree, and parsing the line a second time to build that
     * sentence would be work this already did — in the scope the line actually sits in,
     * which a caller with only the text could not reproduce.
     */
    fun astOf(id: LineId): Ast? = (graph.parsed[id] as? CalcResult.Ok)?.value
}
