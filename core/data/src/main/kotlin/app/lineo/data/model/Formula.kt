package app.lineo.data.model

/**
 * A user-defined formula, the premium feature of `TASKS.md` P3-01.
 *
 * Stored in Phase 0 so that the table and its [schemaVersion] exist before anything
 * writes to it — adding a table to a shipped database costs a migration, adding one now
 * costs nothing.
 *
 * [params] are the parameter names in declaration order, so `area(w, h)` stores
 * `["w", "h"]`.
 */
data class Formula(
    val id: Long,
    val name: String,
    val source: String,
    val params: List<String>,
    val schemaVersion: Int = CURRENT_FORMULA_SCHEMA,
)

/** The formula format this build writes. Independent of the database version. */
const val CURRENT_FORMULA_SCHEMA: Int = 1
