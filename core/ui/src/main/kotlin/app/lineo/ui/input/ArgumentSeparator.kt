package app.lineo.ui.input

/**
 * The argument separator that goes with a given decimal separator.
 *
 * `docs/CONVENTIONS.md` §2: where the decimal separator is a comma, `max(1,5)` is
 * ambiguous, so the argument separator becomes a semicolon — the same resolution
 * spreadsheets use. Where the decimal separator is a dot, the comma is free to be the
 * argument separator.
 *
 * The keypad derives it rather than being told, because the two are not independent: a
 * keypad offering both `,` and `,` — one for each job — would show the same glyph twice
 * and type the wrong one half the time. That is exactly what the five-column layout
 * exposed when the argument separator was hardcoded.
 */
internal fun argumentSeparatorFor(decimalSeparator: Char): Char =
    if (decimalSeparator == ',') ';' else ','
