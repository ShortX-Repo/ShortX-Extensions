package tornaco.apps.shortx.ext.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tornaco.apps.shortx.ext.R
import tornaco.apps.shortx.ext.ui.DASelectorDialog
import tornaco.apps.shortx.ext.ui.rememberDADialogState
import tornaco.apps.shortx.ui.base.ListItem

@Composable
fun ExportDAShortcut() {
    val daSelectorState = rememberDADialogState {

    }
    DASelectorDialog(state = daSelectorState)
    ListItem(
        title = stringResource(id = R.string.export_rule_as_apk),
        onClick = {
            daSelectorState.show()
        }
    )
}