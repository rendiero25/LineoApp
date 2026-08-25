package app.lineo.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The switch between the keypad and the text keyboard, hoisted out of both surfaces.
 *
 * The switch used to float above the keypad grid, which put it inside the input pane and so
 * out of reach of anything above the document. It now sits in the top bar, where a control
 * that changes the instrument reads as a mode rather than as a key — and where it stays in
 * one place while the surface below it is replaced.
 *
 * The top bar is an ancestor of the screen, so the state cannot live in the screen: an
 * ancestor cannot read what a descendant holds. It lives here, is created by the shell, and
 * is provided through [LocalInputModeToggle]. A screen *binds* to it — publishes whether the
 * text keyboard is up, and says what pressing the button should do — because which command
 * that is and where it goes is the screen's knowledge and not the shell's.
 *
 * Unbound is a real state: the history and settings screens have no input surface, and the
 * button is not drawn for them.
 */
@Stable
class InputModeToggle {

    /** Whether the text keyboard is the surface right now. Published by the bound screen. */
    var textInputActive: Boolean by mutableStateOf(false)
        private set

    private var onToggle: (() -> Unit)? by mutableStateOf(null)

    /**
     * Who bound last. The screen being left releases the switch only if it still holds it.
     *
     * Navigation composes the arriving screen before it disposes the departing one, so an
     * unconditional release ran *after* the new screen had bound and left the switch owned by
     * nobody — the button vanished on every module screen. Found on a device, opening the
     * scientific module from the notepad.
     */
    private var owner: Any? = null

    /** Whether a screen is bound. False on a screen with no input surface, where nothing is drawn. */
    val bound: Boolean get() = onToggle != null

    /** Presses the switch. Does nothing when no screen is bound. */
    fun toggle() {
        onToggle?.invoke()
    }

    internal fun bind(owner: Any, onToggle: () -> Unit) {
        this.owner = owner
        this.onToggle = onToggle
    }

    internal fun publish(owner: Any, textInputActive: Boolean) {
        if (this.owner !== owner) return
        this.textInputActive = textInputActive
    }

    internal fun unbind(owner: Any) {
        if (this.owner !== owner) return
        this.owner = null
        onToggle = null
        textInputActive = false
    }
}

/**
 * The toggle the shell created, for whoever is drawing the button and whoever owns the state.
 *
 * Static: it is one instance for the life of the window, and what changes inside it is
 * observable state of its own.
 */
val LocalInputModeToggle = staticCompositionLocalOf { InputModeToggle() }

/**
 * Binds a screen to the top bar's switch for as long as that screen is composed.
 *
 * @param textInputActive whether the text keyboard is the surface. Published upwards, so the
 *   button can say which way it is pointing.
 * @param onToggle what a press means on this screen — for both of them, the same
 *   `EditorCommand.ToggleTextInput` the keypad key used to emit, into the same stream. Read
 *   through the latest composition, so a screen may pass a lambda without remembering it.
 */
@Composable
fun InputModeBinding(textInputActive: Boolean, onToggle: () -> Unit) {
    val toggle = LocalInputModeToggle.current
    val latest by rememberUpdatedState(onToggle)
    // Identity, and nothing else: it says which screen's binding this is, so the one leaving
    // cannot release a switch the one arriving has already taken.
    val owner = remember { Any() }
    DisposableEffect(toggle, owner) {
        toggle.bind(owner) { latest() }
        onDispose { toggle.unbind(owner) }
    }
    SideEffect { toggle.publish(owner, textInputActive) }
}
