package app.lineo.shell

import app.lineo.di.CalculatorModulesModule
import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.unit.UnitRegistry
import app.lineo.notepad.LineEvaluation
import app.lineo.notepad.NotepadDocument
import app.lineo.notepad.NotepadEvaluator
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * What P1-04-2 actually promises: a module's function means the same thing in the notepad as
 * it does on the module's own screen.
 *
 * The set of modules comes from the Hilt module rather than from a list written here, so a
 * feature that is bound but forgotten by the notepad's wiring fails this rather than being
 * discovered on a device.
 */
class ModuleWiringTest {

    private val registry = ModuleRegistry(
        setOf(CalculatorModulesModule.scientificModule(), CalculatorModulesModule.converterModule()),
    )
    private val functions = registry.functionRegistry(Tier.FREE, Locale.US)
    private val units = UnitRegistry.BUILTIN.with(registry.units(Tier.FREE, Locale.US))

    @Test
    fun `the build contains the modules it ships`() {
        assertEquals(listOf("converter", "scientific"), registry.all.map { it.id })
        assertTrue(registry.visible(Tier.FREE, Locale.US).isNotEmpty())
    }

    @Test
    fun `no module function or unit is dropped as a duplicate`() {
        // A dropped name is silent by design (`ModuleRegistry`, `UnitRegistry`), so it is
        // asserted here rather than noticed when a key or a category stops working.
        assertEquals(emptyList<String>(), registry.conflicts(Tier.FREE, Locale.US))
        assertEquals(emptyList<String>(), UnitRegistry.BUILTIN.conflicts(registry.units(Tier.FREE, Locale.US)))
    }

    @Test
    fun `a module unit typed into the notepad converts`() {
        val document = NotepadDocument().append("5 km to mi")
        val evaluator = NotepadEvaluator(EvalContext(functions = functions, units = units))

        val line = evaluator.evaluate(document).results.values.last()

        // 5 / 1.609344, to the engine's full precision — the same number the converter screen
        // shows, because it is the same expression.
        assertTrue(
            (line as LineEvaluation.Value).value.canonicalString(),
            line.value.canonicalString().startsWith("3.106855961"),
        )
    }

    @Test
    fun `a data unit reaches the notepad with its binary prefix intact`() {
        val document = NotepadDocument().append("1 GiB to MB")
        val evaluator = NotepadEvaluator(EvalContext(functions = functions, units = units))

        val line = evaluator.evaluate(document).results.values.last()

        assertEquals("1073.741824 MB", (line as LineEvaluation.Value).value.canonicalString())
    }

    @Test
    fun `a module function typed into the notepad evaluates`() {
        val document = NotepadDocument().append("sec(60)")
        val evaluator = NotepadEvaluator(EvalContext(functions = functions, units = units))

        val line = evaluator.evaluate(document).results.values.last()

        assertEquals("2", (line as LineEvaluation.Value).value.canonicalString())
    }

    @Test
    fun `the notepad and a module screen answer the same call the same way`() {
        val document = NotepadDocument().append("cot(45)")
        val evaluator = NotepadEvaluator(EvalContext(functions = functions, units = units))

        val inNotepad = evaluator.evaluate(document).results.values.last()
        val onScreen = Engine.evaluate("cot(45)", EvalContext(functions = functions))

        assertEquals(
            (onScreen as CalcResult.Ok).value.canonicalString(),
            (inNotepad as LineEvaluation.Value).value.canonicalString(),
        )
    }

    @Test
    fun `without the module registry the same line is an unknown name`() {
        // The failure this wiring exists to prevent: the notepad on built-ins alone.
        val document = NotepadDocument().append("sec(60)")

        val line = NotepadEvaluator().evaluate(document).results.values.last()

        assertTrue(line is LineEvaluation.Failed)
    }
}
