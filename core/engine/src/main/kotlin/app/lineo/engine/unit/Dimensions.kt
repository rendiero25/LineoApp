package app.lineo.engine.unit

/**
 * Exponents of the base dimensions (`docs/ARCHITECTURE.md` §2).
 *
 * Currency is a dimension like any other; its scale factor is what varies at runtime,
 * sourced from the rate cache. Information is one too — see [Dimensions.INFORMATION].
 */
data class Dimensions(
    val length: Int = 0,
    val mass: Int = 0,
    val time: Int = 0,
    val current: Int = 0,
    val temperature: Int = 0,
    val amount: Int = 0,
    val luminosity: Int = 0,
    val currency: Int = 0,
    val information: Int = 0,
) {
    val isDimensionless: Boolean get() = this == NONE

    operator fun plus(other: Dimensions): Dimensions = Dimensions(
        length = length + other.length,
        mass = mass + other.mass,
        time = time + other.time,
        current = current + other.current,
        temperature = temperature + other.temperature,
        amount = amount + other.amount,
        luminosity = luminosity + other.luminosity,
        currency = currency + other.currency,
        information = information + other.information,
    )

    operator fun minus(other: Dimensions): Dimensions = this + (-other)

    operator fun unaryMinus(): Dimensions = this * -1

    operator fun times(factor: Int): Dimensions = Dimensions(
        length = length * factor,
        mass = mass * factor,
        time = time * factor,
        current = current * factor,
        temperature = temperature * factor,
        amount = amount * factor,
        luminosity = luminosity * factor,
        currency = currency * factor,
        information = information * factor,
    )

    companion object {
        val NONE = Dimensions()
        val LENGTH = Dimensions(length = 1)
        val MASS = Dimensions(mass = 1)
        val TIME = Dimensions(time = 1)
        val CURRENT = Dimensions(current = 1)
        val TEMPERATURE = Dimensions(temperature = 1)
        val AMOUNT = Dimensions(amount = 1)
        val LUMINOSITY = Dimensions(luminosity = 1)
        val CURRENCY = Dimensions(currency = 1)

        /**
         * Information, for the data category of `:feature:converter`.
         *
         * Not an SI base quantity, and here for the same reason [CURRENCY] is: without it a
         * byte would be a plain number, `5 KiB + 3` would be accepted, and `10 MB / 2 s`
         * would not come out as a rate. ISO 80000-13 treats information as a quantity of its
         * own, which is exactly what this says.
         */
        val INFORMATION = Dimensions(information = 1)
    }
}
