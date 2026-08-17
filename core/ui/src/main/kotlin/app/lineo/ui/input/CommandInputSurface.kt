package app.lineo.ui.input

import app.lineo.registry.EditorCommand
import app.lineo.registry.InputSurface
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Base for anything the user types with: holds the command stream and nothing else.
 *
 * A plain state holder rather than a `ViewModel`. `docs/ANDROID_STANDARDS.md` §1 puts
 * ViewModels at screen level only — a keypad is a component, and one that owned a
 * ViewModel could not be placed twice on a tablet, previewed, or snapshot.
 *
 * The flow is hot and buffered. A key press is an action the user already took; it must
 * not be lost because the editor happened not to be collecting for a frame, and it must
 * not suspend the press either. [emit] therefore never blocks and never fails.
 */
abstract class CommandInputSurface : InputSurface {

    private val mutableCommands = MutableSharedFlow<EditorCommand>(
        extraBufferCapacity = COMMAND_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val commands: Flow<EditorCommand> = mutableCommands.asSharedFlow()

    /**
     * Publishes [command] to whoever is collecting.
     *
     * Protected rather than public: a surface decides for itself what a press means. A
     * caller that could emit arbitrary commands would make the surface a pass-through and
     * put the mapping back in the UI, which is what `docs/ARCHITECTURE.md` §5 moves out.
     */
    protected fun emit(command: EditorCommand) {
        mutableCommands.tryEmit(command)
    }

    private companion object {
        /**
         * Room for a burst of presses between two collections. Deep enough that fast typing
         * survives a slow frame; shallow enough that a surface with no collector cannot grow
         * a backlog of stale input.
         */
        const val COMMAND_BUFFER = 32
    }
}
