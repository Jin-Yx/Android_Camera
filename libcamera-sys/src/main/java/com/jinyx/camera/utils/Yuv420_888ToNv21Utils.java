package com.jinyx.camera.utils;

import android.media.Image;
import androidx.camera.core.ImageProxy;

public class Yuv420_888ToNv21Utils {

    public static byte[] yuv420ToNv21(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = imageProxy.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = imageProxy.getPlanes()[2];

        byte[] yBuffer = new byte[yPlane.getBuffer().remaining()];
        yPlane.getBuffer().get(yBuffer, 0, yBuffer.length);
        byte[] uBuffer = new byte[uPlane.getBuffer().remaining()];
        uPlane.getBuffer().get(uBuffer, 0, uBuffer.length);
        byte[] vBuffer = new byte[vPlane.getBuffer().remaining()];
        vPlane.getBuffer().get(vBuffer, 0, vBuffer.length);

        return yuv420ToNv21(
            yBuffer, uBuffer, vBuffer, imageProxy.getWidth(), imageProxy.getHeight(),
            yPlane.getRowStride(), uPlane.getRowStride(), vPlane.getRowStride(),
            yPlane.getPixelStride(), uPlane.getPixelStride(), vPlane.getPixelStride()
        );
    }

    public static byte[] yuv420ToNv21(Image image) {
        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        byte[] yBuffer = new byte[yPlane.getBuffer().remaining()];
        yPlane.getBuffer().get(yBuffer, 0, yBuffer.length);
        byte[] uBuffer = new byte[uPlane.getBuffer().remaining()];
        uPlane.getBuffer().get(uBuffer, 0, uBuffer.length);
        byte[] vBuffer = new byte[vPlane.getBuffer().remaining()];
        vPlane.getBuffer().get(vBuffer, 0, vBuffer.length);

        return yuv420ToNv21(
            yBuffer, uBuffer, vBuffer, image.getWidth(), image.getHeight(),
            yPlane.getRowStride(), uPlane.getRowStride(), vPlane.getRowStride(),
            yPlane.getPixelStride(), uPlane.getPixelStride(), vPlane.getPixelStride()
        );
    }

    private static byte[] yuv420ToNv21(
        byte[] yBuffer, byte[] uBuffer, byte[] vBuffer, int width, int height,
        int yRowStride, int uRowStride, int vRowStride,
        int yPixelStride, int uPixelStride, int vPixelStride
    ) {
        // 640 * 480: y=307200, u=153599, v=153599  (yuvRowStride=640)
        // 480 * 640: y=327648, u=163807, v=163807  (yuvRowStride=512)
        byte[] nv21 = new byte[width * height * 3 / 2];
        byte[] y = getYUVBuffer(yBuffer, yPixelStride, yRowStride, width, width * height);
        System.arraycopy(y, 0, nv21, 0, y.length);
        // uv pixelStride = 2, 则表明 uvBuffer 中 uv 间隔存在；NV21 使用 vBuffer, NV12 使用 uBuffer
        if (uPixelStride == 2 && vPixelStride == 2 && yRowStride == uRowStride && yRowStride == vRowStride && yRowStride == width) {
            // 最后一位空；实际补不补看不出啥区别
            vBuffer[vBuffer.length - 1] = uBuffer[uBuffer.length - 2];
            System.arraycopy(vBuffer, 0, nv21, y.length, vBuffer.length);
        } else {
            byte[] u = getYUVBuffer(uBuffer, uPixelStride, uRowStride, width, width * height / 4);
            byte[] v = getYUVBuffer(vBuffer, vPixelStride, vRowStride, width, width * height / 4);
            for (int i = 0; i < u.length; i++) {    // NV21 => VU..VU 、NV12 => UV..UV
                nv21[y.length + i * 2] = v[i];
                nv21[y.length + i * 2 + 1] = u[i];
            }
        }
        return nv21;
    }

    /**
     * uv pixelStride 如果是 2，表示 uBuffer 或 vBuffer 里面会间隔插入一个 v 或 u；
     * 因此可以直接将 vBuffer 当做 uvBuffer, 最后再增加一个 uBuffer[uBuffer.length - 1]
     * 如果是 NV12，则是 uBuffer 当做 uvBuffer, 最后再增加一个 vBuffer[vBuffer.length - 1]
     * {@see https://blog.csdn.net/weekend_y45/article/details/125079916}
     */
    private static byte[] getYUVBuffer(byte[] buffer, int pixelStride, int rowStride, int width, int len) {
        if (pixelStride == 1 && buffer.length == len) { // pixelStride == 1 && rowStride == width
            return buffer;
        } else {
            byte[] yuv = new byte[len];
            for (int i = 0; i < len; i++) {
                yuv[i] = buffer[i * pixelStride + i / pixelStride * (rowStride - width)];
            }
            return yuv;
        }
    }

}
