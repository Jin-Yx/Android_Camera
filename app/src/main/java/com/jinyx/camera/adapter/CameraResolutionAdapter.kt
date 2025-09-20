package com.jinyx.camera.adapter

import android.hardware.camera2.CameraMetadata
import android.widget.TextView
import com.jinyx.camera.R
import com.jinyx.camera.bean.*
import com.jinyx.camera.listener.OnItemClickListener

class CameraResolutionAdapter : BaseAdapter(mapOf(
    EItemViewType.CAMERA_SYS_INFO to R.layout.item_camera_sys_info,
    EItemViewType.CAMERA_V4L2_INFO to R.layout.item_camera_v4l2_info,
    EItemViewType.CAMERA_RESOLUTION to R.layout.item_camera_resolution
)) {

    private var onItemClickListener: OnItemClickListener<CameraResolution>? = null

    override fun onItemViewBind(holder: BaseViewHolder, position: Int, item: ItemEntity) {
        when (item.getViewType()) {
            EItemViewType.CAMERA_SYS_INFO -> {
                (item as CameraSysInfo).let {
                    val txtCameraId = holder.getView<TextView>(R.id.txtCameraId)
                    val txtCameraFacing = holder.getView<TextView>(R.id.txtCameraFacing)
                    txtCameraId.text = "CameraId: ${it.cameraInfo.cameraId}"
                    txtCameraFacing.text = when (it.cameraInfo.facing) {
                        CameraMetadata.LENS_FACING_FRONT -> "前置摄像头"
                        CameraMetadata.LENS_FACING_BACK -> "后置摄像头"
                        CameraMetadata.LENS_FACING_EXTERNAL -> "外置摄像头"
                        else -> "未知摄像头"
                    }
                }
            }
            EItemViewType.CAMERA_V4L2_INFO -> {
                (item as CameraV4L2Info).let {
                    val txtCameraName = holder.getView<TextView>(R.id.txtCameraName)
                    val txtCameraNode = holder.getView<TextView>(R.id.txtCameraNode)
                    txtCameraName.text = it.cameraName
                    txtCameraNode.text = it.videoNode.absolutePath
                }
            }
            EItemViewType.CAMERA_RESOLUTION -> {
                (item as CameraResolution).let {
                    val txtCameraResolution = holder.getView<TextView>(R.id.txtCameraResolution)
                    txtCameraResolution.text = it.resolution
                    holder.itemView.setOnClickListener {
                        onItemClickListener?.onItemClick(txtCameraResolution, position, item)
                    }
                }
            }
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener<CameraResolution>) {
        onItemClickListener = listener
    }

}