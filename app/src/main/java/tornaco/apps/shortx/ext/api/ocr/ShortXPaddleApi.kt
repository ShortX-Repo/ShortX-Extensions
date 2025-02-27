package tornaco.apps.shortx.ext.api.ocr

import android.content.Context
import android.graphics.Bitmap
import autojs.api.OcrPaddle
import autojs.image.ImageWrapper
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.ext.api.ExtAppAssetsHelper

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

    fun detect(
        image: Bitmap,
        cpuThreadNum: Int = 4,
        useSlim: Boolean = false
    ): List<ByteArray> {
        val result = ocr.detect(ImageWrapper.ofBitmap(image), cpuThreadNum, useSlim)
        logger.d("Detect: $result")
        return result.map {
            it.toProtoResult().toByteArray()
        }
    }

    fun recognizeText(
        image: Bitmap,
        cpuThreadNum: Int = 4,
        useSlim: Boolean = false
    ): Array<String> {
        val result = ocr.recognizeText(ImageWrapper.ofBitmap(image), cpuThreadNum, useSlim)
        logger.d("recognizeText: $result")
        return result
    }
}