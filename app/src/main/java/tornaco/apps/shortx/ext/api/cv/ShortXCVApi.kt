package tornaco.apps.shortx.ext.api.cv

import android.graphics.Bitmap
import android.graphics.Point
import autojs.api.Images
import autojs.image.ImageWrapper
import autojs.opencv.OpenCVHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import tornaco.apps.shortx.core.annotations.DoNotStrip
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.ui.cv.api.ScreenMetrics

@OptIn(ExperimentalCoroutinesApi::class)
@DoNotStrip
class ShortXCVApi {
    private val logger = Logger("ShortXCVApi")

    suspend fun initCV() {
        suspendCancellableCoroutine { con ->
            OpenCVHelper.initIfNeeded {
                logger.d("Init complete.")
                con.resume(Unit) {}

                con.invokeOnCancellation {
                }
            }
        }
    }

    fun findImage(image: Bitmap, template: Bitmap): Point? {
        runBlocking {
            initCV()
        }

        val images = Images(ScreenMetrics())
        val point: org.opencv.core.Point? = images.findImage(
            ImageWrapper.ofBitmap(image),
            ImageWrapper.ofBitmap(template)
        )
        return point?.let {
            Point(it.x.toInt(), it.y.toInt())
        }
    }
}