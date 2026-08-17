package app.lineo.notepad

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.eval.Evaluator
import app.lineo.engine.parser.Ast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P1-02: the document evaluates, and a change costs what it reached rather than what the
 * document is worth.
 *
 * The incremental half is asserted by counting — how many lines were evaluated, how many
 * lines were parsed — rather than by timing. A count is the same on any machine and says
 * precisely what went wrong when it moves; the 50 ms of `docs/ARCHITECTURE.md` §9 is a
 * device measurement and belongs to a Macrobenchmark, not to a unit test on a build agent.
 */
class NotepadEvaluatorTest {

    @Test
    fun `a variable takes the value of the line that defines it`() {
        val document = Documents.of("price = 250", "tax = price * 0.1", "price + tax")
        val evaluator = NotepadEvaluator()

        val evaluation = evaluator.evaluate(document)

        assertEquals(listOf("250", "25", "275"), evaluation.canonicalValues())
    }

    @Test
    fun `a line reference reads the line it names, wherever that line sits`() {
        val document = Documents.of("line3 + 1", "99", "10")

        val evaluation = NotepadEvaluator().evaluate(document)

        assertEquals(listOf("11", "99", "10"), evaluation.canonicalValues())
    }

    @Test
    fun `a blank line evaluates to nothing`() {
        val document = Documents.of("1 + 1", "   ", "2")

        val evaluation = NotepadEvaluator().evaluate(document)

        assertEquals(LineEvaluation.Empty, evaluation[document.idAtOrdinal(2)!!])
    }

    @Test
    fun `a line that reads a failed line is blocked, not wrong`() {
        // docs/ARCHITECTURE.md §3: line 2 is not the user's mistake, and must not be red.
        val document = Documents.of("x = 1 km + 1 kg", "x * 2")
        val failing = document.idAtOrdinal(1)!!

        val evaluation = NotepadEvaluator().evaluate(document)

        assertTrue(evaluation[failing].toString(), evaluation[failing] is LineEvaluation.Failed)
        assertEquals(LineEvaluation.Blocked(failing), evaluation[document.idAtOrdinal(2)!!])
    }

    @Test
    fun `blocked carries down the chain, naming the line each one is waiting on`() {
        val document = Documents.of("a = nope", "b = a + 1", "b + 1")

        val evaluation = NotepadEvaluator().evaluate(document)

        assertEquals(LineEvaluation.Blocked(document.idAtOrdinal(1)!!), evaluation[document.idAtOrdinal(2)!!])
        assertEquals(LineEvaluation.Blocked(document.idAtOrdinal(2)!!), evaluation[document.idAtOrdinal(3)!!])
    }

    @Test
    fun `every line of a circle is marked, and each one carries the whole chain`() {
        val document = Documents.of("line2 + 1", "line1 + 1", "7")
        val chain = document.lines.dropLast(1).map { it.id }

        val evaluation = NotepadEvaluator().evaluate(document)

        chain.forEach { id ->
            assertEquals(LineEvaluation.Failed(CalcError.CircularReference(chain)), evaluation[id])
        }
        assertEquals("7", (evaluation[document.idAtOrdinal(3)!!] as LineEvaluation.Value).value.canonicalString())
    }

    @Test
    fun `editing a line re-evaluates it and its dependents, and nothing else`() {
        val document = Documents.of("price = 100", "price * 2", "999", "line3 + 1")
        val counting = CountingEvaluator()
        val evaluator = NotepadEvaluator(evaluateAst = counting.evaluate)
        val first = evaluator.evaluate(document)
        counting.evaluations = 0

        val edited = document.edit(document.idAtOrdinal(1)!!, "price = 200")
        val second = evaluator.reevaluate(first, edited)

        // The definition and the line that reads it. Not the two lines that read neither.
        assertEquals(2, counting.evaluations)
        assertEquals(listOf("200", "400", "999", "1000"), second.canonicalValues())
    }

    @Test
    fun `editing a line nothing reads costs one evaluation`() {
        val document = Documents.of("x = 1", "x + 1", "50", "x * 3")
        val counting = CountingEvaluator()
        val evaluator = NotepadEvaluator(evaluateAst = counting.evaluate)
        val first = evaluator.evaluate(document)
        counting.evaluations = 0

        val edited = document.edit(document.idAtOrdinal(3)!!, "60")
        evaluator.reevaluate(first, edited)

        assertEquals(1, counting.evaluations)
    }

    @Test
    fun `re-evaluating an unchanged document evaluates nothing`() {
        val document = Documents.of("x = 1", "x + 1", "line1 * 2")
        val counting = CountingEvaluator()
        val evaluator = NotepadEvaluator(evaluateAst = counting.evaluate)
        val first = evaluator.evaluate(document)
        counting.evaluations = 0

        val second = evaluator.reevaluate(first, document)

        assertEquals(0, counting.evaluations)
        assertEquals(first.canonicalValues(), second.canonicalValues())
    }

    @Test
    fun `a keystroke on an ordinary line costs one parse per pass, not one per line`() {
        val document = Documents.of(*Array(LONG_DOCUMENT) { "${it + 1}" })
        val cache = ParseCache()
        val evaluator = NotepadEvaluator(cache = cache)
        val first = evaluator.evaluate(document)
        val before = cache.misses

        val edited = document.edit(document.idAtOrdinal(LONG_DOCUMENT)!!, "12345")
        evaluator.reevaluate(first, edited)

        // Two: the pass that looks for a definition, and the parse in the line's own scope.
        assertEquals(2, cache.misses - before)
    }

