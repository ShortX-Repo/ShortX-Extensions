package tornaco.apps.shortx.ext.api.ocr

import android.content.Context
import autojs.api.OcrPaddle
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.ext.api.ExtAppAssetsHelper

class Paddle(private val context: Context) {
    private val logger = Logger("Paddle")

    private val assetsFiles = listOf(
        "labels/ppocr_keys_v1.txt",

        "models/ocr_v3_for_cpu/cls_opt.nb",
        "models/ocr_v3_for_cpu/det_opt.nb",
        "models/ocr_v3_for_cpu/rec_opt.nb",

        "models/ocr_v3_for_cpu(slim)/cls_opt.nb",
        "models/ocr_v3_for_cpu(slim)/det_opt.nb",
        "models/ocr_v3_for_cpu(slim)/rec_opt.nb",
    )

    fun init() {
        assetsFiles.forEach {
            ExtAppAssetsHelper.copyAssets(context, it, context.cacheDir.absolutePath)
        }

        val init = OcrPaddle(context).init(false)
        logger.d("Init: $init")
    }
}