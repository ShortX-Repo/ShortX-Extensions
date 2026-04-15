package tornaco.apps.shortx.ext

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornaco.apps.shortx.core.proto.common.Rect
import tornaco.apps.shortx.core.proto.toAndroidRect
import tornaco.apps.shortx.core.res.Remix
import tornaco.apps.shortx.core.shortXManager
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.ext.api.cv.ShortXCVApi
import tornaco.apps.shortx.ext.api.ocr.ShortXPaddleApi
import tornaco.apps.shortx.ext.api.ocr.ShortXTessApi
import tornaco.apps.shortx.ext.api.ocr.drawBoundingBoxes
import tornaco.apps.shortx.ext.api.ocr.saveBitmapToFile
import tornaco.apps.shortx.ui.base.CategoryTitle
import tornaco.apps.shortx.ui.base.ErrorCard
import tornaco.apps.shortx.ui.base.RemixIcon
import tornaco.apps.shortx.ui.base.SectionSpacer
import tornaco.apps.shortx.ui.base.ShortXAppBarScaffold
import tornaco.apps.shortx.ui.base.TipCard
import tornaco.apps.shortx.ui.base.TipDialog
import tornaco.apps.shortx.ui.base.rememberTipDialogState
import tornaco.apps.shortx.ui.theme.ShortXTheme
import java.io.File
import java.io.FileOutputStream

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {

    val appIntroDialog =
        rememberTipDialogState(title = "About", tip = stringResource(id = R.string.app_intro))
    TipDialog(state = appIntroDialog)

    ShortXAppBarScaffold(
        title = stringResource(id = R.string.app_name) + BuildConfig.VERSION_NAME,
        onBackPressed = null,
        actions = {
            IconButton(onClick = {
                appIntroDialog.show()
            }) {
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
        }


        CategoryTitle(
            title = stringResource(id = R.string.app_intro)
        )

        SectionSpacer()

        if (BuildConfig.DEBUG) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Button(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        testTess(context)
                    }
                }
            }) {
                Text("testTess")
            }

            Button(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        testPaddle(context)
                    }
                }
            }) {
                Text("testPaddle")
            }
        }
    }
}

private fun testPaddle(context: Context) {
    ShortXPaddleApi(context).apply {
        val screenFile = File(context.externalCacheDir, "screen.png")
        screenFile.parentFile?.mkdirs()
        context.resources.openRawResource(R.raw.screen1).use {
            it.copyTo(FileOutputStream(screenFile))
        }
        recognizeText(
            BitmapFactory.decodeResource(
                context.resources,
                R.raw.screen1
            )
        ).apply { Logger.nameless.w(this) }
    }
}

private fun testTess(context: Context) {
    ShortXTessApi(context).apply {
        recognizeText(
            BitmapFactory.decodeResource(
                context.resources,
                R.raw.screen1
            )
        ).apply { Logger.nameless.w(this) }
        recognizeText(
            BitmapFactory.decodeResource(
                context.resources,
                R.raw.screen2
            )
        ).apply { Logger.nameless.w(this) }
        recognizeTextWithRect(
            BitmapFactory.decodeResource(
                context.resources,
                R.raw.screen2
            )
        ).apply { Logger.nameless.w(this) }
        findContinuousTextPosition(
            BitmapFactory.decodeResource(
                context.resources,
                R.raw.screen2
            ),
            "任何地方"
        ).apply { Logger.nameless.w(Rect.parseFrom(this)) }

        findAllContinuousTextPositions(
            BitmapFactory.decodeResource(
                context.resources,
                R.raw.screen2
            ),
            "应用"
        ).apply {
            Logger.nameless.w(this.map {
                Rect.parseFrom(it)
            })

            val bunds = this.map {
                Rect.parseFrom(it).toAndroidRect()
            }
            saveBitmapToFile(
                drawBoundingBoxes(
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.raw.screen2
                    ), bunds
                ),
                File(
                    context.cacheDir,
                    "Bunds-${System.currentTimeMillis()}.png"
                ).absolutePath
            )
        }
    }
}

const val MIN_SHORTX_VERSION_CODE = 102051
