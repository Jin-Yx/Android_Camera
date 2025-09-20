package com.jinyx.camera.bean

import com.hsj.camera.CameraAPI

class CameraResolution(
    val parent: ItemEntity, val width: Int, val height: Int, val pixelFormat: Int? = null
) : ItemEntity {

    val resolution: String = if (pixelFormat != null) {
        "$width x $height (${if (pixelFormat == CameraAPI.FRAME_FORMAT_MJPEG) "MJPEG" else "YUYV"})"
    } else {
        "$width x $height"
    }

    override fun getViewType(): Int = EItemViewType.CAMERA_RESOLUTION

}