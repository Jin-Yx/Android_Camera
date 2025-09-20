package com.jinyx.camera.utils;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Camera2Finder {

	public static List<Camera2Info> getCameraList(Context context) {
		CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
		if (cameraManager == null) {
			return null;
		}
		String[] cameraIds = null;
		try {
			cameraIds = cameraManager.getCameraIdList();
		} catch (CameraAccessException ignore) {}
		if (cameraIds == null) {
			return null;
		}
		List<Camera2Info> cameraInfoList = new ArrayList<>();
		for (String cameraId : cameraIds) {
			try {
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
				int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
				Size[] previewSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
				cameraInfoList.add(new Camera2Info(cameraId, facing, previewSizes));
			} catch (CameraAccessException ignore) {}
		}
		return cameraInfoList;
	}

	public static class Camera2Info {
		private final String cameraId;
		private final int facing;
		private final Size[] previewSizes;

		public Camera2Info(String cameraId, int facing, Size[] previewSizes) {
			this.cameraId = cameraId;
			this.facing = facing;
			this.previewSizes = previewSizes;
		}

		public String getCameraId() {
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
			return "Camera2Info{" +
				"cameraId='" + cameraId + '\'' +
				", facing=" + facing +
				", previewSizes=" + Arrays.toString(previewSizes) +
				'}';
		}
	}

}
