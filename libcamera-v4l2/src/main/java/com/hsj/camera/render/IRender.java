package com.hsj.camera.render;

import android.opengl.GLSurfaceView;
import com.hsj.camera.callback.ISurfaceCallback;

/**
 * @Author:Hsj
 * @Date:2021/5/10
 * @Class:IRender
 * @Desc:
 */
public interface IRender extends GLSurfaceView.Renderer {
     void onRender(boolean isResume);
     void setSurfaceCallback(ISurfaceCallback callback);
}
