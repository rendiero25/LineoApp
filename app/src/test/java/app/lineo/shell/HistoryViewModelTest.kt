package app.lineo.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The tape as the screen reads it.
 *
 * There is little here on purpose: the entries come from the database and the cap is applied
 * on write, so what a view model can get wrong is only *when* it subscribes and whether it
 * publishes what it was given.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = RecordingHistoryRepository()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the tape starts empty and does not wait for the database to say so`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)

        // Empty, immediately: a screen that waited would flash its empty state after the fact.
        assertEquals(emptyList<String>(), viewModel.entries.value.map { it.expression })
    }

    @Test
    fun `what was recorded is what the screen shows, newest first`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)
        val collector = launch { viewModel.entries.collect {} }
        repository.record("2 + 3", "5")
        repository.record("6 * 7", "42")
        advanceUntilIdle()

        assertEquals(listOf("6 * 7", "2 + 3"), viewModel.entries.value.map { it.expression })

        collector.cancel()
    }

    @Test
    fun `clearing empties the tape`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)
        val collector = launch { viewModel.entries.collect {} }
        repository.record("2 + 3", "5")
        advanceUntilIdle()

        viewModel.clear()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.entries.value.map { it.expression })

        collector.cancel()
    }
}
