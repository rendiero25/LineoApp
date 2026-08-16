package app.lineo.registry

import java.util.Locale

/**
 * Which locales a module is offered in (`docs/ARCHITECTURE.md` §4).
 *
 * Regional tax packs are the reason this exists: `:pack:tax-id` is meaningless outside
 * Indonesia, and shipping it to everyone would clutter the module list.
 */
sealed interface LocaleGate {

    fun allows(locale: Locale): Boolean

    /** Offered everywhere. What a general-purpose module uses. */
    data object All : LocaleGate {
        override fun allows(locale: Locale): Boolean = true
    }

    /**
     * Offered only for the given BCP-47 tags.
     *
     * A tag matches when it equals the locale's own tag or is a prefix of it at a tag
     * boundary, so `id` matches `id-ID` while `in` does not match `india`. Matching is
     * case-insensitive; `Locale.ROOT` is used for the folding so the device locale cannot
     * change the outcome (the Turkish dotless-i problem, `docs/CONVENTIONS.md` §1).
     */
    data class Only(val tags: Set<String>) : LocaleGate {
        override fun allows(locale: Locale): Boolean {
            val candidate = locale.toLanguageTag().lowercase(Locale.ROOT)
            return tags.any { tag ->
                val wanted = tag.lowercase(Locale.ROOT)
                candidate == wanted || candidate.startsWith("$wanted-")
            }
        }
    }
}
