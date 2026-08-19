package app.lineo.shell

import app.lineo.data.model.Document
import app.lineo.data.model.Line
import app.lineo.data.repository.DocumentRepository
import app.lineo.engine.EvalContext
import app.lineo.engine.LineId
import app.lineo.engine.unit.UnitRegistry
import app.lineo.notepad.NotepadStore
import app.lineo.registry.EditorCommand
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import app.lineo.scientific.ScientificModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Locale

/**
 * The wiring between the screen and storage: when the document is read, and when it is
 * written.
 *
 * What is *not* tested here is the mapping or the autosave — both belong to
 * `:feature:notepad` and are asserted there. This is only the half that needs a lifecycle:
 * that opening happens once, and that a stop writes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotepadViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = InMemoryDocumentRepository()
    private val store = NotepadStore(repository)

    @Before
    fun setMainDispatcher() {
        // viewModelScope is pinned to Dispatchers.Main, which does not exist in a unit test.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `nothing is published until the document has been read`() = runTest(dispatcher) {
        val viewModel = NotepadViewModel(store, backgroundScope)

        viewModel.open("Notepad", CONTEXT)

        assertNull(viewModel.notepad.value)
    }

    @Test
    fun `opening creates the document and publishes a line to type on`() = runTest(dispatcher) {
        val viewModel = NotepadViewModel(store, backgroundScope)

        viewModel.open("Notepad", CONTEXT)
        advanceUntilIdle()

        assertEquals(1, repository.getDocumentsStream().first().size)
        assertEquals(1, viewModel.notepad.value?.state?.uiState?.value?.lines?.size)
    }

    @Test
    fun `opening twice does not read the document again`() = runTest(dispatcher) {
        val viewModel = NotepadViewModel(store, backgroundScope)
        viewModel.open("Notepad", CONTEXT)
        advanceUntilIdle()
        val first = viewModel.notepad.value?.state

        viewModel.open("Notepad", CONTEXT)
        advanceUntilIdle()

        // Same object, so the caret and the draft the user is halfway through survive.
        assertSame(first, viewModel.notepad.value?.state)
        assertEquals(1, repository.getDocumentsStream().first().size)
    }

    @Test
    fun `a stop writes what has been typed`() = runTest(dispatcher) {
        val viewModel = NotepadViewModel(store, backgroundScope)
        viewModel.open("Notepad", CONTEXT)
        advanceUntilIdle()
        val state = requireNotNull(viewModel.notepad.value).state
        "6 * 7".forEach { state.apply(EditorCommand.InsertText(it.toString())) }

        viewModel.flush()
        advanceUntilIdle()

        assertEquals(listOf("6 * 7"), store.openOrCreate("Notepad").document.lines.map { it.source })
    }

    private companion object {
        /**
         * What the activity passes in: the built-ins plus every module this build contains,
         * functions and units alike. The notepad has to evaluate against the same set its
         * module screens call, and the view model is where that set arrives.
         */
        val CONTEXT = ModuleRegistry(setOf(ScientificModule())).let { registry ->
            EvalContext(
                locale = Locale.US,
                functions = registry.functionRegistry(Tier.FREE, Locale.US),
                units = UnitRegistry.BUILTIN.with(registry.units(Tier.FREE, Locale.US)),
            )
        }
    }
}

/**
 * The store's other half, in memory.
 *
 * Only what the view model's path touches is implemented; the rest is unreachable from here
 * and says so rather than returning something a test could accidentally rely on. The fuller
 * fake, and the mapping tests that use it, live in `:feature:notepad`.
 */
private class InMemoryDocumentRepository : DocumentRepository {

    private val documents = MutableStateFlow(emptyList<Document>())
    private val lines = MutableStateFlow(emptyList<Line>())

    override fun getDocumentsStream(): Flow<List<Document>> = documents

    override fun getLinesStream(documentId: Long): Flow<List<Line>> =
        lines.map { all -> all.filter { it.documentId == documentId }.sortedBy { it.ordinal } }

    override suspend fun createDocument(title: String): Long {
        val id = (documents.value.maxOfOrNull { it.id } ?: 0L) + 1
        documents.value += Document(id, title, Instant.EPOCH, Instant.EPOCH, sortIndex = 0)
        return id
    }

    override suspend fun updateLine(line: Line) {
        lines.value = lines.value.filterNot { it.id == line.id } + line
    }

    override suspend fun deleteLine(documentId: Long, id: LineId) {
        lines.value = lines.value.filterNot { it.id == id }
    }

    override fun getDocumentStream(id: Long): Flow<Document?> = error("not used by the notepad route")

    override suspend fun renameDocument(id: Long, title: String) = error("not used by the notepad route")

    override suspend fun deleteDocument(id: Long) = error("not used by the notepad route")

    override suspend fun appendLine(documentId: Long, source: String, label: String?): LineId =
        error("the document allocates line ids, not the store")

    override suspend fun reorderLines(documentId: Long, orderedLineIds: List<LineId>) =
        error("not used by the notepad route")
}
