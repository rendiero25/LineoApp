package app.lineo.engine.eval

import app.lineo.engine.Quantity
import java.math.BigDecimal

/**
 * Built-in constants (`docs/GRAMMAR.md` §4). A user variable may shadow one; the editor
 * warns rather than the engine refusing.
 *
 * Values carry more digits than `MathContext.DECIMAL128` keeps, so rounding happens once,
 * in the arithmetic, rather than being baked into the constant.
 */
object Constants {
    private val PI = BigDecimal("3.14159265358979323846264338327950288419716939937510")
    private val E = BigDecimal("2.71828182845904523536028747135266249775724709369995")
    private val PHI = BigDecimal("1.61803398874989484820458683436563811772030917980576")

    private val values: Map<String, BigDecimal> = mapOf(
        "pi" to PI,
        "π" to PI,
        "e" to E,
        "phi" to PHI,
        "φ" to PHI,
    )

    val names: Set<String> get() = values.keys

    fun find(name: String): Quantity? = values[name]?.let { Quantity(it) }
}
