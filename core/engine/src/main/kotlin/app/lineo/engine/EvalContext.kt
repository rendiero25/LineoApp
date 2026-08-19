package app.lineo.engine

import app.lineo.engine.function.FunctionRegistry
import app.lineo.engine.unit.UnitRegistry
import java.util.Locale

/** Angle mode. DEG by default and always visible in the UI (`docs/CONVENTIONS.md` §4). */
enum class AngleMode {
    DEG,
    RAD,
    GRAD,
}

/**
 * Everything an evaluation needs beyond the expression itself: the locale used to read
 * numbers, the angle mode, the variables defined earlier in the document, and the results
 * of previous lines, and the function registry (`docs/ARCHITECTURE.md` §2).
 */
data class EvalContext(
    val locale: Locale = Locale.US,
    val angleMode: AngleMode = AngleMode.DEG,
    val variables: Map<String, Quantity> = emptyMap(),
    val lineResults: Map<LineId, Quantity> = emptyMap(),
    val functions: FunctionRegistry = FunctionRegistry.BUILTIN,
    val units: UnitRegistry = UnitRegistry.BUILTIN,
)
