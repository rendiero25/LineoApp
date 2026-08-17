package app.lineo.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.lineo.engine.CalcError
import app.lineo.ui.R

/**
 * Words for an error.
 *
 * Exhaustive on purpose: the `when` has no `else`, so a new `CalcError` variant fails to
 * compile here rather than reaching a user as a blank message. `docs/CONVENTIONS.md` §8
 * requires error state to carry text, and text nobody wrote is not text.
 *
 * The messages name what is wrong, not what the parser was doing. "Unexpected ')'" is
 * something a person can act on; "expected primary, found RPAREN" is not.
 */
@Composable
@ReadOnlyComposable
fun calcErrorMessage(error: CalcError): String = when (error) {
    is CalcError.Syntax -> stringResource(R.string.error_syntax, error.token)
    is CalcError.UnbalancedParen ->
        pluralStringResource(R.plurals.error_unbalanced_paren, error.missing, error.missing)
    is CalcError.UnknownIdentifier -> stringResource(R.string.error_unknown_identifier, error.name)
    is CalcError.UnitMismatch -> stringResource(R.string.error_unit_mismatch, error.left.symbol, error.right.symbol)
    is CalcError.DomainError -> stringResource(R.string.error_domain, error.fn)
    is CalcError.CircularReference -> stringResource(R.string.error_circular_reference)
    CalcError.DivisionByZero -> stringResource(R.string.error_division_by_zero)
    is CalcError.Overflow -> stringResource(R.string.error_overflow)
    is CalcError.RateUnavailable -> stringResource(R.string.error_rate_unavailable, error.from, error.to)
}
