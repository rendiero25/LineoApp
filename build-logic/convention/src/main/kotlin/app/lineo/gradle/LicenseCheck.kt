package app.lineo.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Licence policy gate, `AGENTS.md` §2 and `docs/ARCHITECTURE.md` §7.
 *
 * The gate splits dependencies in two, because their obligations are not the same:
 *
 * - **Shipped** — anything on a non-test runtime classpath. It reaches the APK, so it is
 *   distributed, so §2 applies in full: Apache-2.0, MIT or BSD, and nothing else. Both the
 *   presence of the entry *and its recorded licence* are checked.
 * - **Test-only** — reachable from unit or instrumented test classpaths and nowhere else.
 *   Never distributed, so copyleft obligations never trigger. It still has to be recorded,
 *   because adding a dependency remains a human decision, but a copyleft licence there does
 *   not fail the build.
 *
 * Before the split, one list held both and every EPL or LGPL test transitive demanded the
 * same approval as a shipped library. Fifteen harmless rows train a reviewer to wave the
 * sixteenth through — and the sixteenth is the one that ships.
 */
abstract class CheckDependencyLicensesTask : DefaultTask() {
    init {
        group = "verification"
        description = "Fails when a dependency is unrecorded, or when a shipped one is not Apache-2.0, MIT or BSD."
    }

    /** `group:name` of every module reachable from a runtime classpath that is not a test one. */
    @get:Input
    abstract val shippedModules: SetProperty<String>

    /** `group:name` of every module reachable from a test runtime classpath. */
    @get:Input
    abstract val testModules: SetProperty<String>

    @get:InputFile
    abstract val shippedAllowlist: RegularFileProperty

    @get:InputFile
    abstract val testAllowlist: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val shippedAllowed = readAllowlistWithLicences(shippedAllowlist.get().asFile.readLines())
        val testAllowed = readAllowlistWithLicences(testAllowlist.get().asFile.readLines())

        val shipped = shippedModules.get().toSortedSet()
        val testOnly = (testModules.get() - shipped).toSortedSet()

        writeReport(shipped, testOnly)

        val problems = buildList {
            (shipped - shippedAllowed.keys).forEach {
                add("$it ships but is missing from $SHIPPED_ALLOWLIST")
            }
            shipped.forEach { module ->
                val licence = shippedAllowed[module] ?: return@forEach
                if (!isPermissive(licence)) {
                    add("$module ships under \"$licence\" — §2 permits only Apache-2.0, MIT and BSD")
                }
            }
            (testOnly - testAllowed.keys - shippedAllowed.keys).forEach {
                add("$it is used by tests but is missing from $TEST_ALLOWLIST")
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Dependency licence policy (AGENTS.md §2, docs/ARCHITECTURE.md §7):")
                    problems.forEach { appendLine("  $it") }
                    appendLine()
                    appendLine("Run ./gradlew writeDependencyLicenseAllowlist, fill in any TODO licence,")
                    appendLine("and review the diff by hand. A new line in $SHIPPED_ALLOWLIST is the one")
                    appendLine("that matters: it is what reaches the Play Store.")
                },
            )
        }
    }

    private fun writeReport(shipped: Set<String>, testOnly: Set<String>) {
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                buildString {
                    appendLine("# shipped")
                    shipped.forEach { appendLine(it) }
                    appendLine("# test-only")
                    testOnly.forEach { appendLine(it) }
                },
            )
        }
    }
}

/**
 * Serialises writes to the allowlists.
 *
 * Every module registers its own write task and Gradle runs them in parallel by default;
 * four tasks read-modify-writing the same two files at once produced torn lines and
 * duplicated entries. A Gradle `BuildService` is the textbook answer and does not work here:
 * each project gets its own classloader for the convention plugin, so the service type of
 * one project is not the service type of the next.
 *
 * An interned string is. The JVM string pool is shared by every classloader in the daemon,
 * so this monitor is the same object in all of them.
 */
private val ALLOWLIST_MONITOR: String = "app.lineo.gradle.licence-allowlist".intern()

/**
 * Merges this module's dependencies into whichever allowlist they belong to.
 *
 * It only ever adds. Moving an entry from one list to the other, or dropping one that is no
 * longer used, is left to a human — the diff is the point of the file.
 */
abstract class WriteDependencyLicenseAllowlistTask : DefaultTask() {
    init {
        group = "verification"
        description = "Merges this module's dependencies into the licence allowlists."
        outputs.upToDateWhen { false }
    }

    @get:Input
    abstract val shippedModules: SetProperty<String>

    @get:Input
    abstract val testModules: SetProperty<String>

    @get:Internal
    abstract val shippedAllowlist: RegularFileProperty

    @get:Internal
    abstract val testAllowlist: RegularFileProperty

    @TaskAction
    fun write() = synchronized(ALLOWLIST_MONITOR) {
        val shippedFile = shippedAllowlist.get().asFile
        val testFile = testAllowlist.get().asFile
        val known = readExisting(shippedFile) + readExisting(testFile)

        val shipped = shippedModules.get().toSortedSet()
        val testOnly = (testModules.get() - shipped).toSortedSet()

        writeList(
            file = shippedFile,
            modules = (readExisting(shippedFile).keys + shipped).toSortedSet(),
            known = known,
            header = listOf(
                "# Dependencies that reach the APK — AGENTS.md §2, docs/ARCHITECTURE.md §7.",
                "# Only Apache-2.0, MIT and BSD are permitted, and the licence recorded here is",
                "# what the build checks. Test-only dependencies belong in the other file.",
            ),
        )
        writeList(
            file = testFile,
            modules = (readExisting(testFile).keys + testOnly - shipped).toSortedSet(),
            known = known,
            header = listOf(
                "# Dependencies used only by tests — never distributed, so §2's licence rule",
                "# does not apply to them. They are still recorded, because adding a dependency",
                "# is a human decision. Record the real licence, copyleft included.",
            ),
        )
    }

