package com.hsj.camera.bean;

import java.util.ArrayList;
import java.util.List;

public class PreviewSize {
    private final int pixelFormat;
    private final int width;
    private final int height;
    private final List<FrameRate> frameRates;

    public PreviewSize(int pixelFormat, int width, int height, ArrayList<FrameRate> frameRates) {
        this.pixelFormat = pixelFormat;
        this.width = width;
        this.height = height;
        this.frameRates = frameRates;
    }

    /**
     * 仅支持:
     * {@link com.hsj.camera.CameraAPI#FRAME_FORMAT_MJPEG}
     * {@link com.hsj.camera.CameraAPI#FRAME_FORMAT_YUYV}
     * 返回的 pixelFormat = 2 对应 h264，但是没有对应的预览，编解码支持
     */
    public int getPixelFormat() {
        return pixelFormat;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<FrameRate> getFrameRates() {
        return frameRates;
    }

    @Override
    public String toString() {
        return "PreviewSize{" +
            "pixelFormat=" + pixelFormat +
            ", width=" + width +
            ", height=" + height +
            ", frameRates=" + frameRates +
            '}';
    }
}
