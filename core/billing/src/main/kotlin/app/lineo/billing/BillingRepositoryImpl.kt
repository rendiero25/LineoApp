package app.lineo.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BillingRepositoryImpl @Inject constructor() : BillingRepository {
    private val _entitlement = MutableStateFlow(Entitlement.Free)
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    override suspend fun refresh() {
        // Placeholder for Play Billing integration
    }
}
