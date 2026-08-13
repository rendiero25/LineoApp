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
 * Every external module on a runtime classpath must appear in
 * `config/licenses/allowed-dependencies.txt` together with its licence. A dependency
 * that is not listed fails the build, which is exactly the intent of `AGENTS.md` §7:
 * adding a dependency is a human decision, and the licence is recorded when it is made.
 */
abstract class CheckDependencyLicensesTask : DefaultTask() {
    init {
        group = "verification"
        description = "Fails when a dependency is not recorded in the licence allowlist."
    }

    /** `group:name` of every module reachable from the resolved runtime classpaths. */
    @get:Input
    abstract val modules: SetProperty<String>

    @get:InputFile
    abstract val allowlist: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val allowed = readAllowlist(allowlist.get().asFile.readLines())
        val used = modules.get().toSortedSet()
        val unknown = used - allowed

        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(used.joinToString(separator = "\n", postfix = "\n"))
        }

        if (unknown.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Dependencies missing from config/licenses/allowed-dependencies.txt:")
                    unknown.forEach { appendLine("  $it") }
                    appendLine()
                    appendLine("Only Apache-2.0, MIT or BSD are permitted (AGENTS.md §2).")
                    appendLine("Record the licence in that file once a human has approved the dependency.")
                },
            )
        }
    }
}

/**
 * Merges this module's dependencies into the shared allowlist. Every module contributes,
 * so the task runs per module and unions rather than overwrites; the resulting diff is
 * what a human reviews before approving a new dependency.
 */
abstract class WriteDependencyLicenseAllowlistTask : DefaultTask() {
    init {
        group = "verification"
        description = "Merges this module's dependencies into the licence allowlist."
        outputs.upToDateWhen { false }
    }

    @get:Input
    abstract val modules: SetProperty<String>

    @get:Internal
    abstract val allowlist: RegularFileProperty

    @TaskAction
    fun write() {
        val file = allowlist.get().asFile
        val existing = if (file.exists()) readAllowlistWithLicences(file.readLines()) else emptyMap()
        val merged = (existing.keys + modules.get()).toSortedSet()
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                appendLine("# Dependency licence allowlist — see AGENTS.md §2, docs/ARCHITECTURE.md §7.")
                appendLine("# Only Apache-2.0, MIT and BSD are permitted.")
                appendLine("# Format: group:name  # LICENCE")
                appendLine("# Regenerate with ./gradlew writeDependencyLicenseAllowlist, then fill in")
                appendLine("# any TODO licence and review the diff by hand.")
                appendLine()
                merged.forEach { module ->
                    appendLine("$module  # ${existing[module]?.takeIf { it.isNotEmpty() } ?: "TODO"}")
                }
            },
        )
    }
}

private fun readAllowlist(lines: List<String>): Set<String> = readAllowlistWithLicences(lines).keys

private fun readAllowlistWithLicences(lines: List<String>): Map<String, String> = lines
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .associate { line ->
        val module = line.substringBefore("#").trim()
        val licence = line.substringAfter("#", missingDelimiterValue = "").trim()
        module to licence
    }

/** Registers the licence tasks for a module, wiring in every resolvable runtime classpath. */
internal fun Project.configureLicenseCheck() {
    val allowlistFile = rootProject.layout.projectDirectory.file("config/licenses/allowed-dependencies.txt")

    afterEvaluate {
        val roots: ListProperty<ResolvedComponentResult> = objects.listProperty(ResolvedComponentResult::class.java)
        configurations
            .filter { it.isCanBeResolved && it.name.lowercase().endsWith("runtimeclasspath") }
            .forEach { roots.add(it.incoming.resolutionResult.rootComponent) }

        val modulesProvider = roots.map { components ->
            components.flatMapTo(sortedSetOf()) { collectModules(it) } as Set<String>
        }

        val check = tasks.register("checkDependencyLicenses", CheckDependencyLicensesTask::class.java) {
            modules.set(modulesProvider)
            allowlist.set(allowlistFile)
            report.set(layout.buildDirectory.file("reports/licenses/dependencies.txt"))
        }

        tasks.register("writeDependencyLicenseAllowlist", WriteDependencyLicenseAllowlistTask::class.java) {
            modules.set(modulesProvider)
            allowlist.set(allowlistFile)
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
