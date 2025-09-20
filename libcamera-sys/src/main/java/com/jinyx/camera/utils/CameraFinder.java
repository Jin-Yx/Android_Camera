package com.jinyx.camera.utils;

import android.hardware.Camera;
import android.util.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraFinder {

	public static List<CameraInfo> getCameraList() {
		int num = Camera.getNumberOfCameras();
		if (num <= 0) {
			return null;
		}
		List<CameraInfo> cameraInfoList = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(i, cameraInfo);
			int facing = cameraInfo.facing;
			Camera camera = Camera.open(i);
			if (camera != null) {
				List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
				Size[] sizes = new Size[previewSizes.size()];
				for (int j = 0; j < previewSizes.size(); j++) {
					Camera.Size size = previewSizes.get(j);
					sizes[j] = new Size(size.width, size.height);
				}
				camera.release();
				cameraInfoList.add(new CameraInfo(i, facing, sizes));
			}
		}
		return cameraInfoList;
	}


	public static class CameraInfo {
		private final int cameraId;
		private final int facing;
		private final Size[] previewSizes;

		public CameraInfo(int cameraId, int facing, Size[] previewSizes) {
			this.cameraId = cameraId;
			this.facing = facing;
			this.previewSizes = previewSizes;
		}

		public int getCameraId() {
			return cameraId;
		}

		public int getFacing() {
			return facing;
		}

		public Size[] getPreviewSizes() {
			return previewSizes;
		}

		@Override
		public String toString() {
			return "CameraInfo{" +
				"cameraId=" + cameraId +
				", facing=" + facing +
				", previewSizes=" + Arrays.toString(previewSizes) +
				'}';
		}
	}

}
