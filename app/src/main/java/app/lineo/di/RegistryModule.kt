package app.lineo.di

import app.lineo.registry.CalculatorModule
import app.lineo.registry.ModuleRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

/**
 * Collects every `CalculatorModule` contributed by a feature into one [ModuleRegistry]
 * (`docs/ARCHITECTURE.md` §4).
 *
 * A feature binds itself with `@Binds @IntoSet`; nothing enumerates features here, which
 * is what keeps `:feature:*` modules independent of each other.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RegistryModule {

    /**
     * Declares the multibinding so the graph resolves before any feature exists. Without
     * it, a build with no calculator module would fail to compile rather than produce an
     * empty set.
     */
    @Multibinds
    abstract fun calculatorModules(): Set<CalculatorModule>

    companion object {
        @Provides
        @Singleton
        fun moduleRegistry(modules: Set<@JvmSuppressWildcards CalculatorModule>): ModuleRegistry =
            ModuleRegistry(modules)
    }
}
