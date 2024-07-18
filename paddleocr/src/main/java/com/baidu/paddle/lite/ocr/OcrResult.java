package com.baidu.paddle.lite.ocr;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author PaddleOCR
 * Modified by TonyJiangWJ
 * @since 2023-08-06
 */
public class OcrResult implements Comparable<OcrResult> {
    private String label;
    private float confidence;
    private Rect bounds;
    private final List<OcrResult> elements = new ArrayList<>();

    public OcrResult() {
    }

    public OcrResult(String label, float confidence, Rect bounds) {
        this.label = label;
        this.confidence = confidence;
        this.bounds = bounds;
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

    public Rect getBounds() {
        return bounds;
    }

    public void setBounds(Rect bounds) {
        this.bounds = bounds;
    }

    public RectLocation getLocation() {
        return new RectLocation(bounds);
    }

    public String getWords() {
        return label.trim().replace("\r", "");
    }

    public List<OcrResult> getElements() {
        return this.elements;
    }

    public void addElements(OcrResult element) {
        this.elements.add(element);
    }

    @Override
    public int compareTo(OcrResult o) {
        // 上下差距小于二分之一的高度 判定为同一行
        int deviation = Math.max(this.bounds.height(), o.bounds.height()) / 2;
        // 通过垂直中心点的距离判定
        if (Math.abs((this.bounds.top + this.bounds.bottom) / 2 - (o.bounds.top + o.bounds.bottom) / 2) < deviation) {
            return this.bounds.left - o.bounds.left;
        } else {
            return this.bounds.bottom - o.bounds.bottom;
        }
    }

    @Override
    public String toString() {
        return "OcrResult{" + "label='" + label + '\'' +
                ", confidence=" + confidence +
                ", bounds=" + bounds +
                ", elements=" + elements +
                '}';
    }

    public static class RectLocation {
        public int left;
        public int top;
        public int width;
        public int height;

        public RectLocation() {
        }

        public RectLocation(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        public RectLocation(Rect rect) {
            left = rect.left;
            top = rect.top;
            width = rect.right - rect.left;
            height = rect.bottom - rect.top;
        }
    }

    public tornaco.apps.shortx.core.proto.common.OcrResult toProtoResult() {
        tornaco.apps.shortx.core.proto.common.OcrResult.Builder builder = tornaco.apps.shortx.core.proto.common.OcrResult.newBuilder();
        builder.setLabel(label).setConfidence(confidence)
                .setBounds(tornaco.apps.shortx.core.proto.common.Rect.newBuilder()
                        .setLeft(bounds.left)
                        .setRight(bounds.right)
                        .setTop(bounds.top)
                        .setBottom(bounds.bottom)
                        .build())
                .addAllElements(elements.stream().map(OcrResult::toProtoResult).collect(Collectors.toList()));
        return builder.build();
    }
}
