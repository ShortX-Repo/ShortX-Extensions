package tornaco.apps.shortx.ext

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import tornaco.apps.shortx.core.location.ILocationProvider
import tornaco.apps.shortx.core.os.SynchronousResultReceiver
import tornaco.apps.shortx.core.proto.action.CurrentLocationProviderPreference
import tornaco.apps.shortx.core.proto.action.GetCurrentLocationAddress
import tornaco.apps.shortx.core.proto.action.GetCurrentLocationInfo
import tornaco.apps.shortx.core.proto.common.CurrentLocationAddressData
import tornaco.apps.shortx.core.proto.common.CurrentLocationInfoData
import tornaco.apps.shortx.core.proto.common.Rect
import tornaco.apps.shortx.core.proto.toAndroidRect
import tornaco.apps.shortx.core.res.Remix
import tornaco.apps.shortx.core.rule.action.wrap
import tornaco.apps.shortx.core.shortXManager
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.ext.api.cv.ShortXCVApi
import tornaco.apps.shortx.ext.api.ocr.ShortXPaddleApi
import tornaco.apps.shortx.ext.api.ocr.ShortXTessApi
import tornaco.apps.shortx.ext.api.ocr.drawBoundingBoxes
import tornaco.apps.shortx.ext.api.ocr.saveBitmapToFile
import tornaco.apps.shortx.ext.service.AppLocationService
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
import java.time.Duration
import kotlin.coroutines.resume

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationInfo by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            locationInfo = "Location permission granted"
        } else {
            locationInfo = "Location permission denied"
        }
    }

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

        locationInfo?.let {
            TipCard(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .padding(horizontal = 16.dp),
                tip = it,
            )
        }

        Column(Modifier.padding(16.dp)) {
            Button(onClick = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }) {
                Text("Request Location Permission")
            }

            Button(onClick = {
                scope.launch {
                    testLocation(context) {
                        locationInfo = it
                    }
                }
            }) {
                Text("testLocation")
            }

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

private suspend fun testLocation(context: Context, onResult: (String) -> Unit) {
    Logger.nameless.d("testLocation...")
    onResult("Locating...")
    val provider = suspendCancellableCoroutine<ILocationProvider?> { continuation ->
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Logger.nameless.d("onServiceConnected: $name")
                if (continuation.isActive) {
                    continuation.resume(ILocationProvider.Stub.asInterface(service))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Logger.nameless.d("onServiceDisconnected: $name")
            }
        }
        val intent = Intent(context, AppLocationService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        continuation.invokeOnCancellation {
            context.unbindService(connection)
        }
    }

    provider?.let { service ->
        runCatching {
            val receiver = SynchronousResultReceiver.get()
            val request = GetCurrentLocationInfo.newBuilder()
                .setProviderPreference(CurrentLocationProviderPreference.CurrentLocationProviderPreference_Auto)
                .setTimeoutMillis(10000)
                .build()
            service.getCurrentLocationInfo("test-info", receiver, request.toByteArray().wrap())
            val result = receiver.awaitResultNoInterrupt(Duration.ofSeconds(10))
            val info = result.getValue(null)?.let {
                CurrentLocationInfoData.parseFrom(it.byteData)
            }
            Logger.nameless.d("Location info: $info")

            val addressReceiver = SynchronousResultReceiver.get()
            val addressRequest = GetCurrentLocationAddress.newBuilder()
                .setProviderPreference(CurrentLocationProviderPreference.CurrentLocationProviderPreference_Auto)
                .setTimeoutMillis(10000)
                .build()
            service.getCurrentLocationAddress(
                "test-address",
                addressReceiver,
                addressRequest.toByteArray().wrap()
            )
            val addressResult = addressReceiver.awaitResultNoInterrupt(Duration.ofSeconds(10))
            val address = addressResult.getValue(null)?.let {
                CurrentLocationAddressData.parseFrom(it.byteData)
            }
            Logger.nameless.d("Location address: $address")

            onResult("Location: ${info?.latitude}, ${info?.longitude}\nAddress: ${address?.addressLine}")
        }.onFailure {
            Logger.nameless.e(it, "testLocation failed")
            onResult("testLocation failed: ${it.message}")
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
        )
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