package app.lineo.converter

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lineo.data.settings.UnitSystem
import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.registry.EditorCommand
import app.lineo.ui.editor.EditedLine
import app.lineo.ui.editor.applying

/**
 * What the converter screen is: an amount, a unit to read it in, and a unit to read it as.
 *
 * **It does no arithmetic.** The conversion is `"<amount> <from> to <to>"` handed to the
 * engine, which is the same string a user could type into the notepad — the constraint
 * `docs/ARCHITECTURE.md` §4 puts on a module screen, and what keeps the two surfaces from
 * ever disagreeing. A rounding rule, a temperature offset or a new unit is fixed in one place
 * and both follow.
 *
 * The amount is an [EditedLine] rather than a plain string so that the keypad's commands —
 * backspace, `±`, `AC`, the decimal separator — all mean here what they mean everywhere else.
 *
 * @param evaluate seam for tests and the reason this class needs no `Context`. Defaults to
 *   the engine with the built-in units only, which is enough for nothing: the screen passes
 *   a context that carries this module's catalogue.
 */
@Stable
internal class ConverterState(
    category: ConverterCategory = ConverterCategory.LENGTH,
    private val unitSystem: UnitSystem = UnitSystem.METRIC,
    private val decimalSeparator: Char = '.',
    private val evaluate: (String) -> CalcResult<Quantity> = { Engine.evaluate(it, EvalContext()) },
) {

    var category: ConverterCategory by mutableStateOf(category)
        private set

    var from: ConverterUnit by mutableStateOf(category.defaultPair(unitSystem).first)
        private set

    var to: ConverterUnit by mutableStateOf(category.defaultPair(unitSystem).second)
        private set

    /** What the user has typed. Empty is a real state: the screen shows nothing rather than zero. */
    var amount: EditedLine by mutableStateOf(EditedLine())
        private set

    /**
     * The expression the engine is given.
     *
     * Exactly what a user could type into a notepad line, `to in` included: P1-05-0b made the
     * parser read `in` after a conversion keyword as the inch, since a target cannot itself
     * be a conversion (`docs/GRAMMAR.md` §3.3).
     */
    val expression: String
        get() = "${amount.text.trim()} ${from.expression} to ${to.expression}"

    /**
     * The converted amount, or `null` while there is nothing to convert.
     *
     * An error is `null` too, deliberately: on this screen the only way to reach one is a
     * half-typed number, and `5.` is not a mistake to report — it is a number the user has
     * not finished. The notepad, where an expression can be genuinely wrong, says so instead.
     */
    val result: Quantity?
        get() = if (amount.text.isBlank()) null else (evaluate(expression) as? CalcResult.Ok)?.value

    fun apply(command: EditorCommand) {
        amount = amount.applying(command, decimalSeparator)
    }

    /**
     * Switching category takes that category's starting pair: the old one belongs to another
     * dimension, and which pair is the starting one depends on how the user writes units.
     */
    fun select(category: ConverterCategory) {
        this.category = category
        val (start, target) = category.defaultPair(unitSystem)
        from = start
        to = target
    }

    fun selectFrom(unit: ConverterUnit) {
        from = unit
    }

    fun selectTo(unit: ConverterUnit) {
        to = unit
    }

    /** The `⇅` button: read the same amount the other way round. */
    fun swap() {
        val previous = from
        from = to
        to = previous
    }
}
