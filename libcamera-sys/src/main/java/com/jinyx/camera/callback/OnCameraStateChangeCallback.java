package com.jinyx.camera.callback;

public interface OnCameraStateChangeCallback {
    void onCameraState(boolean isOpened, String error);
}
