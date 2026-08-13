package app.lineo.engine

/**
 * Result of an engine operation.
 *
 * `docs/ARCHITECTURE.md` §3 writes this as `Result<Quantity, CalcError>`. Kotlin's own
 * `Result` takes a single type parameter and carries a `Throwable`, which is exactly the
 * shape this engine must not use — errors here are values, never exceptions.
 */
sealed interface CalcResult<out T> {
    data class Ok<out T>(val value: T) : CalcResult<T>

    data class Err(val error: CalcError) : CalcResult<Nothing>

    val isOk: Boolean get() = this is Ok

    /** The value, or `null` when this is an error. */
    fun valueOrNull(): T? = (this as? Ok)?.value

    /** The error, or `null` when this is a value. */
    fun errorOrNull(): CalcError? = (this as? Err)?.error
}

inline fun <T, R> CalcResult<T>.map(transform: (T) -> R): CalcResult<R> = when (this) {
    is CalcResult.Ok -> CalcResult.Ok(transform(value))
    is CalcResult.Err -> this
}

inline fun <T, R> CalcResult<T>.flatMap(transform: (T) -> CalcResult<R>): CalcResult<R> = when (this) {
    is CalcResult.Ok -> transform(value)
    is CalcResult.Err -> this
}

fun <T> T.ok(): CalcResult<T> = CalcResult.Ok(this)

fun CalcError.err(): CalcResult<Nothing> = CalcResult.Err(this)
