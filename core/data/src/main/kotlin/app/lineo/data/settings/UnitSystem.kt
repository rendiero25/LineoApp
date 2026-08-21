package app.lineo.data.settings

/**
 * Which family of units a screen offers first (`TASKS.md` P1-07).
 *
 * It changes what is *offered*, never what is stored or computed: a quantity is a quantity,
 * and `docs/CONVENTIONS.md` §1 keeps the engine free of any such preference. The converter
 * reads it to pick the pair a fresh category starts on.
 */
enum class UnitSystem {

    /** Follow the locale: metric everywhere except the handful of places that are not. The default. */
    AUTO,

    METRIC,

    IMPERIAL,
}
