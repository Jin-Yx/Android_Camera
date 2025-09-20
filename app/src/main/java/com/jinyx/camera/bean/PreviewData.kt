package com.jinyx.camera.bean

data class PreviewData(
    val nv21: ByteArray,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val flip: Boolean
)