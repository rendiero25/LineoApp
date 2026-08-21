package app.lineo.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * The arguments a literal becomes user-facing text through.
 *
 * Android's own `HardcodedText` reads layout XML, and Lineo has no layouts — every screen is
 * Compose, where a hardcoded string is an ordinary argument and lint sees nothing wrong with
 * it. These are the names that carry words to a screen or to a screen reader.
 */
private val SINKS = Regex(
    """\b(text|label|title|subtitle|description|contentDescription|onClickLabel|stateDescription)\s*=\s*"((\\.|[^"\\])*)"""",
)

/** `Text("Settings")` — the same thing, written positionally. */
private val POSITIONAL_TEXT = Regex("""\bText\s*\(\s*"((\\.|[^"\\])*)"""")

/** `${…}` and `$name`, which are values rather than words. */
private val INTERPOLATIONS = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_]*""")

/** Two letters in a row: what tells a sentence from a glyph, a separator or a format. */
private val WORDS = Regex("""\p{L}\p{L}""")

/**
 * What an author writes to say "this literal is not user-facing".
 *
 * On the same line, with a reason after it. A symbol that is the same in every language —
 * `√`, `⇅` — is the case this exists for, and saying so out loud is cheaper than arguing
 * with the check.
 */
private const val ALLOW_MARKER = "not-translatable:"

/**
 * Fails the build on user-facing text written into Kotlin rather than into `strings.xml`
 * (`AGENTS.md` §5, P1-09).
 *
 * Deliberately narrow, in two ways. Only the arguments in [SINKS] are looked at, because a
 * check that flagged every literal in a composable would flag keypad glyphs and format
 * patterns — and a check that cries wolf gets suppressed wholesale. And only literals with
 * *words* in them are flagged: `"$title: ${label(option)}"` is built from resources already,
 * while `"Clear history"` is a translation waiting to be lost.
 */
@CacheableTask
abstract class CheckHardcodedTextTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val offences = sources.asFileTree.files
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .flatMap { file ->
                file.readLines().withIndex().mapNotNull { (index, line) ->
                    if (line.contains(ALLOW_MARKER)) return@mapNotNull null
                    val literal = literalIn(line) ?: return@mapNotNull null
                    "${file.path}:${index + 1}: $literal"
                }
            }

        val output = report.get().asFile
        output.parentFile.mkdirs()
        output.writeText(if (offences.isEmpty()) "none\n" else offences.joinToString("\n", postfix = "\n"))

        if (offences.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("User-facing text belongs in strings.xml (AGENTS.md §5):")
                    offences.forEach { appendLine("  $it") }
                    appendLine()
                    appendLine("Use stringResource(R.string.…), or mark the line `$ALLOW_MARKER <why>`")
                    appendLine("when the literal is a symbol that reads the same in every language.")
                },
            )
        }
    }

    /** The first literal on [line] that carries words into a sink, or `null`. */
    private fun literalIn(line: String): String? {
        val candidates = SINKS.findAll(line).map { it.groupValues[2] } +
            POSITIONAL_TEXT.findAll(line).map { it.groupValues[1] }
        return candidates.firstOrNull { WORDS.containsMatchIn(INTERPOLATIONS.replace(it, "")) }
    }
}

/** Registers the check on this module's own `main` sources and hangs it off `check`. */
internal fun Project.configureHardcodedTextCheck() {
    val task = tasks.register("checkHardcodedText", CheckHardcodedTextTask::class.java) {
        // Main sources only. A test may say `Text("2 + 3")` all it likes — nobody translates
        // a fixture, and a snapshot needs the string it asserts on to be the string it draws.
        sources.from(layout.projectDirectory.dir("src/main"))
        report.set(layout.buildDirectory.file("reports/hardcoded-text/report.txt"))
    }
    tasks.named("check").configure { dependsOn(task) }
}
