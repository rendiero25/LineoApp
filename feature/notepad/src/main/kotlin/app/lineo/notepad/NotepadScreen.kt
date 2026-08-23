package app.lineo.notepad

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lineo.registry.EditorCommand
import app.lineo.ui.input.Keypad
import app.lineo.ui.input.rememberAccessoryRowState
import app.lineo.ui.input.rememberKeypadState
import app.lineo.ui.layout.AdaptivePane
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import kotlinx.coroutines.flow.merge

/**
 * The notepad: a document of lines, each with its own result, over whichever surface the
 * user is typing with.
 *
 * Stateless in the Compose sense. Everything it shows comes from [NotepadState.uiState] and
 * every edit leaves through `EditorCommand`, so the keypad, the system keyboard and a
 * suggestion chip all reach the document by one path and cannot each edit it their own way
 * (`docs/ARCHITECTURE.md` §5).
 *
 * The surface swap changes what is *below* the chip row and nothing else: the row is there
 * either way, at the same height, so pressing `ABC` does not move the document.
 *
 * @param state the screen's model. Held by whatever owns the open document — a `ViewModel`
 *   at screen level, per `docs/ANDROID_STANDARDS.md` §1 — and never created here, or a
 *   configuration change would open an empty notepad.
 */
@Composable
fun NotepadScreen(state: NotepadState, modifier: Modifier = Modifier) {
    val uiState by state.uiState.collectAsStateWithLifecycle()
    val keypad = rememberKeypadState()
    val accessory = rememberAccessoryRowState()
    val keyboard = LocalSoftwareKeyboardController.current

    // Both surfaces are collected at once, and neither is detached when the other is shown.
    // A press that arrived while the surfaces were being swapped is still the user's input.
    LaunchedEffect(state, keypad, accessory) {
        merge(keypad.commands, accessory.commands).collect(state::apply)
    }

    // The keyboard follows the state rather than the press, so the two cannot disagree: the
    // context switch of §5 raises it as well, and it has no key to have been pressed. The
    // focus request that goes with it belongs to the line, which is the only thing that
    // knows whether its field is currently composed.
    // Not in a preview or a screenshot test, where there is no keyboard to raise and asking
    // for one starts a platform thread layoutlib cannot give it (`Thread.setPosixNicenessInternal`).
    val inspecting = LocalInspectionMode.current
    LaunchedEffect(uiState.textInputActive, inspecting) {
        if (inspecting) return@LaunchedEffect
        if (uiState.textInputActive) keyboard?.show() else keyboard?.hide()
    }

    // A hardware keyboard types into the document even when no line holds the caret (P1-08b).
    // `onKeyEvent` and not `onPreviewKeyEvent`: a focused line's field gets first refusal, and
    // only what it does not want arrives here. Found on a foldable — with the keypad showing,
    // typing `12+3` reached whatever had view focus and opened the settings screen instead.
    val typing = remember { FocusRequester() }
    LaunchedEffect(inspecting) { if (!inspecting) typing.requestFocus() }

    AdaptivePane(
        modifier = modifier
            .focusRequester(typing)
            .focusable()
            .onKeyEvent { event -> state.handleHardwareKey(event) },
        document = {
            NotepadLines(state = state, uiState = uiState)
        },
        input = {
            Column(modifier = Modifier.fillMaxWidth()) {
                NotepadChipRow(
                    // The surface switch is only shown when the keyboard is up. The keypad
                    // carries its own `ABC`, and two keys doing the same thing on one screen
                    // is two places to look for it.
                    keys = accessory.keys.filter {
                        uiState.textInputActive || it.role != LineoRole.InputSwitch
                    },
                    suggestions = uiState.suggestions,
                    docked = uiState.textInputActive,
                    onCommand = state::apply,
                )
                if (!uiState.textInputActive) Keypad(state = keypad)
            }
        },
    )
}

/**
 * The document itself.
 *
 * Lazy and keyed by [app.lineo.engine.LineId], not by position: a line inserted above line
 * three must not make every line below it a different item, or the focused field would be
 * torn down and rebuilt on every Enter.
 */
