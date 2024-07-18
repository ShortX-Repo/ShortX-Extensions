package autojs.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Log;

import com.baidu.paddle.lite.ocr.OcrResult;
import com.baidu.paddle.lite.ocr.Predictor;

import java.util.Collections;
import java.util.List;

import autojs.image.ImageWrapper;

/**
 * @author TonyJiangWJ
 * @since 2023-08-06
 */
public class OcrPaddle {
    private final Predictor predictor = new Predictor();
    private final Context context;

    public OcrPaddle(Context context) {
        this.context = context;
    }

    public synchronized boolean init(boolean useSlim) {
        if (!predictor.isLoaded || useSlim != predictor.isUseSlim()) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                VolatileDispose<Boolean> result = new VolatileDispose<>();
                new Thread(() -> {
                    result.setAndNotify(predictor.init(context, useSlim));
                }).start();
                return result.blockedGet(60_000);
            } else {
                return predictor.init(context, useSlim);
            }
        }
        return predictor.isLoaded;
    }

    public void release() {
        predictor.releaseModel();
    }

    public List<OcrResult> detect(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        if (predictor.cpuThreadNum != cpuThreadNum) {
            predictor.releaseModel();
            predictor.cpuThreadNum = cpuThreadNum;
        }
        init(useSlim);
        return predictor.runOcr(bitmap);
    }

    public List<OcrResult> detect(ImageWrapper image, int cpuThreadNum) {
        return detect(image, cpuThreadNum, true);
    }

    public List<OcrResult> detect(ImageWrapper image) {
        return detect(image, 4, true);
    }

    public String[] recognizeText(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        List<OcrResult> words_result = detect(image, cpuThreadNum, useSlim);
        Collections.sort(words_result);
        String[] outputResult = new String[words_result.size()];
        for (int i = 0; i < words_result.size(); i++) {
            outputResult[i] = words_result.get(i).getLabel();
            // show LOG in Logcat panel
            Log.i("outputResult", outputResult[i]);
        }
        return outputResult;
    }

    public String[] recognizeText(ImageWrapper image, int cpuThreadNum) {
        return recognizeText(image, cpuThreadNum, true);
    }

    public String[] recognizeText(ImageWrapper image) {
        return recognizeText(image, 4, true);
    }
}
