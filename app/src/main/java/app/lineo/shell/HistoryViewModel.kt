package app.lineo.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lineo.data.model.HistoryEntry
import app.lineo.data.repository.FREE_TIER_HISTORY_LIMIT
import app.lineo.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The history tape, as screen state (P1-06).
 *
 * The tape is read from the database rather than kept in memory, which is what makes it
 * survive process death for free: the entries were written as they happened, and this only
 * shows them. The cap is applied on write, so there is nothing to trim here.
 *
 * @see NotepadHistory for what puts a line on the tape.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
) : ViewModel() {

    /**
     * The newest [FREE_TIER_HISTORY_LIMIT] entries, newest first.
     *
     * `WhileSubscribed` with a timeout, so a rotation does not drop the query and open it
     * again — and so the query is not left running while the user is on another screen.
     */
    val entries: StateFlow<List<HistoryEntry>> = repository
        .getEntriesStream(FREE_TIER_HISTORY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
