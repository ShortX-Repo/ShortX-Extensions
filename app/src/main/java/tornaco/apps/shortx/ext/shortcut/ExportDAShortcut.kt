package tornaco.apps.shortx.ext.shortcut

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.promeg.pinyinhelper.Pinyin
import kotlinx.coroutines.launch
import tornaco.apps.shortx.core.res.Remix
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.core.util.fallbackOnEmpty
import tornaco.apps.shortx.core.util.removeSpecialCharacters
import tornaco.apps.shortx.ext.R
import tornaco.apps.shortx.ext.ui.DASelectorDialog
import tornaco.apps.shortx.ext.ui.rememberDADialogState
import tornaco.apps.shortx.ui.base.ListItem
import tornaco.apps.shortx.ui.base.ProgressDialog
import tornaco.apps.shortx.ui.base.rememberProgressDialogState
import tornaco.apps.shortx.ui.shortcut.ApkInstallState
import tornaco.apps.shortx.ui.shortcut.ShortcutStubInstaller

@Composable
fun ExportDAShortcut() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progressDialogState =
        rememberProgressDialogState(title = "Working", message = "...")
    ProgressDialog(state = progressDialogState)
    val uiHandler = remember {
        Handler(Looper.getMainLooper())
    }

    val daSelectorState = rememberDADialogState { da ->
        val pkgNameSuffix = Pinyin.toPinyin(da.title, "").lowercase().removeSpecialCharacters()
        val info = ShortcutStubInfo(
            appLabel = da.title,
            appPkgName = "shortx.stub.${pkgNameSuffix}",
            appIcon = Remix.System.apps_2_line,
            appIconTintColor = Color.WHITE,
            appIconBgColor = androidx.compose.ui.graphics.Color(Color.RED).toArgb(),
            daId = da.id
        )
        scope.launch {
            ShortcutStubInstaller.doInstallShortcutApk(context, info) {
                uiHandler.post {
                    Logger("ExportDAShortcut").i("doInstallShortcutApk: $it")
                    when (it) {
                        is ApkInstallState.InstallFailure -> {
                            progressDialogState.dismiss()
                            Toast.makeText(
                                context,
                                it.message.fallbackOnEmpty { "Failed." },
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        ApkInstallState.InstallSuccess -> {
                            progressDialogState.dismiss()
                            Toast.makeText(context, "Success", Toast.LENGTH_LONG)
                                .show()
                        }

                        ApkInstallState.Installing -> {
                            progressDialogState.show()
                        }
                    }
                }
            }
        }
    }
    DASelectorDialog(state = daSelectorState)
    ListItem(
        title = stringResource(id = R.string.export_rule_as_apk),
        onClick = {
            daSelectorState.show()
        }
    )
}