package app.lineo.data.model

import java.time.Instant

/**
 * A notepad document: a title and an ordered list of [Line]s.
 *
 * [schemaVersion] is the version of the *content* format, not of the database
 * (`AGENTS.md` §2). The two move independently: a Room migration changes the table, a
 * schema version change means the meaning of `Line.source` changed and old rows may need
 * rewriting. A document is always written with [CURRENT_DOCUMENT_SCHEMA].
 */
data class Document(
    val id: Long,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val sortIndex: Int,
    val schemaVersion: Int = CURRENT_DOCUMENT_SCHEMA,
)

/** The content format this build writes. Bump it when the meaning of a line's source changes. */
const val CURRENT_DOCUMENT_SCHEMA: Int = 1
