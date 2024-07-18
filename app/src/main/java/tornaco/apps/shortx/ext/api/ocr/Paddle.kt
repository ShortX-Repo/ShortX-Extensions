package tornaco.apps.shortx.ext.api.ocr

import android.content.Context
import autojs.api.OcrPaddle
import tornaco.apps.shortx.core.util.Logger

class Paddle(private val context: Context) {
    private val logger = Logger("Paddle")

    fun init() {
        val init = OcrPaddle(context).init(false)
        logger.d("Init: $init")
    }
}