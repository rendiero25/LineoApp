package app.lineo.data.settings

/**
 * Which decimal separator to read and render (`docs/CONVENTIONS.md` §2).
 *
 * The override exists because a phone's locale is often not how its owner writes numbers —
 * an Indonesian phone set to `en-US` still belongs to someone who writes `1,5`. Choosing
 * the decimal separator also chooses the argument separator, since the two cannot be the
 * same character.
 */
enum class SeparatorPreference {
    /** Take the separator from the active locale. The default. */
    AUTO,

    /** `1.5`, arguments separated by `,`. */
    DOT,

    /** `1,5`, arguments separated by `;`. */
    COMMA,
}
