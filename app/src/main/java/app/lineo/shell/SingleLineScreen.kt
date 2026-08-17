package app.lineo.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.lineo.registry.EditorCommand
import app.lineo.ui.editor.EditorState
import app.lineo.ui.editor.ExpressionEditor
import app.lineo.ui.input.AccessoryRow
import app.lineo.ui.input.Keypad
import app.lineo.ui.input.rememberAccessoryRowState
import app.lineo.ui.input.rememberKeypadState
import app.lineo.ui.layout.AdaptivePane
import kotlinx.coroutines.flow.merge

/**
 * One line, evaluated end to end — the Phase 0 exit criterion.
 *
 * It wires the three pieces together and does nothing else: the editor owns the text, the
 * surfaces own what a press means, and this decides only which surface is attached. There
 * is no document, no line list, and no persistence; `:feature:notepad` brings those at
 * P1-01 and P1-03, and replaces this file when it does.
 */
@Composable
internal fun SingleLineScreen() {
    val editor = remember { EditorState() }
    val keypad = rememberKeypadState()
    val accessory = rememberAccessoryRowState()
    var textInputActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(editor) { editor.evaluationLoop() }

    LaunchedEffect(keypad, accessory, editor) {
        merge(keypad.commands, accessory.commands).collect { command ->
            when (command) {
                EditorCommand.ToggleTextInput -> {
                    textInputActive = !textInputActive
                    if (textInputActive) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    } else {
                        keyboard?.hide()
                    }
                }
                // = means "I am finished", so the answer appears now rather than 400 ms later.
                EditorCommand.NewLine -> editor.evaluateAndPublish()
                else -> editor.apply(command)
            }
        }
    }

    AdaptivePane(
        document = { ExpressionEditor(state = editor, focusRequester = focusRequester) },
        input = {
            if (textInputActive) {
                AccessoryRow(state = accessory)
            } else {
                Keypad(state = keypad)
            }
        },
    )
}
