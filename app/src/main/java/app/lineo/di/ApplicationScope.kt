package app.lineo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * A scope that lives as long as the process, for work that must finish even though the thing
 * that started it is gone.
 *
 * There is exactly one such piece of work in Lineo: the final write of the notepad when the
 * screen stops. A `viewModelScope` is cancelled the moment the activity is finished, which on
 * a back press is the same moment the write is asked for — so the write would be cancelled by
 * the very event that makes it necessary.
 *
 * Not a general escape hatch. Anything launched here has no owner to cancel it, which is why
 * it is a qualifier rather than an injectable `CoroutineScope` anyone can ask for by type.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
