package app.lineo.engine.golden

import java.util.Locale

/**
 * Reads golden files. A malformed golden line is itself an error — it fails loudly rather
 * than being skipped, otherwise a typo would silently delete test coverage.
 */
object GoldenFileParser {
    private const val LOCALE_PREFIX = "locale="
    private const val SEPARATOR = '|'
    private const val ERROR_MARKER = '!'
    private const val SPAN_MARKER = '@'
    private const val SPAN_RANGE = ".."

    fun parse(fileName: String, content: String, defaultLocale: Locale = Locale.US): List<GoldenCase> {
        var locale = defaultLocale
        val cases = mutableListOf<GoldenCase>()

        content.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()

            when {
                line.isEmpty() || line.startsWith("#") -> Unit

                line.startsWith(LOCALE_PREFIX) ->
                    locale = Locale.forLanguageTag(line.removePrefix(LOCALE_PREFIX).trim())

                line.contains(SEPARATOR) -> cases += parseCase(fileName, lineNumber, locale, line)

                else -> error("$fileName:$lineNumber: not a golden line, expected 'input | expected': $line")
            }
        }

        return cases
    }

    private fun parseCase(fileName: String, lineNumber: Int, locale: Locale, line: String): GoldenCase {
        val input = line.substringBefore(SEPARATOR).trim()
        val expected = line.substringAfter(SEPARATOR).trim()

        require(input.isNotEmpty()) { "$fileName:$lineNumber: empty input" }
        require(expected.isNotEmpty()) { "$fileName:$lineNumber: empty expectation" }

        return GoldenCase(
            file = fileName,
            lineNumber = lineNumber,
            locale = locale,
            input = input,
            expectation = parseExpectation(fileName, lineNumber, expected),
        )
    }

    private fun parseExpectation(fileName: String, lineNumber: Int, expected: String): Expectation {
        if (!expected.startsWith(ERROR_MARKER)) return Expectation.Value(expected)

        val body = expected.removePrefix(ERROR_MARKER.toString())
        val type = body.substringBefore(SPAN_MARKER).trim()
        require(type.isNotEmpty()) { "$fileName:$lineNumber: '!' without an error type" }

        if (!body.contains(SPAN_MARKER)) return Expectation.Error(type, span = null)

        val spanText = body.substringAfter(SPAN_MARKER).trim()
        val start = spanText.substringBefore(SPAN_RANGE).trim().toIntOrNull()
        val end = spanText.substringAfter(SPAN_RANGE).trim().toIntOrNull()
        require(start != null && end != null) {
            "$fileName:$lineNumber: malformed span '$spanText', expected '@start..end'"
        }

        return Expectation.Error(type, span = start..end)
    }
}
