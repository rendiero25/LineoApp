package app.lineo.scientific

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.lineo.engine.AngleMode
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.registry.EditorCommand
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import app.lineo.ui.editor.EditorState
import app.lineo.ui.editor.ExpressionEditor
import app.lineo.ui.editor.LocalAngleMode
import app.lineo.ui.format.LocalNumberLocale
import app.lineo.ui.input.AccessoryRow
import app.lineo.ui.input.Keypad
import app.lineo.ui.input.LocalDecimalSeparator
import app.lineo.ui.input.rememberAccessoryRowState
import app.lineo.ui.input.rememberKeypadState
import app.lineo.ui.layout.AdaptivePane
import kotlinx.coroutines.flow.merge
import java.util.Locale

/**
 * The focused scientific surface: one expression, one result, and the function keypad.
 *
 * It computes nothing. Every key inserts a call into the line and the engine evaluates it,
 * so anything a user can reach here they can also type into the notepad — the constraint
 * `docs/ARCHITECTURE.md` §4 puts on a module screen, and the reason there is no arithmetic
 * in this file to read.
 *
 * One expression rather than a document: the notepad is where lines accumulate, and a second
 * document surface would be two places for the same work to live. Committing a line here
 * publishes the result and leaves it on screen, which is what a calculator does.
 *
 * Leaving is not handled here. Back is the host's, wired to `ModuleNav.back()` where the
 * screen is placed: intercepting it needs `activity-compose`, and a feature module taking a
 * dependency to re-implement what the host already owns would be the wrong trade
 * (`AGENTS.md` §7 — a dependency is a decision, not a convenience).
 */
@Composable
internal fun ScientificScreen(modifier: Modifier = Modifier) {
    // From the settings, never from the device configuration: the separator setting resolves
    // to a reading locale, and a screen that read the configuration would offer a comma key
    // and then report a syntax error on the comma it had just typed.
    val locale = LocalNumberLocale.current
    val angleMode = LocalAngleMode.current
    val decimalSeparator = LocalDecimalSeparator.current
    // The line, kept across a rotation and across process death. `EditorState` is not
    // saveable and a rotation recreates it, so what is saved is the two things it holds that
    // the user typed — found on a device, where turning the phone emptied the line.
    var savedText by rememberSaveable { mutableStateOf("") }
    var savedCaret by rememberSaveable { mutableStateOf(0) }
    val editor = rememberScientificEditor(locale, angleMode, decimalSeparator, savedText, savedCaret)
    // The layout is an object, so the keypad is remembered across recompositions and the
    // command stream survives them.
    val keypad = rememberKeypadState(layout = ScientificKeypadLayout)
    val accessory = rememberAccessoryRowState()
    var textInputActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val inspecting = LocalInspectionMode.current

    LaunchedEffect(editor) { editor.evaluationLoop() }

    LaunchedEffect(editor) {
        snapshotFlow { editor.text to editor.caret }.collect { (text, caret) ->
            savedText = text
            savedCaret = caret
        }
    }

    LaunchedEffect(keypad, accessory, editor) {
        merge(keypad.commands, accessory.commands).collect { command ->
            when (command) {
                EditorCommand.ToggleTextInput -> textInputActive = !textInputActive
                // = means "I am finished", so the answer appears now rather than 400 ms later.
                EditorCommand.NewLine -> editor.evaluateAndPublish()
                else -> editor.apply(command)
            }
        }
    }

    // The keyboard follows the state rather than the press, so the two cannot disagree.
    // Never in a screenshot test: there is no keyboard there, and asking for one starts a
    // platform thread layoutlib cannot give it.
    LaunchedEffect(textInputActive, inspecting) {
        if (inspecting) return@LaunchedEffect
        if (textInputActive) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    AdaptivePane(
        modifier = modifier,
        document = { ExpressionEditor(state = editor, focusRequester = focusRequester) },
        input = {
            // One surface at a time, as the single-line screen did. The chip row the notepad
            // keeps above *both* surfaces lives in `:feature:notepad`, and a feature may not
            // read another; lifting it into `:core:ui` is its own task, recorded in TASKS.md.
            if (textInputActive) AccessoryRow(state = accessory) else Keypad(state = keypad)
        },
    )
}

/**
 * An editor that can see this module's own functions.
 *
 * `EditorState` evaluates against `FunctionRegistry.BUILTIN` unless it is told otherwise,
 * and `sec` is not a built-in — the screen would offer a key whose result is
 * `UnknownIdentifier`. The registry is built from this module alone, since it is the only
 * one whose keys are on screen; `:app` builds the full one for the notepad, where every
 * module's functions have to resolve.
 */
@Composable
private fun rememberScientificEditor(
    locale: Locale,
    angleMode: AngleMode,
    decimalSeparator: Char,
    text: String,
    caret: Int,
): EditorState = remember(locale, angleMode, decimalSeparator) {
    val functions = ModuleRegistry(setOf(ScientificModule())).functionRegistry(Tier.FREE, locale)
    EditorState(
        decimalSeparator = decimalSeparator,
        // The angle mode is the user's, not this screen's: `sin(30)` has to mean here what it
        // means in the notepad, and the setting is the only thing that decides which.
        evaluate = { source ->
            Engine.evaluate(source, EvalContext(locale = locale, angleMode = angleMode, functions = functions))
        },
    ).apply { if (text.isNotEmpty()) setText(text, caret) }
}
