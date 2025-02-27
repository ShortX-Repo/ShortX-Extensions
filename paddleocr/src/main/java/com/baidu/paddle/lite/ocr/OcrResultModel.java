package com.baidu.paddle.lite.ocr;

import android.graphics.Point;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * @author PaddleOCR
 * Modified by TonyJiangWJ
 * @since 2023-08-06
 */
public class OcrResultModel {
    private final List<Point> points;
    private final List<Integer> wordIndex;
    private String label;
    private float confidence;
    private float clsIdx;
    private String clsLabel;
    private float clsConfidence;

    public OcrResultModel() {
        super();
        points = new ArrayList<>();
        wordIndex = new ArrayList<>();
    }

    public void addPoints(int x, int y) {
        Point point = new Point(x, y);
        points.add(point);
    }

    public void addWordIndex(int index) {
        wordIndex.add(index);
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Integer> getWordIndex() {
        return wordIndex;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getClsIdx() {
        return clsIdx;
    }

    public void setClsIdx(float idx) {
        this.clsIdx = idx;
    }

    public String getClsLabel() {
        return clsLabel;
    }

    public void setClsLabel(String label) {
        this.clsLabel = label;
    }

    public float getClsConfidence() {
        return clsConfidence;
    }

    public void setClsConfidence(float confidence) {
        this.clsConfidence = confidence;
    }

    @Override
    public String toString() {
        return "OcrResultModel{" +
                "points=" + points +
                ", wordIndex=" + wordIndex +
                ", label='" + label + '\'' +
                ", confidence=" + confidence +
                ", clsIdx=" + clsIdx +
                ", clsLabel='" + clsLabel + '\'' +
                ", clsConfidence=" + clsConfidence +
                '}';
    }

    public OcrResult toOcrResult() {
        int left = -1, right = -1, top = -1, bottom = -1;
        for (Point point : getPoints()) {
            if (point.x < left || left == -1) {
                left = point.x;
            }
            if (point.x > right || right == -1) {
                right = point.x;
            }
            if (point.y < top || top == -1) {
                top = point.y;
            }
            if (point.y > bottom || bottom == -1) {
                bottom = point.y;
            }
        }
        return new OcrResult(getLabel(), getConfidence(),
                new Rect(left, top, right, bottom));
    }
}