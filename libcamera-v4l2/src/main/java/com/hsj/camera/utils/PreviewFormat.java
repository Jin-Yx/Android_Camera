package com.hsj.camera.utils;

import java.nio.ByteBuffer;

public class PreviewFormat {

	/**
	 * @param data		// 使用 MJPEG 格式预览返回的图像数据，sdk 内部已解码成 YUV422
	 * @param width		// 图像宽度
	 * @param height	// 图像高度
	 *
	 * YYYYYYYY         YYYYYYYY
	 * UUUU       转成   VUVU
	 * VVVV
	 */
	public static byte[] mjpegToNV21(ByteBuffer data, int width, int height) {
		byte[] yuv = new byte[data.limit()];
		data.get(yuv);
		if (yuv.length == width * height * 3 / 2) {
			return nv12ToNV21(yuv, width, height);
		}
		// yuv422 水平每两个 y 共用一个 uv；每个像素占用 2 个字节
		// nv21 水平+垂直每四个 y 共用一个 uv；每个像素占用 1.5 个字节
		int yLen = yuv.length / 2;
		byte[] nv21 = new byte[yLen * 3 / 2];
		// 因此前面一半内存的 y 值直接复制
		System.arraycopy(yuv, 0, nv21, 0, yLen);
		// 通过抓 sdk 中的日志，测试 uv 的 stride 是 width 的 1/2，并且因为共用关系，需要每隔一组 stride 丢弃一组 uv 值
		int stride = width / 2;
		for (int i = 0; i < yLen / 4; i++) {
			int row = 2 * i / stride * stride;
			int col = i % stride;
			nv21[yLen + i * 2 + 1] = yuv[yLen + row + i % stride];	// u
			nv21[yLen + i * 2] = yuv[yLen * 3 / 2 + row + col];	// v
		}
		return nv21;
	}

	public static byte[] yuyvToNV21(ByteBuffer data, int width, int height) {
		byte[] yuyv = new byte[data.limit()];
		data.get(yuyv);
		if (yuyv.length != width * height * 2) {
			return yuyv;
		}
		int yLen = yuyv.length / 2;
		byte[] nv21 = new byte[yLen * 3 / 2];
		for(int i = 0; i < yuyv.length; i += 4) {
			nv21[i / 2] = yuyv[i];
			nv21[i / 2 + 1] = yuyv[i + 2];
			// 一行 width 个像素，每个像素 yuyv 占用 2 字节；nv21 上下两个像素共用一个 uv
			int line = i / (2 * width);
			if (line % 2 == 0) {
				int x = line * width / 2;
				int y = i / 4;
				nv21[yLen + 2 * y - x] = yuyv[i + 3];
				nv21[yLen + 2 * y - x + 1] = yuyv[i + 1];
			}

		}
		return nv21;
	}

	private static byte[] nv12ToNV21(byte[] nv12, int width, int height) {
		for (int i = width * height; i < nv12.length; i += 2) {
			byte u = nv12[i];
			byte v = nv12[i + 1];
			nv12[i] = v;
			nv12[i + 1] = u;
		}
		return nv12;
	}

}
