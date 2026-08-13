package app.lineo.engine.unit

import java.math.BigDecimal

/**
 * How a unit symbol relates to its base dimension.
 *
 * A linear unit converts with [scale] alone: `value_in_base = value × scale`. Temperatures
 * are affine and also carry an [offset] (`docs/GRAMMAR.md` §3.8).
 */
data class UnitDefinition(
    val symbol: String,
    val dimensions: Dimensions,
    val scale: BigDecimal,
    val kind: UnitKind = UnitKind.LINEAR,
    val offset: BigDecimal = BigDecimal.ZERO,
    val aliases: List<String> = emptyList(),
) {
    val isAbsoluteTemperature: Boolean get() = kind == UnitKind.ABSOLUTE_TEMPERATURE
}

/**
 * Absolute temperatures are a distinct kind because they do not behave like the others:
 * adding two of them is meaningless, while adding a delta to one is not.
 */
enum class UnitKind {
    LINEAR,
    ABSOLUTE_TEMPERATURE,
    TEMPERATURE_DELTA,
}
