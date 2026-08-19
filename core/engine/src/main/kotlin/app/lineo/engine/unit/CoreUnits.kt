package app.lineo.engine.unit

import java.math.BigDecimal
import java.math.MathContext

/**
 * The unit definitions `UnitRegistry.BUILTIN` is made of.
 *
 * A file of data, kept apart from the registry that indexes it so that adding a symbol is
 * never an edit to lookup code. What belongs here is what the *grammar* refers to: angles,
 * because trigonometry reads them (`docs/GRAMMAR.md` §3.8); temperatures, because they are
 * affine and the arithmetic rules single them out; and the everyday length, mass and time
 * symbols an expression is expected to understand with no module installed.
 *
 * Everything else — area, volume, speed, pressure, energy, data — is `:feature:converter`'s,
 * registered through `CalculatorModule.units()`.
 */
internal object CoreUnits {

    /** Declared before [ALL], which reads it: an object initialises in source order. */
    private val PI = BigDecimal("3.141592653589793238462643383279503")

    val ALL: List<UnitDefinition> = buildList {
        // Length, base metre.
        add(linear("m", Dimensions.LENGTH, "1"))
        add(linear("km", Dimensions.LENGTH, "1000"))
        add(linear("cm", Dimensions.LENGTH, "0.01"))
        add(linear("mm", Dimensions.LENGTH, "0.001"))
        add(linear("mi", Dimensions.LENGTH, "1609.344"))
        add(linear("yd", Dimensions.LENGTH, "0.9144"))
        add(linear("ft", Dimensions.LENGTH, "0.3048"))
        add(linear("in", Dimensions.LENGTH, "0.0254"))

        // Mass, base kilogram.
        add(linear("kg", Dimensions.MASS, "1"))
        add(linear("g", Dimensions.MASS, "0.001"))
        add(linear("mg", Dimensions.MASS, "0.000001"))
        add(linear("t", Dimensions.MASS, "1000"))
        add(linear("lb", Dimensions.MASS, "0.45359237"))
        add(linear("oz", Dimensions.MASS, "0.028349523125"))

        // Time, base second.
        add(linear("s", Dimensions.TIME, "1"))
        add(linear("ms", Dimensions.TIME, "0.001"))
        add(linear("min", Dimensions.TIME, "60"))
        add(linear("h", Dimensions.TIME, "3600"))
        add(linear("d", Dimensions.TIME, "86400"))

        // Temperature, base kelvin. °C and °F are affine (docs/GRAMMAR.md §3.8).
        add(linear("K", Dimensions.TEMPERATURE, "1", kind = UnitKind.ABSOLUTE_TEMPERATURE))
        add(
            UnitDefinition(
                symbol = "°C",
                dimensions = Dimensions.TEMPERATURE,
                scale = BigDecimal.ONE,
                kind = UnitKind.ABSOLUTE_TEMPERATURE,
                offset = BigDecimal("273.15"),
                aliases = listOf("degC"),
            ),
        )
        add(
            UnitDefinition(
                symbol = "°F",
                dimensions = Dimensions.TEMPERATURE,
                scale = BigDecimal("5").divide(BigDecimal("9"), MathContext.DECIMAL128),
                kind = UnitKind.ABSOLUTE_TEMPERATURE,
                offset = BigDecimal("459.67")
                    .multiply(BigDecimal("5"))
                    .divide(BigDecimal("9"), MathContext.DECIMAL128),
                aliases = listOf("degF"),
            ),
        )

        // Plane angle. Dimensionless per ISO 80000, but the scale is what lets trigonometry
        // honour `sin 90°` regardless of the angle mode.
        add(linear("rad", Dimensions.NONE, "1", kind = UnitKind.ANGLE))
        add(
            UnitDefinition(
                symbol = "°",
                dimensions = Dimensions.NONE,
                scale = PI.divide(BigDecimal("180"), MathContext.DECIMAL128),
                kind = UnitKind.ANGLE,
                aliases = listOf("deg"),
            ),
        )
        add(
            UnitDefinition(
                symbol = "grad",
                dimensions = Dimensions.NONE,
                scale = PI.divide(BigDecimal("200"), MathContext.DECIMAL128),
                kind = UnitKind.ANGLE,
            ),
        )

        // Temperature differences. Adding one of these to an absolute temperature is legal.
        add(delta("Δ°C", BigDecimal.ONE))
        add(delta("ΔK", BigDecimal.ONE))
        add(delta("Δ°F", BigDecimal("5").divide(BigDecimal("9"), MathContext.DECIMAL128)))
    }

    private fun linear(
        symbol: String,
        dimensions: Dimensions,
        scale: String,
        kind: UnitKind = UnitKind.LINEAR,
    ): UnitDefinition = UnitDefinition(symbol, dimensions, BigDecimal(scale), kind)

    private fun delta(symbol: String, scale: BigDecimal): UnitDefinition =
        UnitDefinition(symbol, Dimensions.TEMPERATURE, scale, UnitKind.TEMPERATURE_DELTA)
}
