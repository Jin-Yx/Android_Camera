package com.jinyx.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.jinyx.camera.callback.OnCameraStateChangeCallback;
import com.jinyx.camera.callback.OnImagePreviewCallback;
import com.jinyx.camera.utils.Yuv420_888ToNv21Utils;
import com.jinyx.camera.widgets.FocusView;
import com.jinyx.camerax.R;

import java.util.ArrayList;
import java.util.List;

public class Camera2Fragment extends Fragment {

    private static final String TAG = "q-camera";

    private static final String KEY_CAMERA_ID = "camera_id";
    private static final String KEY_PREVIEW_WIDTH = "preview_width";
    private static final String KEY_PREVIEW_HEIGHT = "preview_height";
    private static final String KEY_NEED_FOCUS_EXPOSURE = "need_focus_exposure";
    private static final String KEY_NEED_PREVIEW_DATA = "need_preview_data";
    private static final String KEY_FLIP_FRONT_CAMERA = "flip_front_camera";

    private static final int DEFAULT_PREVIEW_WIDTH = 640;
    private static final int DEFAULT_PREVIEW_HEIGHT = 480;

    public static class Builder {
        private final Bundle bundle = new Bundle();

        public Builder(String cameraId) {
            bundle.putString(KEY_CAMERA_ID, cameraId);
        }

        public Builder setPreviewSize(int previewWidth, int previewHeight) {
            bundle.putInt(KEY_PREVIEW_WIDTH, previewWidth);
            bundle.putInt(KEY_PREVIEW_HEIGHT, previewHeight);
            return this;
        }

        public Builder useFocusExposure() {
            bundle.putBoolean(KEY_NEED_FOCUS_EXPOSURE, true);
            return this;
        }

        public Builder usePreviewData() {
            bundle.putBoolean(KEY_NEED_PREVIEW_DATA, true);
            return this;
        }

        public Builder flipFrontCamera() {
            bundle.putBoolean(KEY_FLIP_FRONT_CAMERA, true);
            return this;
        }

        public Camera2Fragment build() {
            Camera2Fragment fragment = new Camera2Fragment();
            fragment.setArguments(bundle);
            return fragment;
        }
    }

    private TextureView textureCamera2;
    private CameraManager cameraManager;

    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;

    private static OnImagePreviewCallback onImagePreviewCallback;
    private static OnCameraStateChangeCallback onCameraStateChangeCallback;

    private String cameraId;
    private int previewWidth = DEFAULT_PREVIEW_WIDTH, previewHeight = DEFAULT_PREVIEW_HEIGHT;

    private int rotation = 0;
    private boolean flip = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(requireContext()).inflate(R.layout.fragment_camera2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textureCamera2 = view.findViewById(R.id.textureCamera2);
        FocusView focusView = view.findViewById(R.id.focusView);

        cameraId = requireArguments().getString(KEY_CAMERA_ID);
        previewWidth = requireArguments().getInt(KEY_PREVIEW_WIDTH, DEFAULT_PREVIEW_WIDTH);
        previewHeight = requireArguments().getInt(KEY_PREVIEW_HEIGHT, DEFAULT_PREVIEW_HEIGHT);

        cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
        if (checkCameraId(cameraId)) {
            if (requireArguments().getBoolean(KEY_NEED_FOCUS_EXPOSURE, false)) {
                focusView.setOnFocusViewListener(focusViewListener);
            }
            textureCamera2.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private boolean checkCameraId(String cameraId) {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing == CameraMetadata.LENS_FACING_FRONT) {
                rotation = isPortrait ? 270 : 0;
                flip = !requireArguments().getBoolean(KEY_FLIP_FRONT_CAMERA, false);
                if (!flip) { // Android 前置相机默认镜像，flip true 表示预览画面和实际数据需要翻转；false 则是预览画面已经翻转，和实际数据一致
                    textureCamera2.setScaleX(-1F);
                }
            } else if (facing == CameraMetadata.LENS_FACING_BACK) {
                rotation = isPortrait ? 90 : 0;
            }
            StreamConfigurationMap scm = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = null;
            if (scm != null) {
                sizes = scm.getOutputSizes(SurfaceTexture.class);
            }
            if (sizes == null || sizes.length == 0) {
                onCameraState(false, "未找到可用的预览尺寸");
                return false;
            }
            Size previewSize = null;
            for (Size size : sizes) {
                if (size.getWidth() == previewWidth && size.getHeight() == previewHeight) {
                    previewSize = size;
                    break;
                } else if (size.getWidth() == DEFAULT_PREVIEW_WIDTH && size.getHeight() == DEFAULT_PREVIEW_HEIGHT) {
                    previewSize = size;
                }
            }
            if (previewSize == null) {
                onCameraState(false, "不支持的分辨率 " + previewWidth + "x" + previewHeight);
                return false;
            }
            previewWidth = previewSize.getWidth();
            previewHeight = previewSize.getHeight();
            Log.d(TAG, "camera id = " + cameraId + ", preview size = " + previewWidth + "x" + previewHeight);
            return true;
        } catch (CameraAccessException e) {
            onCameraState(false, "加载相机失败: " + e.getMessage());
            return false;
        }
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "texture size = " + width + "x" + height + ", preview size = " + previewWidth + "x" + previewHeight);
            surface.setDefaultBufferSize(previewWidth, previewHeight);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            if (textureCamera2.getSurfaceTexture() != null) {
                textureCamera2.getSurfaceTexture().release();
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };

