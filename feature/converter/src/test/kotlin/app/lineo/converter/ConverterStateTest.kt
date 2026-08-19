package app.lineo.converter

import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.unit.UnitRegistry
import app.lineo.registry.EditorCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/**
 * The screen's model: what it asks the engine, and what it does with the answer.
 *
 * The assertions are on [ConverterState.expression] as much as on the result, because the
 * expression *is* the contract — a screen that computed anything itself would be unreachable
 * from notepad mode, which `docs/ARCHITECTURE.md` §4 rules out.
 */
class ConverterStateTest {

    private fun state(
        category: ConverterCategory = ConverterCategory.LENGTH,
        decimalSeparator: Char = '.',
    ) = ConverterState(
        category = category,
        decimalSeparator = decimalSeparator,
        evaluate = { source ->
            Engine.evaluate(
                source,
                EvalContext(locale = Locale.US, units = UnitRegistry.BUILTIN.with(ConverterUnits.ALL)),
            )
        },
    )

    private fun ConverterState.type(digits: String) = digits.forEach { apply(EditorCommand.InsertText(it.toString())) }

    @Test
    fun `the expression is what a user could type`() {
        val state = state()
        state.type("1.5")
        state.selectFrom(ConverterUnit("km"))
        state.selectTo(ConverterUnit("mi"))

        assertEquals("1.5 km to mi", state.expression)
        assertEquals("0.9320567883560009544261512765449773", state.result?.value?.toPlainString())
    }

    @Test
    fun `nothing is converted until something is typed`() {
        val state = state()

        assertNull(state.result)

        state.type("2")

        assertEquals("2 m to km", state.expression)
    }

    @Test
    fun `the keypad edits the amount the same way it edits a line`() {
        val state = state()
        state.type("50")
        state.apply(EditorCommand.Backspace)
        state.apply(EditorCommand.ToggleSign)

        assertEquals("-5", state.amount.text)

        state.apply(EditorCommand.ClearLine)

        assertEquals("", state.amount.text)
        assertNull(state.result)
    }

    @Test
    fun `a comma decimal separator is read as a decimal separator`() {
        val state = state(decimalSeparator = ',')
        state.type("2,5")
        state.apply(EditorCommand.ToggleSign)

        // The sign flips the whole number rather than stopping at the comma, which is what
        // `EditedLine` already guarantees — this asserts the converter uses it.
        assertEquals("-2,5", state.amount.text)
    }

    @Test
    fun `switching category takes the units of the new one`() {
        val state = state()
        state.selectTo(ConverterUnit("mi"))

        state.select(ConverterCategory.DATA)

        assertEquals("B", state.from.label)
        assertEquals("kB", state.to.label)
    }

    @Test
    fun `swapping reads the same amount the other way`() {
        val state = state()
        state.type("1")
        state.selectTo(ConverterUnit("cm"))

        state.swap()

        assertEquals("1 cm to m", state.expression)
        assertEquals("0.01", state.result?.value?.stripTrailingZeros()?.toPlainString())
    }

    @Test
    fun `an area converts through the power the label hides`() {
        val state = state(ConverterCategory.AREA)
        state.type("2")
        state.selectTo(ConverterUnit("ha"))

        assertEquals("2 m^2 to ha", state.expression)
        assertEquals("0.0002", state.result?.value?.stripTrailingZeros()?.toPlainString())
    }

    @Test
    fun `a half-typed number shows nothing rather than an error`() {
        // `5.` is not a mistake, it is a number the user has not finished. The notepad, where
        // an expression can be genuinely wrong, is where errors belong.
        val state = state()
        state.type("5.")

        assertNull(state.result)
    }

    @Test
    fun `temperature keeps its offset, so the conversion is not a scale`() {
        val state = state(ConverterCategory.TEMPERATURE)
        state.type("100")
        state.selectTo(ConverterUnit("°F"))

        assertEquals("100 °C to °F", state.expression)
        assertEquals("212", state.result?.value?.stripTrailingZeros()?.toPlainString())
    }

    @Test
    fun `inches are reachable as a target`() {
        val state = state()
        state.type("5")
        state.selectFrom(ConverterUnit("cm"))
        state.selectTo(ConverterUnit("in"))

        assertEquals("5 cm to in", state.expression)
        assertEquals("1.968503937007874015748031496062992", state.result?.value?.toPlainString())
    }
}
