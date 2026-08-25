package app.lineo.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the connection to the billing service and tracks the user's entitlements.
 */
interface BillingRepository {
    /** The current entitlement tier. */
    val entitlement: StateFlow<Entitlement>

    /** Refreshes the purchase history. */
    suspend fun refresh()
}
