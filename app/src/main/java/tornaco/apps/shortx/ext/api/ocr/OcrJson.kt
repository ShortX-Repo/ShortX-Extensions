package tornaco.apps.shortx.ext.api.ocr

import android.graphics.Rect
import com.baidu.paddle.lite.ocr.OcrResult
import com.google.gson.GsonBuilder

private val ocrJsonGson = GsonBuilder()
    .disableHtmlEscaping()
    .serializeNulls()
    .create()

data class OcrJsonPayload(
    val engine: String,
    val text: String,
    val blocks: List<OcrJsonBlock>,
)

data class OcrJsonBlock(
    val index: Int,
    val text: String,
    val confidence: Float?,
    val bounds: OcrJsonBounds,
    val points: List<OcrJsonPoint>,
    val clsIdx: Float?,
    val clsLabel: String?,
    val clsConfidence: Float?,
)

data class OcrJsonBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val width: Int,
    val height: Int,
)

data class OcrJsonPoint(
    val x: Int,
    val y: Int,
)

fun OcrJsonPayload.toJson(): String = ocrJsonGson.toJson(this)

fun List<OcrResult>.toOcrJson(engine: String = "paddle"): String =
    toOcrJsonPayload(engine).toJson()

fun List<OcrResult>.toOcrJsonPayload(engine: String = "paddle"): OcrJsonPayload {
    val sortedBlocks = sorted()
    return OcrJsonPayload(
        engine = engine,
        text = sortedBlocks.joinToString(separator = "") { it.label.orEmpty() },
        blocks = sortedBlocks.mapIndexed { index, result ->
            result.toOcrJsonBlock(index)
        }
    )
}

fun List<TextBlock>.toOcrJson(
    engine: String = "tesseract",
    text: String = joinToString(separator = "") { it.text },
): String = toOcrJsonPayload(engine, text).toJson()

fun List<TextBlock>.toOcrJsonPayload(
    engine: String = "tesseract",
    text: String = joinToString(separator = "") { it.text },
): OcrJsonPayload {
    return OcrJsonPayload(
        engine = engine,
        text = text,
        blocks = mapIndexed { index, block ->
            OcrJsonBlock(
                index = index,
                text = block.text,
                confidence = null,
                bounds = block.rect.toOcrJsonBounds(),
                points = block.rect.toOcrJsonPoints(),
                clsIdx = null,
                clsLabel = null,
                clsConfidence = null,
            )
        }
    )
}

private fun OcrResult.toOcrJsonBlock(index: Int): OcrJsonBlock {
    val safeBounds = bounds ?: Rect()
    val safePoints = points
        .takeIf { it.isNotEmpty() }
        ?.map { OcrJsonPoint(it.x, it.y) }
        ?: safeBounds.toOcrJsonPoints()

    return OcrJsonBlock(
        index = index,
        text = label.orEmpty(),
        confidence = confidence,
        bounds = safeBounds.toOcrJsonBounds(),
        points = safePoints,
        clsIdx = clsIdx,
        clsLabel = clsLabel?.takeIf { it.isNotBlank() },
        clsConfidence = clsConfidence,
    )
}

private fun Rect.toOcrJsonBounds(): OcrJsonBounds {
    return OcrJsonBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        width = width(),
        height = height(),
    )
}

private fun Rect.toOcrJsonPoints(): List<OcrJsonPoint> {
    return listOf(
        OcrJsonPoint(left, top),
        OcrJsonPoint(right, top),
        OcrJsonPoint(right, bottom),
        OcrJsonPoint(left, bottom),
    )
}
