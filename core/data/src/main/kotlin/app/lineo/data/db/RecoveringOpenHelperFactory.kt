package app.lineo.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * Wraps a [SupportSQLiteOpenHelper.Factory] so that a corrupt database is reported to
 * [DatabaseRecovery] instead of taking the process down.
 *
 * SQLite's own recovery — discard the unreadable file and start again — is the only option
 * available: a corrupt page cannot be read back by any amount of retrying. What the
 * platform does *not* do is tell anyone it happened, so the user would silently find an
 * empty app. This wrapper adds exactly that signal, and leaves the deletion to the
 * framework.
 *
 * [SupportSQLiteOpenHelper.Configuration.allowDataLossOnRecovery] is set for the same
 * reason: without it the helper rethrows on a file it cannot salvage, which is the crash
 * `docs/ARCHITECTURE.md` §6 forbids.
 */
internal class RecoveringOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory,
    private val recovery: DatabaseRecovery,
) : SupportSQLiteOpenHelper.Factory {

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration,
    ): SupportSQLiteOpenHelper = delegate.create(
        SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .callback(ReportingCallback(configuration.callback, recovery))
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .allowDataLossOnRecovery(true)
            .build(),
    )

    /** Delegates every callback untouched; the one addition is the report in [onCorruption]. */
    private class ReportingCallback(
        private val delegate: SupportSQLiteOpenHelper.Callback,
        private val recovery: DatabaseRecovery,
    ) : SupportSQLiteOpenHelper.Callback(delegate.version) {

        override fun onCreate(db: SupportSQLiteDatabase) = delegate.onCreate(db)

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onUpgrade(db, oldVersion, newVersion)

        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onDowngrade(db, oldVersion, newVersion)

        override fun onConfigure(db: SupportSQLiteDatabase) = delegate.onConfigure(db)

        override fun onOpen(db: SupportSQLiteDatabase) = delegate.onOpen(db)

        override fun onCorruption(db: SupportSQLiteDatabase) {
            recovery.report(RecoveryState.Reason.CORRUPT_DATABASE)
            delegate.onCorruption(db)
        }
    }
}