    @Test
    fun `a change reaching every line still costs one evaluation per line`() {
        // The worst case, and the one that has to stay linear: a chain where each line reads
        // the one above it, changed at the top.
        val document = Documents.of("x0 = 1", *Array(LONG_DOCUMENT - 1) { "x${it + 1} = x$it + 1" })
        val counting = CountingEvaluator()
        val evaluator = NotepadEvaluator(evaluateAst = counting.evaluate)
        val first = evaluator.evaluate(document)
        counting.evaluations = 0

        val edited = document.edit(document.idAtOrdinal(1)!!, "x0 = 2")
        val second = evaluator.reevaluate(first, edited)

        assertEquals(LONG_DOCUMENT, counting.evaluations)
        // The chain now starts at 2 and adds one per line, so the last line is one higher.
        assertEquals("${LONG_DOCUMENT + 1}", second.canonicalValues().last())
    }

    @Test
    fun `inserting a line above leaves the values below it alone`() {
        val document = Documents.of("x = 2", "x * 3", "line2 + 1")
        val evaluator = NotepadEvaluator()
        val first = evaluator.evaluate(document)

        val inserted = document.insertAt(0, "0")
        val second = evaluator.reevaluate(first, inserted)

        assertEquals(listOf("0", "2", "6", "7"), second.canonicalValues())
    }

    @Test
    fun `deleting a definition makes what read it wrong, never leaves the old value`() {
        val document = Documents.of("x = 5", "x * 2")
        val evaluator = NotepadEvaluator()
        val first = evaluator.evaluate(document)
        val reader = document.idAtOrdinal(2)!!

        val without = document.delete(document.idAtOrdinal(1)!!)
        val second = evaluator.reevaluate(first, without)

        val evaluation = second[reader]
        assertTrue(evaluation.toString(), evaluation is LineEvaluation.Failed)
        assertTrue(
            evaluation.toString(),
            (evaluation as LineEvaluation.Failed).error is CalcError.UnknownIdentifier,
        )
    }

    @Test
    fun `breaking a circle brings the lines in it back to life`() {
        val document = Documents.of("line2 + 1", "line1 + 1")
        val evaluator = NotepadEvaluator()
        val first = evaluator.evaluate(document)

        val fixed = document.setDisplayText(document.idAtOrdinal(2)!!, "10")
        val second = evaluator.reevaluate(first, fixed)

        assertEquals(listOf("11", "10"), second.canonicalValues())
        assertTrue(second.cycles.isEmpty())
    }

    @Test
    fun `an incremental answer is the same as a full one`() {
        // The property that matters most, and the one a cache is most likely to break.
        val document = Documents.of("a = 1", "b = a + 1", "line2 * 3", "b + line3", "a = 10", "a + 1")
        val evaluator = NotepadEvaluator()
        val first = evaluator.evaluate(document)

        val edited = document.edit(document.idAtOrdinal(1)!!, "a = 4")
        val incremental = evaluator.reevaluate(first, edited)

        assertEquals(NotepadEvaluator().evaluate(edited).canonicalValues(), incremental.canonicalValues())
    }

    @Test
    fun `a keystroke on a 200-line document stays far inside the budget`() {
        // Not the definition of done — that figure is 50 ms measured on an API 26 emulator,
        // and this runs on a build agent's JVM. What it protects is the shape of the cost: a
        // regression that reparsed the document per keystroke, or evaluated it quadratically,
        // would blow a ceiling this loose long before anyone reached for a device.
        val document = Documents.of("x0 = 1", *Array(LONG_DOCUMENT - 1) { "x${it + 1} = x$it + 1" })
        val evaluator = NotepadEvaluator()
        val top = document.idAtOrdinal(1)!!
        var evaluation = evaluator.evaluate(document)

        var fastest = Long.MAX_VALUE
        repeat(WARMUP_KEYSTROKES + MEASURED_KEYSTROKES) { keystroke ->
            val edited = document.edit(top, "x0 = $keystroke")
            val started = System.nanoTime()
            evaluation = evaluator.reevaluate(evaluation, edited)
            val elapsed = System.nanoTime() - started
            if (keystroke >= WARMUP_KEYSTROKES) fastest = minOf(fastest, elapsed)
        }

        val millis = fastest / NANOS_PER_MILLI.toDouble()
        assertTrue("$millis ms for a keystroke on $LONG_DOCUMENT lines", millis < CEILING_MILLIS)
    }

    private fun DocumentEvaluation.canonicalValues(): List<String> = results.values.map { evaluation ->
        when (evaluation) {
            is LineEvaluation.Value -> evaluation.value.canonicalString()
            is LineEvaluation.Blocked -> "blocked"
            is LineEvaluation.Failed -> "failed"
            LineEvaluation.Empty -> ""
        }
    }

    /** Counts how many lines were actually handed to the engine. */
    private class CountingEvaluator {
        var evaluations = 0

        val evaluate: (Ast, EvalContext) -> CalcResult<Quantity> = { ast, context ->
            evaluations++
            Evaluator(context).evaluate(ast)
        }
    }

    private companion object {
        /** Long enough that "one per line" and "one" are impossible to confuse. */
        const val LONG_DOCUMENT = 200

        const val WARMUP_KEYSTROKES = 50
        const val MEASURED_KEYSTROKES = 50
        const val NANOS_PER_MILLI = 1_000_000

        /**
         * Half the device budget of `docs/ARCHITECTURE.md` §9, on hardware that is much
         * faster than the device. Deliberately slack: a tight bound on a shared build agent
         * fails for reasons that have nothing to do with this code.
         */
        const val CEILING_MILLIS = 25.0
    }
}
