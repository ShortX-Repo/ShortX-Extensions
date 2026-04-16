package tornaco.apps.shortx.ext.api.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import tornaco.apps.shortx.core.annotations.DoNotStrip
import tornaco.apps.shortx.core.proto.toProtoRect
import tornaco.apps.shortx.ext.api.ExtAppAssetsHelper
import tornaco.apps.shortx.ext.api.ocr.TessApi.findAllContinuousTextPositions
import tornaco.apps.shortx.ext.api.ocr.TessApi.findContinuousTextPosition
import tornaco.apps.shortx.ext.api.ocr.TessApi.recognizeText
import tornaco.apps.shortx.ext.api.ocr.TessApi.recognizeTextWithRect
import java.io.File
import java.io.FileOutputStream


@DoNotStrip
class ShortXTessApi(private val context: Context) {
    private val tess by lazy {
        TessApi.install(context)
    }

    fun recognizeText(bitmap: Bitmap): String? {
        return tess.recognizeText(bitmap)
    }


    fun recognizeTextWithRect(bitmap: Bitmap): List<TextBlock> {
        return tess.recognizeTextWithRect(bitmap)
    }

    fun recognizeTextJson(bitmap: Bitmap): String {
        val text = tess.recognizeText(bitmap).orEmpty()
        val blocks = tess.recognizeTextWithRect(bitmap)
        return blocks.toOcrJson(engine = "tesseract", text = text)
    }

    fun findContinuousTextPosition(
        bitmap: Bitmap,
        targetText: String
    ): ByteArray? {
        return tess.findContinuousTextPosition(bitmap, targetText)?.toProtoRect()?.toByteArray()
    }

    fun findAllContinuousTextPositions(
        bitmap: Bitmap,
        targetText: String
    ): List<ByteArray> {
        return tess.findAllContinuousTextPositions(bitmap, targetText).map {
            it.toProtoRect().toByteArray()
        }
    }
}

object TessApi {
    private val assetsFiles = listOf(
        "tessdata/chi_sim.traineddata",
        "tessdata/eng.traineddata",
    )

    fun install(context: Context): TessBaseAPI {
        val modelDataDir = File(context.cacheDir, "tess")
        assetsFiles.forEach {
            Log.w("TORNACO", "install Call copyAssets: $it")
            ExtAppAssetsHelper.copyAssets(context, it, modelDataDir.absolutePath)
        }
        Log.w("TORNACO", "install after copyAssets.")
        val tess = TessBaseAPI()
        // create separate instance of TessBaseAPI for each thread.

        // Given path must contain subdirectory `tessdata` where are `*.traineddata` language files
        // The path must be directly readable by the app
        val dataPath: String = modelDataDir.absolutePath + "/"

        // Initialize API for specified language
        // (can be called multiple times during Tesseract lifetime)
        // could be multiple languages, like "eng+deu+fra"
        if (!tess.init(
                dataPath,
                "chi_sim+eng"
            )
        ) {
            // Error initializing Tesseract (wrong/inaccessible data path or not existing language file(s))
            // Release the native Tesseract instance
            tess.recycle()
            error("Failed to init Tess")
        }
        return tess
    }

    fun TessBaseAPI.recognizeText(bitmap: Bitmap): String? {
        // Load the image (file path, Bitmap, Pix...)
        // (can be called multiple times during Tesseract lifetime)
        setImage(bitmap)

        // Start the recognition (if not done for this image yet) and retrieve the result
        // (can be called multiple times during Tesseract lifetime)
        val text = utF8Text?.replace(" ", "")
        return text
    }

    fun TessBaseAPI.recognizeTextWithRect(bitmap: Bitmap): List<TextBlock> {
        setImage(bitmap)
        utF8Text
        // 获取单词级别的边界框
        val wordBoundingBoxes = mutableListOf<TextBlock>()
        val wordIterator = resultIterator
        wordIterator.begin()
        do {
            val wordText = wordIterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD).orEmpty()
            val rect = wordIterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD) ?: Rect()
            wordBoundingBoxes.add(TextBlock(wordText, rect))
        } while (wordIterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
        return wordBoundingBoxes
    }

    fun TessBaseAPI.findContinuousTextPosition(bitmap: Bitmap, targetText: String): Rect? {
        setImage(bitmap)
        // 获取识别结果
        utF8Text
        // 获取单词级别的边界框
        val wordIterator = resultIterator
        wordIterator.begin()

        val words = mutableListOf<Pair<String, Rect>>()
        do {
            val wordText = wordIterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
            val boundingBox = wordIterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
            words.add(wordText to boundingBox)
        } while (wordIterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))

        // 检查是否存在连续的字符串匹配目标字符串
        val combinedText = words.joinToString("") { it.first }
        val targetIndex = combinedText.indexOf(targetText)
        if (targetIndex != -1) {
            // 找到目标字符串，计算其边界框
            var left = Int.MAX_VALUE
            var top = Int.MAX_VALUE
            var right = Int.MIN_VALUE
            var bottom = Int.MIN_VALUE

            var currentLength = 0
            for ((word, rect) in words) {
                if (currentLength <= targetIndex && targetIndex < currentLength + word.length) {
                    left = minOf(left, rect.left)
                    top = minOf(top, rect.top)
                    right = maxOf(right, rect.right)
                    bottom = maxOf(bottom, rect.bottom)
                }
                currentLength += word.length
            }

            return Rect(left, top, right, bottom)
        }

        // 如果未找到目标字符串，返回 null
        return null
    }

    fun TessBaseAPI.findAllContinuousTextPositions(bitmap: Bitmap, targetText: String): List<Rect> {
        setImage(bitmap)

        // 获取识别结果
        utF8Text

        // 获取单词级别的边界框
        val wordIterator = resultIterator
        wordIterator.begin()

        val words = mutableListOf<TextBlock>()
        do {
            val wordText = wordIterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
            val boundingBox = wordIterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
            words.add(TextBlock(wordText, boundingBox))
        } while (wordIterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))

        return findBoundingRects(words, targetText)
    }


}

fun drawBoundingBoxes(bitmap: Bitmap, boundingBoxes: List<Rect>): Bitmap {
    // 创建一个可修改的 Bitmap 副本
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    // 绘制红色框
    for (box in boundingBoxes) {
        // 设置画笔
        val paint = Paint().apply {
            color = listOf(
                Color.RED,
                Color.BLUE,
                Color.CYAN,
                Color.GRAY,
                Color.DKGRAY,
                Color.GREEN,
                Color.MAGENTA,
                Color.YELLOW
            ).random()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        canvas.drawRect(box, paint)
    }

    return mutableBitmap
}

fun saveBitmapToFile(bitmap: Bitmap, filePath: String) {
    val file = File(filePath)
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    outputStream.close()
}
