package app.lineo.ui.theme

/**
 * Every surface in Lineo that has a fixed place in the token mapping of
 * `docs/CONVENTIONS.md` §10.
 *
 * A role exists so that no composable ever names a colour. A keypad key asks for
 * [Digit] or [Operator]; which token that resolves to is decided once, here, and changes
 * with the scheme — dynamic, seeded, or true black. `MaterialTheme.colorScheme.*` is read
 * in exactly one place, [RoleColors.of].
 *
 * The roles are closed on purpose: a new visual element means a new row in §10 first.
 */
enum class LineoRole {

    /** Digit key, `0`–`9` and the decimal separator. */
    Digit,

    /**
     * Operator key: `+ − × ÷`, and the bracket pair.
     *
     * Brackets are here rather than with the functions because of what they do to a
     * reading eye, not because of what they do to the parser: they change the shape of an
     * expression, which is the operators' job, while `sin` and `log` only name a value.
     */
    Operator,

    /** Equals key. The only key that uses the primary colour. */
    Equals,

    /** Clear and all-clear. */
    Clear,

    /** Function key: `sin`, `log`, unit names, and backspace. */
    Function,

    /**
     * The key that swaps one input surface for another — `Aa` on the keypad, `123` on the
     * accessory row.
     *
     * Its own role, and the only one painted from the tertiary family, because it is the
     * only key that does not enter anything. Everything else on the keypad changes the
     * expression; this changes what you are typing with. A third hue says that before the
     * label is read, which matters most for the user who presses it by accident.
     */
    InputSwitch,

    /** The expression editor's own background. */
    Editor,

    /** A computed result. Text only — it sits on the editor, so it has no container. */
    Result,

    /**
     * The rule drawn under the span an error points at. A shape, not text — it carries no
     * content colour, and it is measured against the editor surface behind it.
     */
    ErrorUnderline,

    /**
     * The error message itself. Separate from [ErrorUnderline] because it is text and needs
     * a container it can be read on: `onErrorContainer` on `error` measures 1.31:1 in the
     * dark scheme, which is not a message, it is a smudge.
     *
     * Never colour alone: §10 requires an icon and text as well.
     */
    ErrorMessage,

    /** Suggestion chip: an in-scope variable, a recent unit, a spelling fix. */
    SuggestionChip,

    /** Badge marking a currency rate as stale. */
    StaleRateBadge,
}
