package tornaco.apps.shortx.ext.api.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import autojs.api.OcrPaddle
import autojs.image.ImageWrapper
import com.baidu.paddle.lite.ocr.OcrResult
import tornaco.apps.shortx.core.annotations.DoNotStrip
import tornaco.apps.shortx.core.proto.toProtoRect
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.ext.api.ExtAppAssetsHelper

@Deprecated("Native crash on some Android device.")
@DoNotStrip
class ShortXPaddleApi(private val context: Context) {
    private val logger = Logger("ShortXPaddleApi")

    private val assetsFiles = listOf(
        "labels/ppocr_keys_v1.txt",

        "models/ocr_v3_for_cpu/cls_opt.nb",
        "models/ocr_v3_for_cpu/det_opt.nb",
        "models/ocr_v3_for_cpu/rec_opt.nb",

        "models/ocr_v3_for_cpu(slim)/cls_opt.nb",
        "models/ocr_v3_for_cpu(slim)/det_opt.nb",
        "models/ocr_v3_for_cpu(slim)/rec_opt.nb",
    )

    private val ocr by lazy {
        OcrPaddle(context).apply {
            assetsFiles.forEach {
                ExtAppAssetsHelper.copyAssets(context, it, context.cacheDir.absolutePath)
            }
            val init = init(false)
            logger.d("Init: $init")
        }
    }

    fun initPaddle(): Boolean {
        val loaded = ocr.init(true)
        logger.d("initPaddle: $loaded")
        return loaded
    }

    private fun recognizeResults(
        image: Bitmap
    ): List<OcrResult> {
        return ocr.detect(ImageWrapper.ofBitmap(image), 4, true).sorted()
    }

    fun detect(
        image: Bitmap,
        text: String
    ): List<ByteArray> {
        val result = recognizeResults(image)
        logger.d("Detect: $result")
        val textBlocks = result.map {
            TextBlock(
                it.label.orEmpty(),
                it.bounds ?: Rect()
            )
        }
        val matched = findBoundingRects(textBlocks, text)
        return matched.map { it.toProtoRect().toByteArray() }
    }

    fun recognizeText(
        image: Bitmap
    ): String {
        val result = recognizeResults(image)
        logger.d("recognizeText: $result")
        return result.joinToString(separator = "") { it.label.orEmpty() }
    }

    fun recognizeTextJson(
        image: Bitmap
    ): String {
        val result = recognizeResults(image)
        val json = result.toOcrJson(engine = "paddle")
        logger.d("recognizeTextJson: $json")
        return json
    }
}
