package tornaco.apps.shortx.ext.api.cv

import android.graphics.Point
import android.graphics.Rect
import com.google.gson.GsonBuilder
import org.opencv.core.Rect as OpenCVRect

private val cvJsonGson = GsonBuilder()
    .disableHtmlEscaping()
    .serializeNulls()
    .create()

data class FindImageJsonPayload(
    val engine: String = "opencv-template",
    val found: Boolean,
    val point: CvJsonPoint?,
    val similarity: Double?,
    val templateWidth: Int,
    val templateHeight: Int,
    val matchCount: Int,
    val limit: Int,
    val truncated: Boolean,
    val bestMatch: ImageMatchJsonItem?,
    val matches: List<ImageMatchJsonItem>,
)

data class ImageMatchJsonItem(
    val index: Int,
    val similarity: Double,
    val leftTop: CvJsonPoint,
    val rightBottom: CvJsonPoint,
    val center: CvJsonPoint,
    val width: Int,
    val height: Int,
)

data class FindPointsByColorJsonPayload(
    val engine: String = "opencv-color",
    val found: Boolean,
    val color: Int,
    val threshold: Int,
    val searchRect: CvJsonRect?,
    val regionCount: Int,
    val totalMatchedPixels: Int,
    val regions: List<ColorRegionJsonItem>,
)

data class ColorRegionJsonItem(
    val index: Int,
    val bounds: CvJsonRect,
    val center: CvJsonPoint,
    val pixelCount: Int,
    val samplePoints: List<CvJsonPoint>,
)

data class CvJsonPoint(
    val x: Int,
    val y: Int,
)

data class CvJsonRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val width: Int,
    val height: Int,
)

fun FindImageJsonPayload.toJson(): String = cvJsonGson.toJson(this)

fun FindPointsByColorJsonPayload.toJson(): String = cvJsonGson.toJson(this)

fun Point.toCvJsonPoint(): CvJsonPoint = CvJsonPoint(
    x = x,
    y = y,
)

fun Rect.toCvJsonRect(): CvJsonRect = CvJsonRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    width = width(),
    height = height(),
)

fun OpenCVRect.toCvJsonRect(): CvJsonRect = CvJsonRect(
    left = x,
    top = y,
    right = x + width,
    bottom = y + height,
    width = width,
    height = height,
)
