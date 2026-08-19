package app.lineo.scientific

import app.lineo.registry.EditorCommand
import app.lineo.ui.input.KeypadKey
import app.lineo.ui.input.KeypadLayout
import app.lineo.ui.input.basicKeypadRows
import app.lineo.ui.theme.LineoRole

/**
 * The scientific grid: function rows above the calculator keypad everything else uses.
 *
 * Above, and not instead of. A user who has learned where `7` and `÷` are on the notepad
 * keypad finds them in the same place here, because the digits come from
 * [basicKeypadRows] rather than from a second copy of them. This module owns which
 * functions are offered and nothing about how a key is drawn.
 *
 * **Width decides how many rows there are, never orientation.** P1-04 was written as
 * "landscape expands to the full scientific keypad", and `docs/ANDROID_STANDARDS.md` §2
 * forbids branching on orientation or device type — a tablet held upright and an unfolded
 * foldable have the room too, and a phone in landscape has it for the same reason they do.
 * The narrow set is the one that has to earn each key.
 *
 * `π` has a key because no software keyboard offers the glyph; `e` does not, because every
 * keyboard has the letter and `docs/GRAMMAR.md` reads it as the constant. `√` and `^` appear
 * in the narrow set only, since the wide layout already carries them in the fifth column
 * `basicKeypadRows` adds.
 */
internal object ScientificKeypadLayout : KeypadLayout {

    override fun rows(decimalSeparator: Char, hasRoomForFunctions: Boolean): List<List<KeypadKey>> =
        if (hasRoomForFunctions) {
            WIDE_ROWS + basicKeypadRows(decimalSeparator, hasRoomForFunctions = true)
        } else {
            NARROW_ROWS + basicKeypadRows(decimalSeparator, hasRoomForFunctions = false)
        }

    /**
     * Four columns, one row.
     *
     * One, and not the two this started as. A key is square and takes a quarter of the
     * width, so every extra row costs a quarter of the screen's width in height — two of
     * them left a compact phone with a document pane too short to draw the expression in,
     * found on a device and invisible to every snapshot. The narrow set is one row because
     * that is what a compact window can pay for.
     *
     * These four and not others: `√` and `^` are already reachable from the accessory row
     * above, and the trigonometry and the natural logarithm are what a phone-sized
     * scientific keypad is for.
     */
    private val NARROW_ROWS: List<List<KeypadKey>> = listOf(
        listOf(
            function("sin", R.string.key_sine_description),
            function("cos", R.string.key_cosine_description),
            function("tan", R.string.key_tangent_description),
            function("ln", R.string.key_natural_log_description),
        ),
    )

    /** Five columns, three rows: the inverses, the other bases, and the reciprocals this module adds. */
    private val WIDE_ROWS: List<List<KeypadKey>> = listOf(
        listOf(
            function("sin", R.string.key_sine_description),
            function("cos", R.string.key_cosine_description),
            function("tan", R.string.key_tangent_description),
            function("ln", R.string.key_natural_log_description),
            function("log", R.string.key_log_description),
        ),
        listOf(
            function("asin", R.string.key_arcsine_description, label = "sin⁻¹"),
            function("acos", R.string.key_arccosine_description, label = "cos⁻¹"),
            function("atan", R.string.key_arctangent_description, label = "tan⁻¹"),
            function("log2", R.string.key_log2_description, label = "log₂"),
            function("exp", R.string.key_exponential_description, label = "eˣ"),
        ),
        listOf(
            function("sec", R.string.key_secant_description),
            function("csc", R.string.key_cosecant_description),
            function("cot", R.string.key_cotangent_description),
            function("fact", R.string.key_factorial_description, label = "n!"),
            KeypadKey("π", LineoRole.Function, EditorCommand.InsertText("π"), R.string.key_pi_description),
        ),
    )

    /**
     * A key that types a call.
     *
     * `InsertFunction` and not `InsertText`, so the caret lands inside the brackets and a
     * two-argument function arrives with its separator already typed — which separator that
     * is depends on the decimal separator, and only the editor knows it.
     */
    private fun function(name: String, description: Int, label: String = name): KeypadKey = KeypadKey(
        label = label,
        role = LineoRole.Function,
        command = EditorCommand.InsertFunction(name, arity = 1),
        contentDescription = description,
    )
}
