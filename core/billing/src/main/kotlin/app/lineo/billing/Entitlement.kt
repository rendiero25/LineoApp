package app.lineo.billing

/**
 * The tier of service the user is entitled to.
 */
enum class Entitlement {
    /** The default experience. */
    Free,

    /** Unlocks all Phase 2 and 3 features. */
    Premium
}
