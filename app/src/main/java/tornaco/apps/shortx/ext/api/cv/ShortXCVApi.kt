package tornaco.apps.shortx.ext.api.cv

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import autojs.api.Images
import autojs.api.ScreenMetrics
import autojs.image.ImageWrapper
import autojs.image.TemplateMatching
import autojs.opencv.OpenCVHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point as OpenCVPoint
import org.opencv.core.Rect as OpenCVRect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import tornaco.apps.shortx.core.annotations.DoNotStrip
import tornaco.apps.shortx.core.util.Logger
import java.util.ArrayList
import kotlin.math.roundToInt

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

    private fun ensureCvReady() {
        runBlocking {
            initCV()
        }
    }

    fun findImage(image: Bitmap, template: Bitmap): Point? {
        return matchTemplate(image, template, limit = 1).firstOrNull()?.let {
            Point(it.point.x.toInt(), it.point.y.toInt())
        }
    }

    fun findImageJson(image: Bitmap, template: Bitmap): String {
        val rawMatches = matchTemplate(
            image = image,
            template = template,
            limit = IMAGE_JSON_MATCH_LIMIT + 1,
        )
        val truncated = rawMatches.size > IMAGE_JSON_MATCH_LIMIT
        val matches = rawMatches
            .take(IMAGE_JSON_MATCH_LIMIT)
            .mapIndexed { index, match ->
                match.toImageMatchJsonItem(index, template.width, template.height)
            }
        val bestMatch = matches.firstOrNull()
        val json = FindImageJsonPayload(
            found = matches.isNotEmpty(),
            point = bestMatch?.leftTop,
            similarity = bestMatch?.similarity,
            templateWidth = template.width,
            templateHeight = template.height,
            matchCount = matches.size,
            limit = IMAGE_JSON_MATCH_LIMIT,
            truncated = truncated,
            bestMatch = bestMatch,
            matches = matches,
        ).toJson()
        logger.d("findImageJson: $json")
        return json
    }

    fun findPointsByColor(
        image: Bitmap,
        color: Int,
        threshold: Int,
        rect: Rect?
    ): List<Point> {
        ensureCvReady()
        val sourceMat = Mat()
        try {
            Utils.bitmapToMat(image, sourceMat)
            val searchRect = rect?.let { convertFromAndroidRect(it) }
            val mask = createColorMask(sourceMat, color, threshold, searchRect)
            val nonZero = Mat()
            try {
                Core.findNonZero(mask, nonZero)
                if (nonZero.rows() == 0 || nonZero.cols() == 0) {
                    return emptyList()
                }
                val nonZeroPoints = OpenCVHelper.newMatOfPoint(nonZero)
                val points = try {
                    nonZeroPoints.toArray().map {
                        val absX = it.x.toInt() + (rect?.left ?: 0)
                        val absY = it.y.toInt() + (rect?.top ?: 0)
                        Point(absX, absY)
                    }
                } finally {
                    OpenCVHelper.release(nonZeroPoints)
                }
                return points
            } finally {
                OpenCVHelper.release(mask)
                OpenCVHelper.release(nonZero)
            }
        } finally {
            OpenCVHelper.release(sourceMat)
        }
    }

    fun findPointsByColorJson(
        image: Bitmap,
        color: Int,
        threshold: Int,
        rect: Rect?
    ): String {
        ensureCvReady()
        val sourceMat = Mat()
        try {
            Utils.bitmapToMat(image, sourceMat)
            val searchRect = rect?.let { convertFromAndroidRect(it) }
            val mask = createColorMask(sourceMat, color, threshold, searchRect)
            val regionSnapshots = try {
                extractColorRegions(mask, rect)
            } finally {
                OpenCVHelper.release(mask)
            }
            val regions = regionSnapshots
                .sortedWith(
                    compareByDescending<ColorRegionSnapshot> { it.pixelCount }
                        .thenBy { it.bounds.y }
                        .thenBy { it.bounds.x }
                )
                .mapIndexed { index, snapshot ->
                    ColorRegionJsonItem(
                        index = index,
                        bounds = snapshot.bounds.toCvJsonRect(),
                        center = snapshot.center,
                        pixelCount = snapshot.pixelCount,
                        samplePoints = snapshot.samplePoints,
                    )
                }
            val json = FindPointsByColorJsonPayload(
                found = regions.isNotEmpty(),
                color = color,
                threshold = threshold,
                searchRect = rect?.toCvJsonRect(),
                regionCount = regions.size,
                totalMatchedPixels = regions.sumOf { it.pixelCount },
                regions = regions,
            ).toJson()
            logger.d("findPointsByColorJson: $json")
            return json
        } finally {
            OpenCVHelper.release(sourceMat)
        }
    }

    private fun matchTemplate(
        image: Bitmap,
        template: Bitmap,
        limit: Int,
    ): List<TemplateMatching.Match> {
        ensureCvReady()
        val images = Images(ScreenMetrics())
        val imageWrapper = ImageWrapper.ofBitmap(image)
        val templateWrapper = ImageWrapper.ofBitmap(template)
        if (imageWrapper == null || templateWrapper == null) {
            return emptyList()
        }
        val matches = images.matchTemplate(
            imageWrapper,
            templateWrapper,
            IMAGE_JSON_WEAK_THRESHOLD,
            IMAGE_JSON_STRICT_THRESHOLD,
            null,
            TemplateMatching.MAX_LEVEL_AUTO,
            limit,
        )
        return matches.sortedByDescending { it.similarity }
    }

    private fun createColorMask(
        sourceMat: Mat,
        color: Int,
        threshold: Int,
        searchRect: OpenCVRect?
    ): Mat {
        val mask = Mat()
        val lowerBound = Scalar(
            android.graphics.Color.red(color) - threshold.toDouble(),
            android.graphics.Color.green(color) - threshold.toDouble(),
            android.graphics.Color.blue(color) - threshold.toDouble(),
            255.0
        )
        val upperBound = Scalar(
            android.graphics.Color.red(color) + threshold.toDouble(),
            android.graphics.Color.green(color) + threshold.toDouble(),
            android.graphics.Color.blue(color) + threshold.toDouble(),
            255.0
        )
        if (searchRect != null) {
            val roi = Mat(sourceMat, searchRect)
            try {
                Core.inRange(roi, lowerBound, upperBound, mask)
            } finally {
                OpenCVHelper.release(roi)
            }
        } else {
            Core.inRange(sourceMat, lowerBound, upperBound, mask)
        }
        return mask
    }

    private fun extractColorRegions(mask: Mat, searchRect: Rect?): List<ColorRegionSnapshot> {
        val contourSource = mask.clone()
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        try {
            Imgproc.findContours(
                contourSource,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            return contours.mapNotNull { contour ->
                contour.toColorRegionSnapshot(mask, searchRect)
            }
        } finally {
            contours.forEach { OpenCVHelper.release(it) }
            OpenCVHelper.release(contourSource)
            OpenCVHelper.release(hierarchy)
        }
    }

    private fun MatOfPoint.toColorRegionSnapshot(mask: Mat, searchRect: Rect?): ColorRegionSnapshot? {
        if (rows() <= 0 || cols() <= 0) {
            return null
        }
        val localBounds = Imgproc.boundingRect(this)
        if (localBounds.width <= 0 || localBounds.height <= 0) {
            return null
        }
        val translatedContour = translateContour(this, -localBounds.x.toDouble(), -localBounds.y.toDouble())
        val regionMask = Mat.zeros(localBounds.height, localBounds.width, CvType.CV_8UC1)
        val maskRegion = Mat(mask, localBounds)
        val exactMatchMask = Mat()
        try {
            Imgproc.drawContours(
                regionMask,
                listOf(translatedContour),
                -1,
                Scalar(255.0),
                Imgproc.FILLED
            )
            Core.bitwise_and(maskRegion, regionMask, exactMatchMask)
            val pixelCount = Core.countNonZero(exactMatchMask)
            if (pixelCount <= 0) {
                return null
            }
            val offsetX = searchRect?.left ?: 0
            val offsetY = searchRect?.top ?: 0
            val absoluteBounds = OpenCVRect(
                localBounds.x + offsetX,
                localBounds.y + offsetY,
                localBounds.width,
                localBounds.height,
            )
            val samplePoints = extractRegionSamplePoints(
                regionMask = exactMatchMask,
                localBounds = localBounds,
                offsetX = offsetX,
                offsetY = offsetY,
            )
            val center = computeRegionCenter(
                regionMask = exactMatchMask,
                localBounds = localBounds,
                offsetX = offsetX,
                offsetY = offsetY,
                fallbackPoint = samplePoints.firstOrNull(),
            )
            return ColorRegionSnapshot(
                bounds = absoluteBounds,
                center = center,
                pixelCount = pixelCount,
                samplePoints = samplePoints,
            )
        } finally {
            OpenCVHelper.release(translatedContour)
            OpenCVHelper.release(regionMask)
            OpenCVHelper.release(maskRegion)
            OpenCVHelper.release(exactMatchMask)
        }
    }

    private fun computeRegionCenter(
        regionMask: Mat,
        localBounds: OpenCVRect,
        offsetX: Int,
        offsetY: Int,
        fallbackPoint: CvJsonPoint?,
    ): CvJsonPoint {
        val moments = Imgproc.moments(regionMask, true)
        if (moments.m00 != 0.0) {
            val localCenterX = (moments.m10 / moments.m00).roundToInt()
            val localCenterY = (moments.m01 / moments.m00).roundToInt()
            if (regionMask.isNonZeroAt(localCenterX, localCenterY)) {
                return CvJsonPoint(
                    x = localBounds.x + localCenterX + offsetX,
                    y = localBounds.y + localCenterY + offsetY,
                )
            }
        }

        if (fallbackPoint != null) {
            return fallbackPoint
        }

        return CvJsonPoint(
            x = localBounds.x + localBounds.width / 2 + offsetX,
            y = localBounds.y + localBounds.height / 2 + offsetY,
        )
    }

    private fun extractRegionSamplePoints(
        regionMask: Mat,
        localBounds: OpenCVRect,
        offsetX: Int,
        offsetY: Int,
    ): List<CvJsonPoint> {
        val samples = LinkedHashMap<String, CvJsonPoint>()
        fun addCandidate(localX: Int, localY: Int) {
            if (!regionMask.isNonZeroAt(localX, localY)) {
                return
            }
            val point = CvJsonPoint(
                x = localBounds.x + localX + offsetX,
                y = localBounds.y + localY + offsetY,
            )
            samples.putIfAbsent("${point.x}:${point.y}", point)
        }

        val maxX = regionMask.cols() - 1
        val maxY = regionMask.rows() - 1
        if (maxX < 0 || maxY < 0) {
            return emptyList()
        }

        val anchorCandidates = listOf(
            0 to 0,
            maxX / 2 to 0,
            maxX to 0,
            0 to maxY / 2,
            maxX / 2 to maxY / 2,
            maxX to maxY / 2,
            0 to maxY,
            maxX to maxY,
        )
        anchorCandidates.forEach { (x, y) ->
            if (samples.size < COLOR_REGION_SAMPLE_LIMIT) {
                addCandidate(x, y)
            }
        }

        if (samples.size < COLOR_REGION_SAMPLE_LIMIT) {
            val stepX = maxOf(1, regionMask.cols() / 4)
            val stepY = maxOf(1, regionMask.rows() / 4)
            var y = 0
            while (y < regionMask.rows() && samples.size < COLOR_REGION_SAMPLE_LIMIT) {
                var x = 0
                while (x < regionMask.cols() && samples.size < COLOR_REGION_SAMPLE_LIMIT) {
                    addCandidate(x, y)
                    x += stepX
                }
                y += stepY
            }
        }

        if (samples.size < COLOR_REGION_SAMPLE_LIMIT) {
            loop@ for (y in 0 until regionMask.rows()) {
                for (x in 0 until regionMask.cols()) {
                    addCandidate(x, y)
                    if (samples.size >= COLOR_REGION_SAMPLE_LIMIT) {
                        break@loop
                    }
                }
            }
        }

        return samples.values.toList()
    }

    private fun Mat.isNonZeroAt(x: Int, y: Int): Boolean {
        if (x !in 0 until cols() || y !in 0 until rows()) {
            return false
        }
        val value = get(y, x) ?: return false
        return value.isNotEmpty() && value[0] > 0.0
    }

    private fun translateContour(
        contour: MatOfPoint,
        deltaX: Double,
        deltaY: Double,
    ): MatOfPoint {
        val translated = contour.toArray().map { point ->
            OpenCVPoint(point.x + deltaX, point.y + deltaY)
        }
        return MatOfPoint(*translated.toTypedArray())
    }

    private fun TemplateMatching.Match.toImageMatchJsonItem(
        index: Int,
        templateWidth: Int,
        templateHeight: Int,
    ): ImageMatchJsonItem {
        val left = point.x.toInt()
        val top = point.y.toInt()
        val width = templateWidth
        val height = templateHeight
        val right = left + width
        val bottom = top + height
        return ImageMatchJsonItem(
            index = index,
            similarity = similarity,
            leftTop = CvJsonPoint(left, top),
            rightBottom = CvJsonPoint(right, bottom),
            center = CvJsonPoint(left + width / 2, top + height / 2),
            width = width,
            height = height,
        )
    }

    private fun convertFromAndroidRect(androidRect: Rect): OpenCVRect {
        val x = androidRect.left
        val y = androidRect.top
        val width = androidRect.width()
        val height = androidRect.height()

        return OpenCVRect(x, y, width, height)
    }

    private data class ColorRegionSnapshot(
        val bounds: OpenCVRect,
        val center: CvJsonPoint,
        val pixelCount: Int,
        val samplePoints: List<CvJsonPoint>,
    )

    private companion object {
        const val IMAGE_JSON_MATCH_LIMIT = 20
        const val COLOR_REGION_SAMPLE_LIMIT = 8
        const val IMAGE_JSON_WEAK_THRESHOLD = 0.7f
        const val IMAGE_JSON_STRICT_THRESHOLD = 0.9f
    }
}
