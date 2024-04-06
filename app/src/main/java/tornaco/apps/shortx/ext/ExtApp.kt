package tornaco.apps.shortx.ext

import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import org.lsposed.hiddenapibypass.HiddenApiBypass
import tornaco.apps.shortx.ext.shortcut.StubApkGen

@HiltAndroidApp
class ExtApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        StubApkGen.cleanUp(this)
    }
}