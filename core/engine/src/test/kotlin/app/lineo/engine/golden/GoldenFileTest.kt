package app.lineo.engine.golden

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import java.io.File

/**
 * Runs every `.txt` file in `src/test/resources/golden` against the engine.
 *
 * Golden files are the definition of correct behaviour. Per `docs/GRAMMAR.md` §5, an
 * existing line is never edited to make a test pass — that is a behaviour change and needs
 * a human decision. New lines may be added freely.
 */
class GoldenFileTest {
    @TestFactory
    fun `golden files`(): List<DynamicNode> = goldenFiles().map { file ->
        val cases = GoldenFileParser.parse(file.name, file.readText())
        DynamicContainer.dynamicContainer(
            file.name,
            cases.map { case ->
                DynamicTest.dynamicTest("${case.lineNumber}: ${case.input}") {
                    GoldenRunner.run(case)?.let { failure -> fail(failure) }
                }
            },
        )
    }

    private fun goldenFiles(): List<File> {
        val root = javaClass.classLoader.getResource(GOLDEN_DIR)
            ?: return emptyList()
        return File(root.toURI())
            .listFiles { file -> file.isFile && file.name.endsWith(".txt") }
            .orEmpty()
            .sortedBy { it.name }
    }

    private companion object {
        const val GOLDEN_DIR = "golden"
    }
}
