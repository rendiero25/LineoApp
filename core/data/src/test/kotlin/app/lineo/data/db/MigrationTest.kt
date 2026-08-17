package app.lineo.data.db

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The migration harness, running against version 1.
 *
 * There is nothing to migrate yet, and that is the point: this test proves the exported
 * schema is committed, is readable, and matches the entities. Without it, the first real
 * migration would be written against a schema nobody had ever validated.
 *
 * `runMigrationsAndValidate` compares every column, index, and foreign key against
 * `core/data/schemas/…/1.json`, so a change to an entity that forgets to bump
 * [DATABASE_VERSION] fails here rather than on a device.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val file = File(
        ApplicationProvider.getApplicationContext<Context>().cacheDir,
        "migration-test.db",
    )

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = file,
        driver = AndroidSQLiteDriver(),
        databaseClass = LineoDatabase::class,
    )

    @Test
    fun `version 1 schema is exported and matches the entities`() {
        helper.createDatabase(DATABASE_VERSION).close()

        val connection = helper.runMigrationsAndValidate(DATABASE_VERSION, emptyList())

        assertEquals(DATABASE_VERSION.toLong(), connection.readLong("PRAGMA user_version"))
        connection.close()
    }

    @Test
    fun `every table of ARCHITECTURE section 6 exists at version 1`() {
        val connection = helper.createDatabase(DATABASE_VERSION)

        val tables = connection.readTexts("SELECT name FROM sqlite_master WHERE type = 'table'")

        assertTrue(
            tables.toString(),
            tables.containsAll(listOf("documents", "lines", "history", "formulas", "rate_cache")),
        )
        connection.close()
    }

    @Test
    fun `lines are indexed by document`() {
        val connection = helper.createDatabase(DATABASE_VERSION)

        val indexes = connection.readTexts(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'lines'",
        )

        // docs/ANDROID_STANDARDS.md §4: reading a document must not scan the table.
        assertTrue(indexes.toString(), indexes.any { it.contains("documentId") })
        connection.close()
    }

    @Test
    fun `deleting a document takes its lines with it`() {
        val connection = helper.createDatabase(DATABASE_VERSION)
        connection.execSQL("PRAGMA foreign_keys = ON")
        connection.execSQL(
            "INSERT INTO documents (id, title, schemaVersion, createdAt, updatedAt, sortIndex) " +
                "VALUES (1, 'x', 1, 0, 0, 0)",
        )
        connection.execSQL(
            "INSERT INTO lines (id, documentId, ordinal, source, label) VALUES (7, 1, 0, '1+1', NULL)",
        )

        connection.execSQL("DELETE FROM documents WHERE id = 1")

        assertEquals(0L, connection.readLong("SELECT COUNT(*) FROM lines"))
        connection.close()
    }

    private fun SQLiteConnection.readLong(sql: String): Long = prepare(sql).use { statement ->
        statement.step()
        statement.getLong(0)
    }

    private fun SQLiteConnection.readTexts(sql: String): List<String> =
        prepare(sql).use { statement ->
            buildList {
                while (statement.step()) add(statement.getText(0))
            }
        }
}
