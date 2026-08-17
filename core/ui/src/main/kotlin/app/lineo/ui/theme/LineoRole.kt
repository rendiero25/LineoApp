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

    /** Operator key: `+ − × ÷`. */
    Operator,

    /** Equals key. The only key that uses the primary colour. */
    Equals,

    /** Clear and all-clear. */
    Clear,

    /** Function key: `sin`, `log`, unit names. */
    Function,

    /** The expression editor's own background. */
    Editor,

    /** A computed result. Text only — it sits on the editor, so it has no container. */
    Result,

    /** Error underline and message. Never colour alone: §10 requires an icon and text. */
    Error,

    /** Suggestion chip: an in-scope variable, a recent unit, a spelling fix. */
    SuggestionChip,

    /** Badge marking a currency rate as stale. */
    StaleRateBadge,
}
