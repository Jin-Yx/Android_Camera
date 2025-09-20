package com.jinyx.camera.callback;

import androidx.annotation.NonNull;

public interface OnImagePreviewCallback {

    /**
     * @param format    PixelFormat.RGBA_8888(CameraX) 或 ImageFormat.NV21
     * @param data      RGBA 或 NV21 图像数据
     * @param width     图像宽度
     * @param height    图像高度
     * @param rotation  预览的画面和返回的数据之间，需要顺时针旋转的角度，一般竖直相机需要旋转，前置旋转270，后置旋转90
     * @param flip      预览的画面和返回的数据之间，是否需要水平翻转，一般前置需要反转
     */
    void onImagePreview(int format, @NonNull byte[] data, int width, int height, int rotation, boolean flip);

}
