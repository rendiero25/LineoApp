package app.lineo.notepad

import app.lineo.engine.LineId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The graph P1-02 evaluates from: who reads whom, in what order, and which lines are stuck
 * in a circle.
 *
 * Documents are written as display text — the ordinals a user would type — and converted the
 * way the editor converts them, so the tests read like the document on screen while the
 * graph underneath works in ids.
 */
class DependencyGraphTest {

    @Test
    fun `a variable is read by the lines below its definition`() {
        val document = document("price = 100", "price * 2", "price + 1")
        val graph = DependencyGraph.of(document)
        val definition = document.idAtOrdinal(1)!!

        assertEquals(setOf(definition), graph.dependenciesOf(document.idAtOrdinal(2)!!))
        assertEquals(
            setOf(document.idAtOrdinal(2), document.idAtOrdinal(3)),
            graph.dependentsOf(definition),
        )
        assertEquals(mapOf(definition to "price"), graph.definitions)
    }

    @Test
    fun `a name used above its definition is not a dependency`() {
        // docs/GRAMMAR.md §3.1 scopes a variable to the lines after it. Above, `price` is an
        // unknown identifier, and an unknown identifier depends on nothing.
        val document = document("price * 2", "price = 100")
        val graph = DependencyGraph.of(document)

        assertEquals(emptySet<LineId>(), graph.dependenciesOf(document.idAtOrdinal(1)!!))
        assertEquals(emptySet<LineId>(), graph.dependentsOf(document.idAtOrdinal(2)!!))
    }

    @Test
    fun `the nearest definition above wins when a name is defined twice`() {
        val document = document("x = 1", "x + 1", "x = 5", "x + 2")
        val graph = DependencyGraph.of(document)

        assertEquals(setOf(document.idAtOrdinal(1)), graph.dependenciesOf(document.idAtOrdinal(2)!!))
        assertEquals(setOf(document.idAtOrdinal(3)), graph.dependenciesOf(document.idAtOrdinal(4)!!))
    }

    @Test
    fun `a line the parser cannot finish reads nothing and breaks nothing`() {
        val document = document("x = 5", "x +", "x * 2")
        val graph = DependencyGraph.of(document)

        assertEquals(emptySet<LineId>(), graph.dependenciesOf(document.idAtOrdinal(2)!!))
        assertEquals(setOf(document.idAtOrdinal(1)), graph.dependenciesOf(document.idAtOrdinal(3)!!))
        assertEquals(document.lines.size, graph.order.size)
    }

    @Test
    fun `a variable in scope beats a magnitude suffix, and the graph sees the edge`() {
        // `2k` is two thousand until a line defines `k` (docs/GRAMMAR.md §3.2). Getting this
        // right is why the definitions are collected in a pass of their own.
        val withoutDefinition = DependencyGraph.of(document("2k"))
        val document = document("k = 3", "2k")
        val graph = DependencyGraph.of(document)

        assertEquals(emptySet<LineId>(), withoutDefinition.dependenciesOf(withoutDefinition.order.single()))
        assertEquals(setOf(document.idAtOrdinal(1)), graph.dependenciesOf(document.idAtOrdinal(2)!!))
    }

    @Test
    fun `a line reference may point downwards, and the order follows the reference`() {
        val document = document("line3 + 1", "10", "20")
        val graph = DependencyGraph.of(document)

        assertEquals(
            listOf(document.idAtOrdinal(3), document.idAtOrdinal(1), document.idAtOrdinal(2)),
            graph.order,
        )
        assertTrue(graph.cycles.isEmpty())
    }

    @Test
    fun `dependencies come before dependents whatever the document order`() {
        val document = document("line4 * 2", "line1 + 1", "5", "line3 + 1")
        val graph = DependencyGraph.of(document)

        graph.order.forEachIndexed { position, id ->
            graph.dependenciesOf(id).forEach { dependency ->
                assertTrue("$dependency after $id", graph.order.indexOf(dependency) < position)
            }
        }
    }

    @Test
    fun `two lines that reference each other are both marked, and only them`() {
        val document = document("line2 + 1", "line1 + 1", "99")
        val graph = DependencyGraph.of(document)

        assertEquals(
            listOf(listOf(document.idAtOrdinal(1), document.idAtOrdinal(2))),
            graph.cycles,
        )
        // Every line in the cycle, not just the one the walk happened to close it on.
        assertEquals(graph.cycleOf(document.idAtOrdinal(1)!!), graph.cycleOf(document.idAtOrdinal(2)!!))
        assertNull(graph.cycleOf(document.idAtOrdinal(3)!!))
    }

    @Test
    fun `a line that references itself is a circle of one`() {
        val document = document("line1 + 1")

        val graph = DependencyGraph.of(document)

        assertEquals(listOf(listOf(document.idAtOrdinal(1))), graph.cycles)
    }

    @Test
    fun `a longer circle names every line on it`() {
        val document = document("line3", "line1", "line2")

        val graph = DependencyGraph.of(document)

        assertEquals(1, graph.cycles.size)
        assertEquals(document.lines.map { it.id }, graph.cycles.single())
    }

    @Test
    fun `independent circles stay independent`() {
        val document = document("line2", "line1", "line4", "line3")

        val graph = DependencyGraph.of(document)

        assertEquals(2, graph.cycles.size)
        assertEquals(document.lines.map { it.id }.toSet(), graph.inCycle)
        assertEquals(
            listOf(document.idAtOrdinal(1), document.idAtOrdinal(2)),
            graph.cycleOf(document.idAtOrdinal(1)!!),
        )
    }

    @Test
    fun `a line reading a circle is not part of it`() {
        val document = document("line2", "line1", "line1 + 1")
        val graph = DependencyGraph.of(document)
        val reader = document.idAtOrdinal(3)!!

        assertEquals(setOf(document.idAtOrdinal(1), document.idAtOrdinal(2)), graph.inCycle)
        assertNull(graph.cycleOf(reader))
    }

    @Test
    fun `transitive dependents are everything a change reaches and nothing else`() {
        val document = document("a = 1", "b = a + 1", "c = b + 1", "99")
        val graph = DependencyGraph.of(document)

        assertEquals(
            setOf(document.idAtOrdinal(2), document.idAtOrdinal(3)),
            graph.transitiveDependentsOf(document.idAtOrdinal(1)!!),
        )
        assertEquals(emptySet<LineId>(), graph.transitiveDependentsOf(document.idAtOrdinal(4)!!))
    }

    @Test
    fun `transitive dependents terminate inside a circle`() {
        val document = document("line2 + 1", "line1 + 1")
        val graph = DependencyGraph.of(document)

        assertEquals(
            setOf(document.idAtOrdinal(1), document.idAtOrdinal(2)),
            graph.transitiveDependentsOf(document.idAtOrdinal(1)!!),
        )
    }

    @Test
    fun `a reference to a deleted line is not an edge`() {
        val document = document("10", "line1 + 1")
        val without = document.delete(document.idAtOrdinal(1)!!)

        val graph = DependencyGraph.of(without)

        assertEquals(emptySet<LineId>(), graph.dependenciesOf(without.lines.single().id))
        assertEquals(listOf(without.lines.single().id), graph.order)
    }

    @Test
    fun `the same document always produces the same order`() {
        val document = document("x = 1", "line4", "x + 1", "x * 2", "line3 + line1")

        val orders = List(5) { DependencyGraph.of(document).order }

        assertEquals(1, orders.distinct().size)
        assertEquals(document.lines.size, orders.first().size)
    }

    private fun document(vararg displayLines: String): NotepadDocument = Documents.of(*displayLines)
}
