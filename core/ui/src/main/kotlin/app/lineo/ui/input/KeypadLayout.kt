package app.lineo.ui.input

/**
 * Which grid a [KeypadState] shows.
 *
 * The keypad had exactly one layout while there was exactly one surface. A focused module
 * needs its own — `:feature:scientific` puts trigonometry and logarithms above the same
 * digits — and the alternative was a flag per module inside `:core:ui`, which would make
 * every new module an edit to a `:core:*` file. The layout is data the caller supplies
 * instead, so a module owns its keys and `:core:ui` owns how a key is drawn.
 *
 * A layout is a pure function of what can change under it: the decimal separator, which
 * `docs/CONVENTIONS.md` §1 puts at the display boundary, and whether the window is wide
 * enough for more columns. It is never told the orientation or the device
 * (`docs/ANDROID_STANDARDS.md` §2).
 */
interface KeypadLayout {

    /** The grid, top row first. Rows may differ in length; [Keypad] lays each one out on its own. */
    fun rows(
        decimalSeparator: Char,
        hasExtraColumn: Boolean,
        hasRoomForFunctions: Boolean,
    ): List<List<KeypadKey>>

    companion object {

        /** The calculator grid of P0-13: four columns, five when there is room. */
        val Basic: KeypadLayout = object : KeypadLayout {
            override fun rows(
                decimalSeparator: Char,
                hasExtraColumn: Boolean,
                hasRoomForFunctions: Boolean,
            ): List<List<KeypadKey>> = basicKeypadRows(decimalSeparator, hasExtraColumn)
        }
    }
}
