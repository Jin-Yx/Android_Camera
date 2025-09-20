package com.jinyx.camera.fragment

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.hsj.camera.CameraAPI
import com.hsj.camera.utils.CameraFinder
import com.jinyx.camera.R
import com.jinyx.camera.activity.PreviewActivity
import com.jinyx.camera.adapter.CameraResolutionAdapter
import com.jinyx.camera.bean.CameraResolution
import com.jinyx.camera.bean.CameraV4L2Info
import com.jinyx.camera.bean.ItemEntity
import com.jinyx.camera.listener.OnItemClickListener
import java.io.File

class V4L2CameraResolutionFragment : Fragment() {

    private val adapter by lazy {
        CameraResolutionAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_resolution, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rcyCameraResolution = view.findViewById<RecyclerView>(R.id.rcyCameraResolution)
        rcyCameraResolution.adapter = adapter
        adapter.setOnItemClickListener(onItemClickListener)

        loadCameraList()
    }

    private fun loadCameraList() {
        val videoNodeNameMap = mutableMapOf<String, String>()
        (requireContext().getSystemService(Context.USB_SERVICE) as UsbManager?)?.let { usbManager ->
            usbManager.deviceList?.forEach { (_, device) ->
                CameraFinder.findVideoNode(device.vendorId, device.productId)?.forEach { videoNode ->
                    videoNodeNameMap[videoNode.absolutePath] = device.productName ?: "NaN"
                }
            }
        }
        val list = mutableListOf<ItemEntity>()
        videoNodeNameMap.forEach { (node, name) ->
            val videoNode = File(node)
            val previewSizeList = CameraFinder.getPreviewSize(videoNode)
            // usb 相机设备可能存在两个 video 节点，其中一个可以预览(可以获取到分辨率信息)，一个不可以；
            if (!previewSizeList.isNullOrEmpty()) {
                val cameraV4L2Info = CameraV4L2Info(videoNode, name)
                list.add(cameraV4L2Info)
                previewSizeList.filter {    // 预览只支持 MJPEG 和 YUYV
                    it.pixelFormat == CameraAPI.FRAME_FORMAT_MJPEG || it.pixelFormat == CameraAPI.FRAME_FORMAT_YUYV
                }.sortedByDescending {
                    it.width * it.height
                }.sortedBy {
                    it.pixelFormat
                }.forEach {
//                    previewSize.frameRates    // 分辨率支持的帧率信息，未处理，按照默认 MJPEG 30 YUYV 10 设置
                    list.add(CameraResolution(cameraV4L2Info, it.width, it.height, it.pixelFormat))
                }
            }
        }
        if (list.isNotEmpty()) {
            adapter.setItem(list)
        } else if (videoNodeNameMap.isNotEmpty()) {
            Toast.makeText(requireContext(), "请检查 /dev/video 节点权限", Toast.LENGTH_SHORT).show()
        }
    }

    private val onItemClickListener = object : OnItemClickListener<CameraResolution> {
        override fun onItemClick(v: View, position: Int, item: CameraResolution) {
            val parent = item.parent as CameraV4L2Info
            PreviewActivity.previewCameraV4L2(requireContext(), parent.videoNode, item.pixelFormat!!, item.width, item.height)
        }
    }

}