package app.lineo.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * `docs/ARCHITECTURE.md` §6: a database that cannot be read enters recovery mode; it never
 * crashes.
 *
 * The test writes bytes that are not a SQLite file where the database belongs, which is
 * what a truncated write or failing storage leaves behind, and then uses the database
 * normally.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseRecoveryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var recovery: DatabaseRecovery
    private lateinit var database: LineoDatabase

    @Before
    fun setUp() {
        recovery = DatabaseRecovery()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun `a healthy database reports no recovery`() = runTest {
        database = open()

        database.documentDao().insertDocument(document())

        assertEquals(RecoveryState.Healthy, recovery.state.value)
    }

    @Test
    fun `a corrupt database is replaced instead of crashing`() = runTest {
        // Create the file first, so the path and its parent directory exist exactly as
        // they would on a device, then destroy its contents.
        open().use { it.documentDao().insertDocument(document()) }
        context.getDatabasePath(TEST_DB).writeBytes(ByteArray(NOT_A_DATABASE) { 0x7A })

        database = open()
        val documents = database.lineDao().lines(documentId = 1)

        assertEquals(
            RecoveryState.Recovered(RecoveryState.Reason.CORRUPT_DATABASE),
            recovery.state.value,
        )
        assertTrue(documents.isEmpty())
    }

    private fun open(): LineoDatabase =
        Room.databaseBuilder(context, LineoDatabase::class.java, TEST_DB)
            .openHelperFactory(
                RecoveringOpenHelperFactory(FrameworkSQLiteOpenHelperFactory(), recovery),
            )
            .allowMainThreadQueries()
            .build()

    private fun document() = app.lineo.data.db.entity.DocumentEntity(
        title = "scratch",
        schemaVersion = 1,
        createdAt = 0,
        updatedAt = 0,
        sortIndex = 0,
    )

    private inline fun <T : LineoDatabase, R> T.use(block: (T) -> R): R =
        try {
            block(this)
        } finally {
            close()
        }

    private companion object {
        const val TEST_DB = "recovery-test.db"
        const val NOT_A_DATABASE = 4096
    }
}
