package app.lineo.di

import app.lineo.registry.CalculatorModule
import app.lineo.scientific.ScientificModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * The calculator modules this build contains (`docs/ARCHITECTURE.md` §4).
 *
 * One `@Provides @IntoSet` per feature, and this is the only file that names them — a
 * feature never learns about another, and `ModuleRegistry` sees whatever set is bound.
 *
 * Provided rather than `@Binds`, so the feature class carries no injection annotation and
 * `:feature:scientific` keeps its pure-JVM tests and no Dagger processor
 * (the same reason `NotepadModule` provides the store).
 */
@Module
@InstallIn(SingletonComponent::class)
object CalculatorModulesModule {

    @Provides
    @Singleton
    @IntoSet
    fun scientificModule(): CalculatorModule = ScientificModule()
}
