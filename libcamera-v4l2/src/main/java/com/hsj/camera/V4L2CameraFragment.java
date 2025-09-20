package com.hsj.camera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.hsj.camera.bean.FrameRate;
import com.hsj.camera.bean.PreviewSize;
import com.hsj.camera.callback.IFrameCallback;
import com.hsj.camera.callback.OnCameraStateChangeListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class V4L2CameraFragment extends Fragment implements SurfaceHolder.Callback {

	private static final String EXTRA_VIDEO_NODE = "video_node";
	private static final String EXTRA_DEVICE_ID = "device_id";
	private static final String EXTRA_VENDOR_ID = "vendor_id";
	private static final String EXTRA_PRODUCT_ID = "product_id";
	private static final String EXTRA_FRAME_FORMAT = "frame_format";
	private static final String EXTRA_PREVIEW_WIDTH = "preview_width";
	private static final String EXTRA_PREVIEW_HEIGHT = "preview_height";
	private static final String EXTRA_FRAME_RATE = "frame_rate";

	private static final String TAG = "v4l2Camera";

	private CameraAPI cameraAPI;
	private OnCameraStateChangeListener onCameraStateChangeListener;
	private IFrameCallback frameCallback;

	private SurfaceHolder surfaceHolder;

	private V4L2CameraFragment() {
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return LayoutInflater.from(requireContext()).inflate(R.layout.fragment_v4l2, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		cameraAPI = new CameraAPI();
		String createError = createCamera();
		if (TextUtils.isEmpty(createError)) {
			surfaceHolder = ((SurfaceView) view.findViewById(R.id.sfvV4L2)).getHolder();
			surfaceHolder.addCallback(this);
		} else {
			notifyCameraStateChange(false, createError);
		}
	}

	@Override
	public void surfaceCreated(@NonNull SurfaceHolder holder) {
		Log.d(TAG, "surface create");
		startPreviewCamera(holder.getSurface());
	}

	@Override
	public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
		Log.d(TAG, "surface destroy");
		stopPreviewCamera();
	}

	private String createCamera() {
		String videoNodePath = requireArguments().getString(EXTRA_VIDEO_NODE);
		if (TextUtils.isEmpty(videoNodePath)) {
			List<UsbDevice> usbDeviceList = findUsbCameraDevices();
			if (usbDeviceList.isEmpty()) {
				return "no usb camera device found";
			} else if (usbDeviceList.size() > 1) {
				return "more than one usb camera device found";
			} else {
				UsbDevice usbDevice = usbDeviceList.get(0);
				if (!cameraAPI.create(usbDevice.getProductId(), usbDevice.getVendorId())) {
					return "create camera failed: " + usbDevice.getProductName() + String.format("(%04X:%04X)", usbDevice.getVendorId(), usbDevice.getProductId());
				}
			}
		} else {
			File videoNode = new File(videoNodePath);
			if (!videoNode.exists()) {
				return "video node not found: " + videoNodePath;
			} else if (!videoNode.canRead() || !videoNode.canWrite()) {
				return "video node no permission: " + videoNodePath;
			} else if (!cameraAPI.create(videoNodePath)) {
				return "create camera failed: " + videoNodePath;
			}
		}
		return null;
	}

	private void startPreviewCamera(Surface surface) {
		stopPreviewCamera();
		Log.i(TAG, "start preview " + Thread.currentThread().getName());
		PreviewSize preview = findPreviewSize(cameraAPI.getPreviewSize());
		if (preview == null) {
			notifyCameraStateChange(false, "no preview size found");
		} else {
			int frameRate = getFrameRate(preview);
			Log.d(TAG, "preview format = " + preview.getPixelFormat() + ", size = " + preview.getWidth() + "x" + preview.getHeight() + ", frameRate = " + frameRate);
			cameraAPI.setFrameSize(preview.getWidth(), preview.getHeight(), preview.getPixelFormat(), frameRate);
			if (frameCallback != null) {
				cameraAPI.setFrameCallback(frameCallback);
			}
			cameraAPI.setPreview(surface);
			if (cameraAPI.start()) {
				notifyCameraStateChange(true, "");
			} else {
				notifyCameraStateChange(false, "start preview failed");
			}
		}
	}

	private void stopPreviewCamera() {
		Log.i(TAG, "stop preview " + Thread.currentThread().getName());
		cameraAPI.stop();
	}

	private void notifyCameraStateChange(boolean isOpened, String error) {
		if (onCameraStateChangeListener != null) {
			onCameraStateChangeListener.onCameraStateChange(isOpened, error);
		}
	}

	protected List<UsbDevice> findUsbCameraDevices() {
		UsbManager usbManager = (UsbManager) requireContext().getSystemService(Context.USB_SERVICE);
		Map<String, UsbDevice> usbDeviceMap = usbManager.getDeviceList();
		int deviceId = requireArguments().getInt(EXTRA_DEVICE_ID, -1);
		int vendorId = requireArguments().getInt(EXTRA_VENDOR_ID, -1);
		int productId = requireArguments().getInt(EXTRA_PRODUCT_ID, -1);
		List<UsbDevice> usbDeviceList = new ArrayList<>();
		for (UsbDevice usbDevice : usbDeviceMap.values()) {
			if ((deviceId == -1 || usbDevice.getDeviceId() == deviceId) && usbDevice.getVendorId() == vendorId && usbDevice.getProductId() == productId) {
				usbDeviceList.add(usbDevice);
			}
		}
		return usbDeviceList;
	}

	protected PreviewSize findPreviewSize(List<PreviewSize> previewSizes) {
		if (previewSizes == null || previewSizes.isEmpty()) {
			return null;
		}
		int format = requireArguments().getInt(EXTRA_FRAME_FORMAT, CameraAPI.FRAME_FORMAT_MJPEG);
		int previewWidth = requireArguments().getInt(EXTRA_PREVIEW_WIDTH, 1920);
		int previewHeight = requireArguments().getInt(EXTRA_PREVIEW_HEIGHT, 1080);
		PreviewSize firstMJPEG = null;
		PreviewSize firstYUYV = null;
		for (PreviewSize previewSize : previewSizes) {
			if (previewSize.getPixelFormat() == format && previewSize.getWidth() == previewWidth && previewSize.getHeight() == previewHeight) {
				return previewSize;
			} else if (firstMJPEG == null && previewSize.getPixelFormat() == CameraAPI.FRAME_FORMAT_MJPEG) {
				firstMJPEG = previewSize;
			} else if (firstYUYV == null && previewSize.getPixelFormat() == CameraAPI.FRAME_FORMAT_YUYV) {
				firstYUYV = previewSize;
			}
		}
		return firstMJPEG != null ? firstMJPEG : firstYUYV;
	}

	private int getFrameRate(PreviewSize previewSize) {
		int targetFrameRate = requireArguments().getInt(EXTRA_FRAME_RATE, -1);
		if (targetFrameRate > 0) {
			for (FrameRate frameRate : previewSize.getFrameRates()) {
				if (frameRate.getDenominator() == targetFrameRate) {
					return targetFrameRate;
				}
			}
		}
		// 原先 CameraAPI.cpp 中默认帧率
		return previewSize.getPixelFormat() == CameraAPI.FRAME_FORMAT_MJPEG ? 30 : 10;
	}

	public void setOnCameraStateChangeListener(OnCameraStateChangeListener listener) {
		this.onCameraStateChangeListener = listener;
	}

	public void setOnCameraPreviewDataListener(IFrameCallback listener) {
		this.frameCallback = listener;
	}

	@Override
	public void onDestroy() {
		surfaceHolder.removeCallback(this);
		cameraAPI.destroy();
		cameraAPI = null;
		super.onDestroy();
	}

	public static class Builder {

		private final Bundle bundle;

		public Builder() {
			bundle = new Bundle();
		}

		public Builder setUsbDevice(UsbDevice usbDevice) {
			bundle.putInt(EXTRA_DEVICE_ID, usbDevice.getDeviceId());
			bundle.putInt(EXTRA_VENDOR_ID, usbDevice.getVendorId());
			bundle.putInt(EXTRA_PRODUCT_ID, usbDevice.getProductId());
			return this;
		}

		public Builder setUsbDeviceId(int vendorId, int productId) {
			bundle.putInt(EXTRA_VENDOR_ID, vendorId);
			bundle.putInt(EXTRA_PRODUCT_ID, productId);
			return this;
		}

		public Builder setVideoNode(String videoNode) {
			bundle.putSerializable(EXTRA_VIDEO_NODE, videoNode);
			return this;
		}

		public Builder setPreviewSize(int format, int width, int height) {
			setPreviewSize(format, width, height, -1);
			return this;
		}

		public Builder setPreviewSize(int format, int width, int height, int frameRate) {
			bundle.putInt(EXTRA_FRAME_FORMAT, format);
			bundle.putInt(EXTRA_PREVIEW_WIDTH, width);
			bundle.putInt(EXTRA_PREVIEW_HEIGHT, height);
			bundle.putInt(EXTRA_FRAME_RATE, frameRate);
			return this;
		}

		public V4L2CameraFragment build() {
			V4L2CameraFragment fragment = new V4L2CameraFragment();
			fragment.setArguments(bundle);
			return fragment;
		}

	}
}
