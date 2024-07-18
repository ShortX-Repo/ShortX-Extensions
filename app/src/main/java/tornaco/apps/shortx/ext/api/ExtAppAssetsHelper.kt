package tornaco.apps.shortx.ext.api


import android.content.Context
import tornaco.apps.shortx.core.res.AppResources
import java.io.File
import java.io.FileOutputStream

object ExtAppAssetsHelper {
    private const val SHORTX_EXT_APP_PKG_NAME: String = "tornaco.apps.shortx.ext"

    fun copyAssets(androidContext: Context, path: String, outDir: String) {
        val destFile = File(outDir, path)
        if (destFile.exists()) return

        val appRes = AppResources(androidContext, SHORTX_EXT_APP_PKG_NAME)
        val appContext = appRes.appContext
        val assets = appContext.assets
        assets.open(path).use { stream ->
            stream.copyTo(FileOutputStream(destFile.also {
                it.parentFile?.mkdirs()
            }))
        }
    }
}