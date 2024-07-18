package tornaco.apps.shortx.ext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import tornaco.apps.shortx.core.res.Remix
import tornaco.apps.shortx.core.shortXManager
import tornaco.apps.shortx.ext.api.cv.ShortXCVApi
import tornaco.apps.shortx.ext.api.ocr.Paddle
import tornaco.apps.shortx.ext.shortcut.ExportDAShortcut
import tornaco.apps.shortx.ui.base.CategoryTitle
import tornaco.apps.shortx.ui.base.ErrorCard
import tornaco.apps.shortx.ui.base.RemixIcon
import tornaco.apps.shortx.ui.base.ShortXAppBarScaffold
import tornaco.apps.shortx.ui.base.TipCard
import tornaco.apps.shortx.ui.base.TipDialog
import tornaco.apps.shortx.ui.base.rememberTipDialogState
import tornaco.apps.shortx.ui.theme.ShortXTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShortXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    val context = LocalContext.current

    val appIntroDialog =
        rememberTipDialogState(title = "About", tip = stringResource(id = R.string.app_intro))
    TipDialog(state = appIntroDialog)

    ShortXAppBarScaffold(
        title = stringResource(id = R.string.app_name),
        onBackPressed = null,
        actions = {
            IconButton(onClick = { appIntroDialog.show() }) {
                RemixIcon(remixName = Remix.System.information_line)
            }
        }
    ) {

        AnimatedVisibility(visible = !shortXManager.isInstalled) {
            TipCard(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .padding(horizontal = 16.dp),
                tip = "ShortX is not active",
            )
        }

        AnimatedVisibility(visible = shortXManager.isInstalled && shortXManager.version().code < MIN_SHORTX_VERSION_CODE) {
            ErrorCard(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .padding(horizontal = 16.dp),
                title = "ERROR",
                warnings = listOf("The installed ShortX version is too low. Please update it.")
            )
        }

        LaunchedEffect(Unit) {
            ShortXCVApi().initCV()
            Paddle(context).init()
        }


        CategoryTitle(
            title = stringResource(id = R.string.ext_features)
        )
        ExportDAShortcut()
    }
}

const val MIN_SHORTX_VERSION_CODE = 102051