package app.lineo.engine.function

import app.lineo.engine.AngleMode
import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * What the golden files cannot state on a single line: the shape of the registry itself,
 * and that a module's function is registered exactly the way a built-in is
 * (`docs/ARCHITECTURE.md` §4).
 */
class BuiltinFunctionsTest {

    @Test
    fun `every name and alias is registered once`() {
        val names = BuiltinFunctions.ALL.flatMap { it.names }
        val duplicates = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        assertEquals(emptySet<String>(), duplicates, "a shadowed name silently loses a function")
    }

    @Test
    fun `every registered name resolves back to its function`() {
        val registry = FunctionRegistry.BUILTIN

        BuiltinFunctions.ALL.forEach { function ->
            function.names.forEach { name ->
                assertEquals(function, registry.find(name), "$name does not resolve")
            }
        }
    }

    @Test
    fun `no built-in is named after a reserved word`() {
        val reserved = setOf("to", "in", "as", "of", "mod", "and", "or", "not", "true", "false", "line", "if", "else")
        val clashing = BuiltinFunctions.ALL.flatMap { it.names }.filter { it in reserved }

        assertEquals(emptyList<String>(), clashing, "docs/GRAMMAR.md §4 reserves these")
    }

    @Test
    fun `built-ins are available without configuring a context`() {
        val result = Engine.evaluate("sin(90)")

        assertEquals("1", (result as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `a module function is registered the same way a built-in is`() {
        val extra = EngineFunction("double", 1..1) { arguments, _, _ ->
            Quantity(arguments[0].value.multiply(BigDecimal(2))).ok()
        }
        val context = EvalContext(functions = FunctionRegistry.BUILTIN.with(listOf(extra)))

        val result = Engine.evaluate("double(21) + sin(90)", context)

        assertEquals("43", (result as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `a call with the wrong number of arguments is a domain error`() {
        val result = Engine.evaluate("sin(1, 2)")

        val error = (result as CalcResult.Err).error
        assertTrue(error is CalcError.DomainError, error.toString())
        assertEquals("sin", (error as CalcError.DomainError).fn)
    }

    @Test
    fun `a misspelt function suggests the nearest built-in`() {
        val result = Engine.evaluate("sni(1)")

        val error = (result as CalcResult.Err).error as CalcError.UnknownIdentifier
        assertEquals("sin", error.suggestion)
        assertEquals(0..2, error.span)
    }

    @Test
    fun `the angle mode decides how a bare number is read`() {
        val degrees = Engine.evaluate("sin(100)", EvalContext(angleMode = AngleMode.DEG))
        val gradians = Engine.evaluate("sin(100)", EvalContext(angleMode = AngleMode.GRAD))

        assertEquals("0.984807753012208", (degrees as CalcResult.Ok).value.canonicalString())
        assertEquals("1", (gradians as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `an angle unit wins over the angle mode`() {
        AngleMode.entries.forEach { mode ->
            val result = Engine.evaluate("sin(90°)", EvalContext(angleMode = mode))

            assertEquals("1", (result as CalcResult.Ok).value.canonicalString(), "in $mode")
        }
    }

    @Test
    fun `the postfix operator and the function agree`() {
        val operator = Engine.evaluate("6!")
        val function = Engine.evaluate("fact(6)")

        assertEquals("720", (operator as CalcResult.Ok).value.canonicalString())
        assertEquals(operator.value, (function as CalcResult.Ok).value)
    }
}
