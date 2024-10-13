package tornaco.apps.shortx.ext.api.cv

import android.graphics.Bitmap
import android.graphics.Point
import autojs.api.Images
import autojs.api.ScreenMetrics
import autojs.image.ColorFinder
import autojs.image.ImageWrapper
import autojs.opencv.OpenCVHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.opencv.core.Rect
import tornaco.apps.shortx.core.annotations.DoNotStrip
import tornaco.apps.shortx.core.util.Logger

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

    fun findPointsByColor(
        image: Bitmap,
        color: Int,
        threshold: Int,
        rect: android.graphics.Rect?
    ): List<Point> {
        runBlocking {
            initCV()
        }
        val images = ColorFinder(ScreenMetrics())
        val points = images.findPointsByColor(
            ImageWrapper.ofBitmap(image),
            color,
            threshold,
            rect?.let {
                convertFromAndroidRect(it)
            }
        )
        return points.map {
            Point(it.x.toInt(), it.y.toInt())
        }
    }

    private fun convertFromAndroidRect(androidRect: android.graphics.Rect): Rect {
        val x = androidRect.left
        val y = androidRect.top
        val width = androidRect.width()
        val height = androidRect.height()

        return Rect(x, y, width, height)
    }
}