    private void openCamera() {
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    List<Surface> surfaceList = new ArrayList<>();
                    surfaceList.add(new Surface(textureCamera2.getSurfaceTexture()));
                    if (requireArguments().getBoolean(KEY_NEED_PREVIEW_DATA, false)) {
                        initImageReader();
                        if (imageReader != null) {
                            surfaceList.add(imageReader.getSurface());
                        }
                    }
                    initCaptureSession(camera, surfaceList);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    onCameraState(false, "相机连接已断开");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice = camera;
                    onCameraState(false, "打开相机异常: " + error);
                }
            }, null);
        } catch (CameraAccessException e) {
            onCameraState(false, "打开相机失败: " + e.getMessage());
        }
    }

    private void initImageReader() {
        imageReader = ImageReader.newInstance(previewWidth, previewHeight, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (onImagePreviewCallback != null) {
                    Image image = imageReader.acquireNextImage();
                    byte[] nv21 = Yuv420_888ToNv21Utils.yuv420ToNv21(image);
                    if (nv21 != null && nv21.length > 0) {
                        onImagePreviewCallback.onImagePreview(ImageFormat.NV21, nv21, image.getWidth(), image.getHeight(), rotation, flip);
                    }
                    image.close();
                }
            }
        }, null);
    }

    private void initCaptureSession(CameraDevice camera, List<Surface> surfaceList) {
        try {
            camera.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    try {
                        captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        for (Surface surface : surfaceList) {
                            captureRequestBuilder.addTarget(surface);
                        }
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                        onCameraState(true, "相机打开成功");
                    } catch (CameraAccessException e) {
                        onCameraState(false, "预览相机失败: " + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    onCameraState(false, "预览相机失败");
                }
            }, null);
        } catch (CameraAccessException e) {
            onCameraState(false, "创建预览会话失败: " + e.getMessage());
        }
    }

    private void closeCamera() {
        Log.d(TAG, "close camera start");
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        captureRequestBuilder = null;
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        Log.d(TAG, "close camera end");
    }

    @Override
    public void onDestroy() {
        closeCamera();
        super.onDestroy();
    }

    private final FocusView.FocusViewListener focusViewListener = new FocusView.FocusViewListener() {
        @Override
        public void onFocusChange(float x, float y, int width, int height) {
            Camera2Fragment.this.onFocusChange(x, y, width, height);
        }

        @Override
        public void onExposureChange(float exposure) {
            Camera2Fragment.this.onExposureChange(exposure);
        }
    };

    private void onFocusChange(float x, float y, int width, int height) {
        if (cameraDevice == null || cameraCaptureSession == null || captureRequestBuilder == null) {
            return;
        }
        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);

            int previewWidth = this.previewWidth;
            int previewHeight = this.previewHeight;
            if ((previewWidth > previewHeight && width < height) || (previewWidth < previewHeight && width > height)) {
                int temp = previewWidth;
                previewWidth = previewHeight;
                previewHeight = temp;
            }
            int areaSize = 100;
            float realX = x * previewWidth / width;
            float realY = y * previewHeight / height;
            int left = Math.max((int) (realX - areaSize), 0);
            int right = Math.min((int) (realX + areaSize), width);
            int top = Math.max((int) (realY - areaSize), 0);
            int bottom = Math.min((int) (realY + areaSize), height);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{
                new MeteringRectangle(new Rect(left, top, right, bottom), 1000)
            });
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{
                new MeteringRectangle(new Rect(left, top, right, bottom), 1000)
            });
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException ignore) {}
    }

    private void onExposureChange(float exposure) {
        if (cameraDevice == null || cameraCaptureSession == null || captureRequestBuilder == null) {
            return;
        }
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Range<Integer> range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            int value = (int) (exposure * (range.getUpper() - range.getLower()) / 2);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, value);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException ignore) {}
    }

    private void onCameraState(boolean isOpened, String error) {
        if (onCameraStateChangeCallback != null) {
            onCameraStateChangeCallback.onCameraState(isOpened, error);
        }
    }

    public void setOnCameraStateChangeCallback(OnCameraStateChangeCallback callback) {
        onCameraStateChangeCallback = callback;
    }

    public void setOnImagePreviewCallback(OnImagePreviewCallback callback) {
        onImagePreviewCallback = callback;
    }

}
