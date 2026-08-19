package app.lineo.di

import app.lineo.data.repository.DocumentRepository
import app.lineo.notepad.NotepadStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wiring for the notepad.
 *
 * The store is a plain class over the `:core:data` contract, so it is provided here rather
 * than annotated in `:feature:notepad` — a feature that carried Hilt annotations would drag
 * the Dagger processor into a module whose tests are pure JVM.
 */
@Module
@InstallIn(SingletonComponent::class)
object NotepadModule {

    @Provides
    @Singleton
    fun notepadStore(documents: DocumentRepository): NotepadStore = NotepadStore(documents)
}
