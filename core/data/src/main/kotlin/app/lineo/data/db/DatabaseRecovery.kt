package app.lineo.data.db

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the database opened normally, and what happened if it did not.
 *
 * `docs/ARCHITECTURE.md` §6: a database that cannot be read enters recovery mode; it never
 * crashes. Recovery is state, in the same way an evaluation error is state — the UI shows
 * it, the app keeps running.
 */
sealed interface RecoveryState {

    /** The normal case: the database opened and is being used. */
    data object Healthy : RecoveryState

    /**
     * The database could not be read and was replaced with an empty one.
     *
     * The user has lost stored documents and must be told so plainly. Silently starting
     * empty would read as the app having deleted their work on its own.
     */
    data class Recovered(val reason: Reason) : RecoveryState

    enum class Reason {
        /** SQLite reported the file as corrupt. Usually an interrupted write or bad storage. */
        CORRUPT_DATABASE,

        /** A migration could not be applied, so the existing rows were unreadable. */
        FAILED_MIGRATION,
    }
}

/**
 * Carries [state] from the point the database is opened to whatever shows it.
 *
 * It is a singleton and not a repository because it holds no data of its own: it exists so
 * that a failure deep inside SQLite reaches the UI as a value rather than as an exception.
 */
@Singleton
class DatabaseRecovery @Inject constructor() {

    private val mutableState = MutableStateFlow<RecoveryState>(RecoveryState.Healthy)

    val state: StateFlow<RecoveryState> = mutableState.asStateFlow()

    internal fun report(reason: RecoveryState.Reason) {
        mutableState.value = RecoveryState.Recovered(reason)
    }
}
