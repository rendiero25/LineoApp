package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.lineo.registry.EditorCommand
import app.lineo.ui.input.AccessoryRow
import app.lineo.ui.input.Keypad
import app.lineo.ui.input.rememberAccessoryRowState
import app.lineo.ui.input.rememberKeypadState
import app.lineo.ui.layout.AdaptivePane
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression
import kotlinx.coroutines.flow.merge

/**
 * The smallest thing that can prove the input surfaces work on a device.
 *
 * P0-13 has to be verified against real keyboards — GBoard, SwiftKey, Samsung Keyboard —
 * and against the `ime` inset they each report differently. None of that can be checked
 * without a focusable field to raise a keyboard over, and the editor that will own one is
 * P0-14. So this exists: a text field with no evaluation, no line model, and no error
 * state, wired to whichever surface is attached.
 *
 * **Delete this file at P0-14.** Every line of it is answered by the real editor.
 */
@Composable
internal fun Phase0InputHarness() {
    val keypad = rememberKeypadState()
    val accessory = rememberAccessoryRowState()
    var text by rememberSaveable { mutableStateOf("") }
    var textInputActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(keypad, accessory) {
        merge(keypad.commands, accessory.commands).collect { command ->
            when (command) {
                is EditorCommand.InsertText -> text += command.text
                is EditorCommand.InsertFunction -> text += "${command.name}("
                EditorCommand.Backspace -> text = text.dropLast(1)
                EditorCommand.NewLine -> text += "\n"
                EditorCommand.ToggleTextInput -> {
                    textInputActive = !textInputActive
                    if (textInputActive) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    } else {
                        keyboard?.hide()
                    }
                }
                // The editor owns the caret; this harness has none to move or wrap.
                is EditorCommand.MoveCursor, is EditorCommand.WrapSelection -> Unit
            }
        }
    }

    AdaptivePane(
        document = {
            HarnessDocument(
                text = text,
                onTextChange = { text = it },
                focusRequester = focusRequester,
            )
        },
        input = {
            // The accessory row replaces the keypad rather than stacking above it: the
            // system keyboard already occupies the space the keypad would want.
            if (textInputActive) {
                AccessoryRow(state = accessory)
            } else {
                Keypad(state = keypad)
            }
        },
    )
}

@Composable
private fun HarnessDocument(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val editor = RoleColors.of(LineoRole.Editor)
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxSize()
            .background(editor.container)
            .padding(LineoDimens.EditorPadding)
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.displaySmall.asExpression().copy(color = editor.content),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
    )
}
