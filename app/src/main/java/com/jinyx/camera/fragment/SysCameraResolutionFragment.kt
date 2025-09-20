package com.jinyx.camera.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.jinyx.camera.R
import com.jinyx.camera.activity.PreviewActivity
import com.jinyx.camera.adapter.CameraResolutionAdapter
import com.jinyx.camera.bean.CameraResolution
import com.jinyx.camera.bean.CameraSysInfo
import com.jinyx.camera.bean.ItemEntity
import com.jinyx.camera.listener.OnItemClickListener
import com.jinyx.camera.utils.Camera2Finder

class SysCameraResolutionFragment : Fragment() {

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
        val cameraList = Camera2Finder.getCameraList(requireContext())
        if (!cameraList.isNullOrEmpty()) {
            val list = mutableListOf<ItemEntity>()
            for (camera in cameraList) {
                val camera2Info = CameraSysInfo(camera)
                list.add(camera2Info)
                camera.previewSizes?.sortedByDescending {
                    it.width * it.height
                }?.forEach { size ->
                    list.add(CameraResolution(camera2Info, size.width, size.height))
                }
            }
            adapter.setItem(list)
        }
    }

    private val onItemClickListener = object : OnItemClickListener<CameraResolution> {
        override fun onItemClick(v: View, position: Int, item: CameraResolution) {
            val parent = item.parent as CameraSysInfo
            PreviewActivity.previewCameraSys(requireContext(), parent.cameraInfo.cameraId, item.width, item.height)
        }
    }

}