    private fun readExisting(file: java.io.File): Map<String, String> =
        if (file.exists()) readAllowlistWithLicences(file.readLines()) else emptyMap()

    private fun writeList(
        file: java.io.File,
        modules: Set<String>,
        known: Map<String, String>,
        header: List<String>,
    ) {
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                header.forEach { appendLine(it) }
                appendLine("# Format: group:name  # LICENCE")
                appendLine("# Regenerate with ./gradlew writeDependencyLicenseAllowlist, then fill in")
                appendLine("# any TODO licence and review the diff by hand.")
                appendLine()
                modules.forEach { module ->
                    appendLine("$module  # ${known[module]?.takeIf { it.isNotEmpty() } ?: "TODO"}")
                }
            },
        )
    }
}

private const val SHIPPED_ALLOWLIST = "config/licenses/allowed-dependencies.txt"
private const val TEST_ALLOWLIST = "config/licenses/allowed-test-dependencies.txt"

/**
 * SPDX identifiers §2 permits for anything that ships.
 *
 * Matched against the head of the recorded licence, so `Apache-2.0 (approved by name in
 * docs/ARCHITECTURE.md §7)` passes while `LGPL-2.1` does not. Anything ambiguous fails; the
 * fix is to record the licence precisely, not to widen this list.
 */
private val PERMISSIVE_LICENCES = listOf(
    "Apache-2.0",
    "MIT",
    "BSD-2-Clause",
    "BSD-3-Clause",
)

internal fun isPermissive(licence: String): Boolean {
    val head = licence.substringBefore("(").trim()
    return PERMISSIVE_LICENCES.any { head == it || head.startsWith("$it ") || head.startsWith("$it,") }
}

private fun readAllowlistWithLicences(lines: List<String>): Map<String, String> = lines
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .associate { line ->
        val module = line.substringBefore("#").trim()
        val licence = line.substringAfter("#", missingDelimiterValue = "").trim()
        module to licence
    }

/**
 * A runtime classpath that only a test can see. Unit tests, instrumented tests, and the
 * screenshot variants AGP and Paparazzi add.
 */
private fun isTestClasspath(name: String): Boolean {
    val lower = name.lowercase()
    return lower.startsWith("test") ||
        lower.contains("unittest") ||
        lower.contains("androidtest") ||
        lower.contains("screenshottest") ||
        lower.contains("testfixtures")
}

/** Registers the licence tasks for a module, wiring in every resolvable runtime classpath. */
internal fun Project.configureLicenseCheck() {
    val shippedFile = rootProject.layout.projectDirectory.file(SHIPPED_ALLOWLIST)
    val testFile = rootProject.layout.projectDirectory.file(TEST_ALLOWLIST)


    afterEvaluate {
        val shippedRoots: ListProperty<ResolvedComponentResult> =
            objects.listProperty(ResolvedComponentResult::class.java)
        val testRoots: ListProperty<ResolvedComponentResult> =
            objects.listProperty(ResolvedComponentResult::class.java)

        configurations
            .filter { it.isCanBeResolved && it.name.lowercase().endsWith("runtimeclasspath") }
            .forEach { configuration ->
                val target = if (isTestClasspath(configuration.name)) testRoots else shippedRoots
                target.add(configuration.incoming.resolutionResult.rootComponent)
            }

        val shippedProvider = shippedRoots.map { components ->
            components.flatMapTo(sortedSetOf()) { collectModules(it) } as Set<String>
        }
        val testProvider = testRoots.map { components ->
            components.flatMapTo(sortedSetOf()) { collectModules(it) } as Set<String>
        }

        val check = tasks.register("checkDependencyLicenses", CheckDependencyLicensesTask::class.java) {
            shippedModules.set(shippedProvider)
            testModules.set(testProvider)
            shippedAllowlist.set(shippedFile)
            testAllowlist.set(testFile)
            report.set(layout.buildDirectory.file("reports/licenses/dependencies.txt"))
        }

        tasks.register("writeDependencyLicenseAllowlist", WriteDependencyLicenseAllowlistTask::class.java) {
            shippedModules.set(shippedProvider)
            testModules.set(testProvider)
            shippedAllowlist.set(shippedFile)
            testAllowlist.set(testFile)
        }

        tasks.named("check").configure { dependsOn(check) }
    }
}

private fun collectModules(root: ResolvedComponentResult): Set<String> {
    val modules = sortedSetOf<String>()
    val seen = mutableSetOf<ComponentIdentifier>()

    fun visit(component: ResolvedComponentResult) {
        if (!seen.add(component.id)) return
        (component.id as? ModuleComponentIdentifier)?.let { modules += "${it.group}:${it.module}" }
        component.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .forEach { visit(it.selected) }
    }

    visit(root)
    return modules
}
