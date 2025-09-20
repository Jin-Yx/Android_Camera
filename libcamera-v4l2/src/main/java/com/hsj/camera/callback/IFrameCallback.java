package com.hsj.camera.callback;

import java.nio.ByteBuffer;

/**
 * @Author:Hsj
 * @Date:2021/5/11
 * @Class:IFrameCallback
 * @Desc:
 */
public interface IFrameCallback {
    void onFrame(int format, int width, int height, ByteBuffer data);
}
