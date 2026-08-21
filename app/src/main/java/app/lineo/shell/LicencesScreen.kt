package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import app.lineo.R
import app.lineo.licenses.LicenceTexts
import app.lineo.licenses.LicensedDependency
import app.lineo.licenses.attributedDependencies
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors

/**
 * What ships inside Lineo, and the licences it ships under (P1-07, P1-11).
 *
 * Apache-2.0 §4 puts this obligation on the **binary**, not on the repository: a copy of the
 * licence has to travel with the app, so naming a licence is not enough — its full text opens
 * from here. The list is generated at build time from the enforced allowlist, so it cannot
 * drift from what the APK actually contains.
 *
 * Tapping a row opens the licence text; tapping it again closes it. No second screen, because
 * there is nothing to do on one: the text is the whole content.
 */
@Composable
internal fun LicencesScreen(
    modifier: Modifier = Modifier,
    dependencies: List<LicensedDependency> = attributedDependencies,
) {
    var openFamily: String? by remember { mutableStateOf(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RoleColors.of(LineoRole.Editor).container),
        contentPadding = PaddingValues(LineoDimens.EditorPadding),
        verticalArrangement = Arrangement.spacedBy(LineoDimens.Grid),
    ) {
        items(dependencies, key = { it.module }) { dependency ->
            val family = dependency.licence.substringBefore(' ')
            DependencyRow(
                dependency = dependency,
                expanded = openFamily == family,
                onClick = { openFamily = if (openFamily == family) null else family },
            )
        }
    }
}

/** One library: its coordinate, its licence, and — when open — the licence in full. */
@Composable
private fun DependencyRow(dependency: LicensedDependency, expanded: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = LineoDimens.Grid),
    ) {
        Text(
            text = dependency.module,
            style = MaterialTheme.typography.bodyLarge,
            color = RoleColors.of(LineoRole.Editor).content,
        )
        Text(
            text = dependency.licence,
            style = MaterialTheme.typography.bodySmall,
            color = RoleColors.of(LineoRole.Result).content,
        )
        if (expanded) {
            LicenceText(family = dependency.licence.substringBefore(' '))
        }
    }
}

/**
 * The full text of one licence family.
 *
 * Read from `res/raw` here rather than carried in state: it is thousands of words, it is
 * needed only while a row is open, and this is the one place in the app with a `Context` in
 * reach for the right reason (`docs/ANDROID_STANDARDS.md` §1 keeps them out of the layers
 * above, not out of a composable reading its own resources).
 */
@Composable
private fun LicenceText(family: String) {
    val resources = LocalResources.current
    val resource = LicenceTexts.resourceFor(family)
    val text = remember(family, resources) {
        // A family with no bundled text is a build error `AttributionTest` already rules out,
        // so this renders a sentence rather than pretending the licence does not exist.
        resource?.let { resources.openRawResource(it).bufferedReader().use { reader -> reader.readText() } }
    }
    // No scroller of its own: the row simply grows and the list scrolls. A vertical scroller
    // inside a LazyColumn is measured with an infinite height and crashes.
    Text(
        text = text ?: stringResource(R.string.licences_missing_text, family),
        style = MaterialTheme.typography.bodySmall,
        color = RoleColors.of(LineoRole.Editor).content,
        modifier = Modifier.padding(top = LineoDimens.Grid),
    )
}
