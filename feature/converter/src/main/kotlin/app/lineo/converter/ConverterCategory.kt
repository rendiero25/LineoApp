package app.lineo.converter

import androidx.annotation.StringRes
import app.lineo.data.settings.UnitSystem

/**
 * One row of the converter: a quantity, and the units it can be expressed in.
 *
 * These are the categories a user thinks in, which are not the dimensions the engine thinks
 * in — speed is length over time and pressure is mass over length over time squared, and
 * neither has a dimension of its own. Grouping is therefore this screen's business, while
 * what converts into what stays the engine's rule: two units convert when their dimensions
 * match, whatever this file calls them.
 *
 * @param id stable, never localised. It is what a saved selection holds.
 * @param titleRes what the category chip shows.
 * @param units the units offered, in the order shown. The first two are what a fresh
 *   selection converts from and to.
 */
internal enum class ConverterCategory(
    val id: String,
    @param:StringRes val titleRes: Int,
    val units: List<ConverterUnit>,
) {
    LENGTH(
        id = "length",
        titleRes = R.string.converter_length,
        units = units("m", "km", "cm", "mm", "µm", "nm", "mi", "yd", "ft", "in", "nmi"),
    ),
    MASS(
        id = "mass",
        titleRes = R.string.converter_mass,
        units = units("kg", "g", "mg", "t", "lb", "oz", "st", "ct"),
    ),
    VOLUME(
        id = "volume",
        titleRes = R.string.converter_volume,
        units = units("L", "mL", "cL", "dL") +
            ConverterUnit("m³", "m^3") + ConverterUnit("cm³", "cm^3") +
            units("gal", "qt", "pt", "floz", "cup"),
    ),
    AREA(
        id = "area",
        titleRes = R.string.converter_area,
        units = listOf(
            ConverterUnit("m²", "m^2"),
            ConverterUnit("km²", "km^2"),
            ConverterUnit("cm²", "cm^2"),
            ConverterUnit("ha"),
            ConverterUnit("acre"),
            ConverterUnit("ft²", "ft^2"),
            ConverterUnit("in²", "in^2"),
            ConverterUnit("mi²", "mi^2"),
        ),
    ),
    SPEED(
        id = "speed",
        titleRes = R.string.converter_speed,
        units = listOf(
            ConverterUnit("km/h"),
            ConverterUnit("m/s"),
            ConverterUnit("mi/h"),
            ConverterUnit("ft/s"),
            ConverterUnit("kn"),
        ),
    ),
    TEMPERATURE(
        id = "temperature",
        titleRes = R.string.converter_temperature,
        units = units("°C", "°F", "K"),
    ),
    DATA(
        id = "data",
        titleRes = R.string.converter_data,
        units = units("B", "kB", "MB", "GB", "TB", "KiB", "MiB", "GiB", "TiB", "bit"),
    ),
    TIME(
        id = "time",
        titleRes = R.string.converter_time,
        units = units("s", "min", "h", "d", "wk", "yr", "ms"),
    ),
    PRESSURE(
        id = "pressure",
        titleRes = R.string.converter_pressure,
        units = units("Pa", "hPa", "kPa", "MPa", "bar", "mbar", "atm", "psi", "mmHg"),
    ),
    ENERGY(
        id = "energy",
        titleRes = R.string.converter_energy,
        units = units("J", "kJ", "MJ", "Wh", "kWh", "cal", "kcal", "BTU", "eV"),
    ),
    ;

    /**
     * The pair a fresh selection starts on, in [system].
     *
     * Metric takes the first two units, which are ordered metric-first; imperial takes the
     * pair a customary user actually reaches for — feet to miles rather than metres to
     * kilometres. Where a category has no imperial reading (data, time) both are the same,
     * and saying so here is cheaper than a setting that quietly does nothing to five of the
     * ten rows.
     */
    fun defaultPair(system: UnitSystem): Pair<ConverterUnit, ConverterUnit> {
        val imperial = IMPERIAL_PAIRS[this]
        return if (system == UnitSystem.IMPERIAL && imperial != null) {
            ConverterUnit(imperial.first) to ConverterUnit(imperial.second)
        } else {
            units[0] to units[1]
        }
    }

    val defaultFrom: ConverterUnit get() = units.first()
    val defaultTo: ConverterUnit get() = units[1]

    companion object {

        /** Unknown ids fall back to length rather than failing: a saved id can outlive a build. */
        fun of(id: String?): ConverterCategory = entries.firstOrNull { it.id == id } ?: LENGTH
    }
}

/**
 * What each category starts on for someone who writes in customary units.
 *
 * Only the categories where it differs: a byte is a byte, and a second is a second.
 *
 * **`by lazy`, and it has to be.** Every constant of [ConverterCategory] is built by
 * `converterUnits`, a top-level function in this file, so constructing the constants runs
 * this file's initializer — while the constants themselves are still null. Built eagerly,
 * this map would key eight entries on null and every lookup would miss, which is exactly how
 * an imperial user was silently shown `m → km`. Deferring it to the first lookup, which
 * cannot happen before the constants exist, is what makes the keys real.
 */
private val IMPERIAL_PAIRS: Map<ConverterCategory, Pair<String, String>> by lazy {
    mapOf(
        ConverterCategory.LENGTH to ("ft" to "mi"),
        ConverterCategory.MASS to ("lb" to "oz"),
        ConverterCategory.VOLUME to ("gal" to "floz"),
        ConverterCategory.AREA to ("acre" to "ha"),
        ConverterCategory.SPEED to ("mi/h" to "km/h"),
        ConverterCategory.TEMPERATURE to ("°F" to "°C"),
        ConverterCategory.PRESSURE to ("psi" to "bar"),
        ConverterCategory.ENERGY to ("BTU" to "kJ"),
    )
}

/**
 * Units whose label is their expression: the plain symbols.
 *
 * A top-level function rather than one on the companion, because an enum's constants are
 * constructed before its companion exists — a helper there is `null` at exactly the moment
 * every entry needs it.
 */
private fun units(vararg symbols: String): List<ConverterUnit> = symbols.map(::ConverterUnit)
