package app.lineo.scientific

import app.lineo.engine.AngleMode
import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.function.FunctionRegistry
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The definition of done of P1-04: every function this module registers is callable as text.
 *
 * The tests go through `Engine.evaluate` with a registry built from the module, and never
 * call a `CalcFunction` directly — a function that works when invoked by hand but is not
 * reachable by name is exactly the failure `AGENTS.md` §1 rules out, and only this path
 * catches it.
 */
class ScientificFunctionsTest {

    @Test
    fun `every registered function is reachable by name`() {
        val registry = ModuleRegistry(setOf(ScientificModule()))
        val registered = ScientificModule().functions().flatMap { it.names }

        val unreachable = registered.filterNot { name ->
            registry.functionRegistry(Tier.FREE, Locale.US).find(name) != null
        }

        assertEquals(emptyList<String>(), unreachable)
    }

    @Test
    fun `none of these names is already a built-in`() {
        // A module function whose name is taken is dropped by the registry, silently and by
        // design. Registering one would leave a key on the keypad calling something else.
        val shadowed = ScientificModule().functions()
            .flatMap { it.names }
            .filter { FunctionRegistry.BUILTIN.find(it) != null }

        assertEquals(emptyList<String>(), shadowed)
    }

    @Test
    fun `sec is the reciprocal of the cosine`() {
        assertEquals("2", evaluate("sec(60)"))
    }

    @Test
    fun `csc is the reciprocal of the sine, under either spelling`() {
        assertEquals("2", evaluate("csc(30)"))
        assertEquals("2", evaluate("cosec(30)"))
    }

    @Test
    fun `cot at a quarter turn is zero rather than an error`() {
        // 1 / tan(90°) would report a domain error, because tan(90°) has none. cos over sin
        // is 0 / 1, which is the answer a table of cotangents gives.
        assertEquals("0", evaluate("cot(90)"))
        assertEquals("1", evaluate("cot(45)"))
    }

    @Test
    fun `a reciprocal at a pole names the function that has no value there`() {
        val error = errorOf("csc(0)")

        // Not DivisionByZero: the user wrote csc, not a division.
        assertEquals(CalcError.DomainError("csc", DomainReason.UNDEFINED, 0..5), error)
    }

    @Test
    fun `sec follows the angle mode like every other trigonometric function`() {
        assertEquals("1", evaluate("sec(0)", angleMode = AngleMode.RAD))
        // An angle that says what it is means it whatever the mode: docs/CONVENTIONS.md §4.
        assertEquals("2", evaluate("sec(60°)", angleMode = AngleMode.RAD))
    }

    @Test
    fun `sign is a number even when its argument is not`() {
        assertEquals("-1", evaluate("sign(-5 km)"))
        assertEquals("0", evaluate("sign(0)"))
        assertEquals("1", evaluate("signum(0.001)"))
    }

    @Test
    fun `trunc cuts towards zero where floor does not`() {
        assertEquals("-2", evaluate("trunc(-2.5)"))
        assertEquals("2", evaluate("trunc(2.9)"))
        assertEquals("-3", evaluate("floor(-2.5)"))
    }

    @Test
    fun `trunc keeps the unit it was given`() {
        assertEquals("5 km", evaluate("trunc(5.7 km)"))
    }

    @Test
    fun `min and max compare across units and answer in the unit that was written`() {
        assertEquals("900 m", evaluate("min(1 km, 900 m)"))
        assertEquals("1 km", evaluate("max(1 km, 900 m)"))
    }

    @Test
    fun `min over more than two arguments`() {
        assertEquals("-4", evaluate("min(3, 7, -4, 12)"))
        assertEquals("12", evaluate("max(3, 7, -4, 12)"))
    }

    @Test
    fun `comparing a length with a mass is the mismatch the minus sign would report`() {
        assertTrue(errorOf("min(1 km, 5 kg)") is CalcError.UnitMismatch)
    }

    @Test
    fun `hypot is the root of the sum of the squares`() {
        assertEquals("5", evaluate("hypot(3, 4)"))
    }

    @Test
    fun `hypot rejects a unit rather than dropping it`() {
        // sqrt takes plain numbers, and hypot reaches that rule rather than restating it.
        val error = errorOf("hypot(3 km, 4 km)")

        assertEquals(DomainReason.OUT_OF_RANGE, (error as CalcError.DomainError).reason)
    }

    @Test
    fun `an error from a module function points at the call, not at the module`() {
        val error = errorOf("1 + csc(0)") as CalcError.DomainError

        assertEquals(4..9, error.span)
    }

    @Test
    fun `these functions compose with the built-ins in one expression`() {
        assertEquals("3", evaluate("max(sqrt(9), trunc(2.5))"))
    }

    private fun evaluate(source: String, angleMode: AngleMode = AngleMode.DEG): String =
        when (val result = Engine.evaluate(source, contextOf(angleMode))) {
            is CalcResult.Ok -> result.value.canonicalString()
            is CalcResult.Err -> "error: ${result.error}"
        }

    private fun errorOf(source: String): CalcError =
        (Engine.evaluate(source, contextOf()) as CalcResult.Err).error

    private fun contextOf(angleMode: AngleMode = AngleMode.DEG) = EvalContext(
        locale = Locale.US,
        angleMode = angleMode,
        functions = ModuleRegistry(setOf(ScientificModule())).functionRegistry(Tier.FREE, Locale.US),
    )
}
