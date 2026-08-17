package app.lineo.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.lineo.data.db.DATABASE_NAME
import app.lineo.data.db.DatabaseRecovery
import app.lineo.data.db.LineoDatabase
import app.lineo.data.db.RecoveringOpenHelperFactory
import app.lineo.data.db.dao.DocumentDao
import app.lineo.data.db.dao.FormulaDao
import app.lineo.data.db.dao.HistoryDao
import app.lineo.data.db.dao.LineDao
import app.lineo.data.db.dao.RateDao
import app.lineo.data.repository.DocumentRepository
import app.lineo.data.repository.HistoryRepository
import app.lineo.data.repository.RateRepository
import app.lineo.data.repository.RoomDocumentRepository
import app.lineo.data.repository.RoomHistoryRepository
import app.lineo.data.repository.RoomRateRepository
import app.lineo.data.settings.DataStoreSettingsRepository
import app.lineo.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Wiring for `:core:data`.
 *
 * The bindings are interface-to-implementation only. Nothing above this module ever sees a
 * DAO or a `DataStore` — that is the rule of `docs/ANDROID_STANDARDS.md` §1, and having the
 * DAOs bound here rather than exported is what enforces it.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun documentRepository(impl: RoomDocumentRepository): DocumentRepository

    @Binds
    @Singleton
    abstract fun historyRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun rateRepository(impl: RoomRateRepository): RateRepository

    @Binds
    @Singleton
    abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {

        /**
         * The database, with corruption routed to [DatabaseRecovery].
         *
         * There is no `fallbackToDestructiveMigration`: losing documents must be the last
         * resort SQLite forces, never a build setting that quietly discards them on the
         * next version bump.
         */
        @Provides
        @Singleton
        fun database(
            @ApplicationContext context: Context,
            recovery: DatabaseRecovery,
        ): LineoDatabase = Room.databaseBuilder(context, LineoDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(
                RecoveringOpenHelperFactory(FrameworkSQLiteOpenHelperFactory(), recovery),
            )
            .build()

        @Provides
        fun documentDao(database: LineoDatabase): DocumentDao = database.documentDao()

        @Provides
        fun lineDao(database: LineoDatabase): LineDao = database.lineDao()

        @Provides
        fun historyDao(database: LineoDatabase): HistoryDao = database.historyDao()

        @Provides
        fun formulaDao(database: LineoDatabase): FormulaDao = database.formulaDao()

        @Provides
        fun rateDao(database: LineoDatabase): RateDao = database.rateDao()

        @Provides
        @Singleton
        fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create {
                context.preferencesDataStoreFile(SETTINGS_NAME)
            }

        /** Injected rather than read statically, so time is substitutable in tests. */
        @Provides
        @Singleton
        fun clock(): Clock = Clock.systemUTC()

        private const val SETTINGS_NAME = "settings"
    }
}
