package app.lineo.shell

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.lineo.R
import app.lineo.data.model.HistoryEntry
import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.UserSettings
import app.lineo.licenses.LicensedDependency
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * What a screen reader is handed by the screens `:app` owns (P1-08).
 *
 * On a device rather than in a snapshot, because none of this is visible: a picture cannot
 * show that a chip publishes its selected state, that a tape row is one node rather than
 * two, or that a licence row says which way it is facing. The semantics tree can, and it is
 * exactly the tree TalkBack walks.
 *
 * These do not replace a human with TalkBack — they assert the labels are there and the tree
 * has the shape intended, not that the result is pleasant to listen to.
 */
class ShellSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aChipSaysWhichOptionItIsAndWhetherItIsChosen() {
        compose.setContent {
            LineoTheme(dynamicColor = false) {
                SettingsScreen(settings = UserSettings(separator = SeparatorPreference.COMMA), actions = NO_ACTIONS)
            }
        }

        // "1,5" alone says nothing out loud, so each chip is read as an answer to the
        // question its title asked.
        val title = context.getString(R.string.settings_separator)
        val comma = context.getString(R.string.settings_separator_comma)
        val auto = context.getString(R.string.settings_separator_auto)

        compose.onNodeWithContentDescription("$title: $comma").assertIsSelected()
        compose.onNodeWithContentDescription("$title: $auto").assertIsNotSelected()
    }

    @Test
    fun choosingAnOptionReportsIt() {
        var chosen: SeparatorPreference? = null
        compose.setContent {
            LineoTheme(dynamicColor = false) {
                SettingsScreen(
                    settings = UserSettings(),
                    actions = NO_ACTIONS.copy(onSeparator = { chosen = it }),
                )
            }
        }

        val title = context.getString(R.string.settings_separator)
        compose.onNodeWithContentDescription("$title: ${context.getString(R.string.settings_separator_dot)}")
            .performClick()

        assert(chosen == SeparatorPreference.DOT) { "the chip reported $chosen" }
    }

    @Test
    fun aTapeRowIsOneNodeWithATapThatSaysWhatItDoes() {
        compose.setContent {
            LineoTheme(dynamicColor = false) {
                HistoryScreen(entries = TAPE, onReuse = {}, onClear = {})
            }
        }

        // One node: the expression and its result are one calculation, not two stops.
        val row = compose.onAllNodes(hasClickAction()).onFirst()
        row.assert(hasClickLabel(context.getString(R.string.history_reuse_action)))
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, TAPE_TEXTS))
    }

    @Test
    fun aLicenceRowSaysWhichWayItIsFacing() {
        compose.setContent {
            LineoTheme(dynamicColor = false) { LicencesScreen(dependencies = LIBRARIES) }
        }

        val hidden = context.getString(R.string.licences_state_hidden)
        val shown = context.getString(R.string.licences_state_shown)
        val row = compose.onNodeWithText(LIBRARIES.first().module)

        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, hidden))
        row.assert(hasClickLabel(context.getString(R.string.licences_show_text)))

        row.performClick()

        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, shown))
        row.assert(hasClickLabel(context.getString(R.string.licences_hide_text)))
    }

    /**
     * A tap whose label finishes TalkBack's "double tap to" sentence.
     *
     * There is no shipped matcher for it, and the label is the whole point: "activate" says
     * nothing about what activating does.
     */
    private fun hasClickLabel(label: String) = SemanticsMatcher("click label is \"$label\"") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    private companion object {

        val NO_ACTIONS = SettingsActions(
            onSeparator = {},
            onAngleMode = {},
            onTheme = {},
            onUnitSystem = {},
            onDecimalPlaces = {},
            onDynamicColor = {},
            onOpenLicences = {},
        )

        val TAPE = listOf(
            HistoryEntry(id = 1, expression = "6 * 7", resultText = "42", createdAt = Instant.EPOCH),
        )

        /** Both texts of the row, in the order the merged node holds them. */
        val TAPE_TEXTS = listOf(
            androidx.compose.ui.text.AnnotatedString("6 * 7"),
            androidx.compose.ui.text.AnnotatedString("42"),
        )

        val LIBRARIES = listOf(
            LicensedDependency(module = "androidx.activity:activity", licence = "Apache-2.0"),
        )
    }
}
