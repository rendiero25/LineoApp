package app.lineo.converter

import app.lineo.engine.unit.Dimensions
import app.lineo.engine.unit.UnitDefinition
import java.math.BigDecimal

/**
 * The unit symbols this module contributes to the engine (`docs/ARCHITECTURE.md` §4).
 *
 * Only the gap: `:core:engine` already ships the everyday length, mass and time symbols and
 * the temperatures, and `UnitRegistry` drops a definition whose symbol is taken — so
 * registering `m` again would be dead code pretending to be a contribution, exactly as
 * `:feature:scientific` found for `sin`.
 *
 * Symbols are ISO 80000 / SI where ISO has one, and the customary symbol where it does not
 * (`acre`, `psi`, `BTU`). Every scale is exact except where the definition itself is not:
 * `cal`, `BTU` and `eV` carry the values their standards fix.
 *
 * Nothing here is a power or a quotient of other units — `m²` and `km/h` are expressions the
 * parser already understands, and inventing `m2` as a symbol would be neither ISO nor
 * lexable. See [ConverterUnit].
 */
internal object ConverterUnits {

    private val MATH = java.math.MathContext.DECIMAL128

    // Derived dimensions, declared before [ALL] reads them: an object initialises in source order.
    private val VOLUME = Dimensions(length = 3)
    private val AREA = Dimensions(length = 2)
    private val SPEED = Dimensions(length = 1, time = -1)
    private val PRESSURE = Dimensions(mass = 1, length = -1, time = -2)
    private val ENERGY = Dimensions(mass = 1, length = 2, time = -2)

    /** Metres per unit, seconds per unit, and so on: the scale is always to the SI base. */
    val ALL: List<UnitDefinition> = buildList {
        addAll(length())
        addAll(mass())
        addAll(volume())
        addAll(area())
        addAll(speed())
        addAll(time())
        addAll(data())
        addAll(pressure())
        addAll(energy())
    }

    private fun length(): List<UnitDefinition> = listOf(
        linear("µm", Dimensions.LENGTH, "0.000001", aliases = listOf("um")),
        linear("nm", Dimensions.LENGTH, "0.000000001"),
        // The international nautical mile, exactly 1852 m since 1929.
        linear("nmi", Dimensions.LENGTH, "1852"),
    )

    private fun mass(): List<UnitDefinition> = listOf(
        linear("st", Dimensions.MASS, "6.35029318"),
        // The metric carat, fixed at 200 mg.
        linear("ct", Dimensions.MASS, "0.0002"),
    )

    /**
     * Volume, base cubic metre. The US customary gallon and its divisions, since that is what
     * `gal` means in the market this ships to first; the imperial gallon is 4.54609 L and is
     * a different unit, not a different spelling.
     */
    private fun volume(): List<UnitDefinition> = listOf(
        linear("L", VOLUME, "0.001", aliases = listOf("l")),
        linear("mL", VOLUME, "0.000001", aliases = listOf("ml")),
        linear("cL", VOLUME, "0.00001", aliases = listOf("cl")),
        linear("dL", VOLUME, "0.0001", aliases = listOf("dl")),
        linear("gal", VOLUME, "0.003785411784"),
        linear("qt", VOLUME, "0.000946352946"),
        linear("pt", VOLUME, "0.000473176473"),
        linear("floz", VOLUME, "0.0000295735295625"),
        // The US legal cup of 240 mL, which is what a recipe means.
        linear("cup", VOLUME, "0.00024"),
    )

    /**
     * Area, base square metre.
     *
     * `a` — the are — is **not** registered, and neither is `a` for the year: ISO 80000 gives
     * the same symbol to both, and a calculator that guessed would be wrong silently. `ha`
     * and `acre` are unambiguous, and `yr` carries the year (see [time]).
     */
    private fun area(): List<UnitDefinition> = listOf(
        linear("ha", AREA, "10000"),
        linear("acre", AREA, "4046.8564224"),
    )

    private fun speed(): List<UnitDefinition> = listOf(
        // One nautical mile per hour: 1852 / 3600 m/s, written exactly.
        UnitDefinition(
            symbol = "kn",
            dimensions = SPEED,
            scale = BigDecimal("1852").divide(BigDecimal("3600"), MATH),
            aliases = listOf("kt"),
        ),
    )

    /**
     * Time beyond what the engine ships.
     *
     * `yr` is the mean Gregorian year of 365.2425 days, the one a calendar actually repeats
     * on. ISO's symbol for it is `a`, which is also the are — see [area] for why neither is
     * registered under that name.
     */
    private fun time(): List<UnitDefinition> = listOf(
        linear("wk", Dimensions.TIME, "604800"),
        linear("yr", Dimensions.TIME, "31556952"),
    )

    /**
     * Data, base bit, per IEC 80000-13.
     *
     * The decimal and binary prefixes are different units and not different spellings: `kB`
     * is 1000 bytes and `KiB` is 1024, which is the distinction the definition of done names
     * and the one every wrong file-size display in the world gets wrong.
     */
    private fun data(): List<UnitDefinition> = listOf(
        linear("bit", Dimensions.INFORMATION, "1", aliases = listOf("b")),
        linear("B", Dimensions.INFORMATION, "8", aliases = listOf("byte")),
        linear("kB", Dimensions.INFORMATION, "8000"),
        linear("MB", Dimensions.INFORMATION, "8000000"),
        linear("GB", Dimensions.INFORMATION, "8000000000"),
        linear("TB", Dimensions.INFORMATION, "8000000000000"),
        linear("KiB", Dimensions.INFORMATION, "8192"),
        linear("MiB", Dimensions.INFORMATION, "8388608"),
        linear("GiB", Dimensions.INFORMATION, "8589934592"),
        linear("TiB", Dimensions.INFORMATION, "8796093022208"),
    )

    /** Pressure, base pascal. `mmHg` and `atm` are exact by definition; `psi` follows the pound. */
    private fun pressure(): List<UnitDefinition> = listOf(
        linear("Pa", PRESSURE, "1"),
        linear("hPa", PRESSURE, "100"),
        linear("kPa", PRESSURE, "1000"),
        linear("MPa", PRESSURE, "1000000"),
        linear("bar", PRESSURE, "100000"),
        linear("mbar", PRESSURE, "100"),
        linear("atm", PRESSURE, "101325"),
        linear("psi", PRESSURE, "6894.757293168361"),
        linear("mmHg", PRESSURE, "133.322387415"),
    )

    /** Energy, base joule. `cal` is the thermochemical calorie and `BTU` the IT one. */
    private fun energy(): List<UnitDefinition> = listOf(
        linear("J", ENERGY, "1"),
        linear("kJ", ENERGY, "1000"),
        linear("MJ", ENERGY, "1000000"),
        linear("Wh", ENERGY, "3600"),
        linear("kWh", ENERGY, "3600000"),
        linear("cal", ENERGY, "4.184"),
        linear("kcal", ENERGY, "4184", aliases = listOf("Cal")),
        linear("BTU", ENERGY, "1055.05585262"),
        linear("eV", ENERGY, "0.0000000000000000001602176634"),
    )

    private fun linear(
        symbol: String,
        dimensions: Dimensions,
        scale: String,
        aliases: List<String> = emptyList(),
    ): UnitDefinition = UnitDefinition(symbol, dimensions, BigDecimal(scale), aliases = aliases)
}