@Composable
private fun NotepadLines(
    state: NotepadState,
    uiState: NotepadUiState,
) {
    val listState = rememberLazyListState()
    val focusedIndex = uiState.lines.indexOfFirst { it.id == uiState.focused }

    // Only when the caret is not already on screen. Scrolling every time focus moves would
    // drag the document under a user who tapped a line they could see perfectly well.
    LaunchedEffect(focusedIndex, uiState.lines.size) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (focusedIndex < 0 || visible.any { it.index == focusedIndex }) return@LaunchedEffect
        // Nothing laid out yet means the document has just been opened on a line further
        // down. There is no journey to animate, because the user has not seen the top.
        if (visible.isEmpty()) {
            listState.scrollToItem(focusedIndex)
        } else {
            listState.animateScrollToItem(focusedIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = LineoDimens.LineGap),
    ) {
        items(items = uiState.lines, key = { it.id.value }) { line ->
            NotepadLineRow(
                line = line,
                caret = uiState.caret.takeIf { line.id == uiState.focused },
                actions = NotepadLineActions(
                    focus = { state.focus(line.id) },
                    setText = { text, caret ->
                        // A keystroke can only reach a line the system keyboard is in, and
                        // the caret follows the field. Focusing first keeps the state's idea
                        // of where the caret is and the field's from parting company.
                        if (line.id != uiState.focused) state.focus(line.id)
                        state.setText(text, caret)
                    },
                    // The keyboard's return key means what `=` means on the keypad: this line
                    // is finished, start the next one.
                    newLine = { state.apply(EditorCommand.NewLine) },
                    applySuggestion = { error -> state.applySuggestion(line.id, error) },
                    ordinalOf = { id -> uiState.lines.firstOrNull { it.id == id }?.ordinal },
                    // Up and down on a hardware keyboard walk the document (P1-08b).
                    moveFocusBy = state::moveFocusBy,
                ),
                keyboardWanted = uiState.textInputActive,
            )
        }
    }
}

/**
 * A hardware key that no field wanted, applied to the document. Returns whether it was ours.
 *
 * The arrows are handled here as well as in the focused line's field, and they have to be:
 * with the keypad showing there is no field at all, and an arrow that reached the platform
 * moved *view* focus onto a key instead — after which the next character typed went to a
 * button rather than to the document. Found on a foldable.
 *
 * Left and right move the caret rather than the line, which is what they do in any editor
 * and what `MoveCursor` already means to every other surface.
 */
private fun NotepadState.handleHardwareKey(event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        // Consumed at the edges too: an arrow handed back moves view focus onto the overflow
        // button, and the next character typed lands on a button instead of on a line.
        Key.DirectionUp -> {
            moveFocusBy(-1)
            true
        }

        Key.DirectionDown -> {
            moveFocusBy(1)
            true
        }

        Key.DirectionLeft -> applied(EditorCommand.MoveCursor(-1))
        Key.DirectionRight -> applied(EditorCommand.MoveCursor(1))
        else -> event.asEditorCommand()?.let { applied(it) } ?: false
    }
}

/** Applies [command] and says it was handled, so a `when` branch reads as one thing. */
private fun NotepadState.applied(command: EditorCommand): Boolean {
    apply(command)
    return true
}

/**
 * What a hardware key means to the document, or `null` for a key that is not ours.
 *
 * The keypad is the model: every key here maps to the same [EditorCommand] the equivalent
 * key on screen sends, so a keyboard and a thumb reach the document by one path
 * (`docs/ARCHITECTURE.md` §5) and the two cannot drift.
 *
 * Key *down* only, so a held key repeats the way the platform repeats it. `=` types a
 * character rather than finishing the line: in a notepad it defines a name
 * (`docs/GRAMMAR.md` §3.5), and the return key is what finishes a line.
 */
private fun KeyEvent.asEditorCommand(): EditorCommand? {
    if (type != KeyEventType.KeyDown) return null
    return when (key) {
        Key.Enter, Key.NumPadEnter -> EditorCommand.NewLine
        Key.Backspace -> EditorCommand.Backspace
        else -> utf16CodePoint.toChar().takeIf { it.isTypable() }?.let { EditorCommand.InsertText(it.toString()) }
    }
}

/**
 * Whether a character belongs in an expression.
 *
 * Letters and digits for names and numbers, and the punctuation the grammar reads. A space
 * is included because `5 km` needs one; a control character is not, which is what rules out
 * tab, escape and the arrows that got this far without a code point.
 */
internal fun Char.isTypable(): Boolean = isLetterOrDigit() || this in TYPABLE_PUNCTUATION

private const val TYPABLE_PUNCTUATION = "+-*/^%()., ;=<>!°"
