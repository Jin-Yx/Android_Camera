package com.jinyx.camera.bean

import java.io.File

class CameraV4L2Info(
    val videoNode: File,
    val cameraName: String
) : ItemEntity {

    override fun getViewType(): Int = EItemViewType.CAMERA_V4L2_INFO

}