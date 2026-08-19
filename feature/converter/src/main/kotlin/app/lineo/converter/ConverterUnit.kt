package app.lineo.converter

/**
 * One unit as the screen offers it: what the user reads, and what the engine is given.
 *
 * The two differ wherever a unit is a power or a quotient of others. `m²` is a label; what
 * the parser accepts is `m^2`, and `km/h` is a division of two symbols rather than a symbol
 * of its own. Writing the expression down here — rather than registering `m2` as a fake
 * symbol, which is neither ISO nor lexable — keeps `docs/CONVENTIONS.md` §5 honest and keeps
 * the conversion in the engine, where `5 km/h to mi/h` already works.
 *
 * @param label what the chip and the picker show. ISO 80000 where ISO has a symbol.
 * @param expression what is typed into the expression. The same as [label] for a plain unit.
 */
internal data class ConverterUnit(val label: String, val expression: String = label)
