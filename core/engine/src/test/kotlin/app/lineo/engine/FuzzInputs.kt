package app.lineo.engine

import java.util.Locale
import kotlin.random.Random

/**
 * Shared inputs for the fuzzing contract of `docs/GRAMMAR.md` §6, used by both the engine
 * and the parser fuzz tests. The seed is fixed so a failure is always reproducible.
 */
object FuzzInputs {
    const val SEED = 20260813L
    const val RANDOM_CASES = 500
    private const val MAX_LENGTH = 60

    val LOCALES: List<Locale> = listOf(
        Locale.forLanguageTag("en-US"),
        Locale.forLanguageTag("id-ID"),
        Locale.forLanguageTag("de-DE"),
        Locale.forLanguageTag("hi-IN"),
    )

    /** Garbage that looks well formed — the inputs a random character generator rarely hits. */
    val WELL_FORMED_LOOKING: List<String> = listOf(
        "sin(",
        "5 +",
        "()",
        "((((",
        "line999999",
        "@",
        "2 to to to km",
        "%%%",
        "!!!",
        "1////2",
        "x = = 5",
        "0/0",
        "-----5",
        "e^e^e",
        " ",
        "𝟘𝟙𝟚",
        "5 in 3",
        "max(1;2,3)",
        "1 mod 0",
        "((1+2)",
        "1)+2",
    )

    val DEEP_NESTING: List<String> = listOf(50, 200, 1_000).flatMap { depth ->
        listOf(
            "(".repeat(depth) + "1" + ")".repeat(depth),
            "(".repeat(depth) + "1",
            "1" + ")".repeat(depth),
        )
    }

    val HUGE_EXPONENTS: List<String> = listOf(
        "1e999999999",
        "9".repeat(500) + "^99999",
        "1e-999999999",
        "2^2^2^2^2^2",
        "10^10^10",
    )

    val MIXED_SEPARATORS: List<String> = listOf(
        "1.234,56 + 1,234.56",
        "max(1,5;2.5)",
        "1 234,56",
        "1,2,3.4.5",
        "٣٫١٤",
    )

    val LONG_INPUTS: List<String> = listOf(" ".repeat(10_000), "1+".repeat(5_000))

    fun randomStrings(count: Int = RANDOM_CASES, seed: Long = SEED): List<String> {
        val random = Random(seed)
        return List(count) {
            val length = random.nextInt(1, MAX_LENGTH)
            buildString(length) {
                repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.size)]) }
            }
        }
    }

    private val ALPHABET: List<Char> = buildList {
        addAll('0'..'9')
        addAll('a'..'z')
        addAll("+-*/^%!()[]{}.,;:= \t\n@#$&|<>?\\\"'~`_".toList())
        addAll("°πµ√∞≠≤≥×÷".toList())
    }
}
