package com.jinyx.camera.bean

import com.jinyx.camera.utils.Camera2Finder.Camera2Info

class CameraSysInfo(val cameraInfo: Camera2Info) : ItemEntity {

    override fun getViewType(): Int = EItemViewType.CAMERA_SYS_INFO

}