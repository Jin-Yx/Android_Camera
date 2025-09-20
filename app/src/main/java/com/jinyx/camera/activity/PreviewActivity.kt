package com.jinyx.camera.activity

import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.os.Bundle
import android.util.Size
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hsj.camera.CameraAPI
import com.hsj.camera.V4L2CameraFragment
import com.hsj.camera.callback.IFrameCallback
import com.hsj.camera.callback.OnCameraStateChangeListener
import com.hsj.camera.utils.PreviewFormat
import com.jinyx.camera.Camera2Fragment
import com.jinyx.camera.R
import com.jinyx.camera.bean.PreviewData
import com.jinyx.camera.callback.OnCameraStateChangeCallback
import com.jinyx.camera.callback.OnImagePreviewCallback
import com.jinyx.camera.image.ImageHelper
import com.jinyx.camera.utils.PreviewResolutionUtils
import com.jinyx.camera.widget.ThumbnailImageView
import java.io.File

class PreviewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CAMERA_ID = "camera_id"
        private const val EXTRA_PREVIEW_WIDTH = "camera_width"
        private const val EXTRA_PREVIEW_HEIGHT = "camera_height"
        private const val EXTRA_PREVIEW_FORMAT = "camera_pixel_format"

        fun previewCameraSys(context: Context, cameraId: String, width: Int, height: Int) {
            val intent = Intent(context, PreviewActivity::class.java)
            intent.putExtra(EXTRA_CAMERA_ID, cameraId)
            intent.putExtra(EXTRA_PREVIEW_WIDTH, width)
            intent.putExtra(EXTRA_PREVIEW_HEIGHT, height)
            context.startActivity(intent)
        }

        fun previewCameraV4L2(context: Context, videoNode: File, pixelFormat: Int, width: Int, height: Int) {
            val intent = Intent(context, PreviewActivity::class.java)
            intent.putExtra(EXTRA_CAMERA_ID, videoNode.absolutePath)
            intent.putExtra(EXTRA_PREVIEW_WIDTH, width)
            intent.putExtra(EXTRA_PREVIEW_HEIGHT, height)
            intent.putExtra(EXTRA_PREVIEW_FORMAT, pixelFormat)
            context.startActivity(intent)
        }
    }

    private lateinit var framePreview: FrameLayout
    private lateinit var imgThumbnail: ThumbnailImageView
    private lateinit var imgTakePhoto: ImageView
    private lateinit var previewSize: Size

    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        framePreview = findViewById(R.id.framePreview)
        imgThumbnail = findViewById(R.id.imgThumbnail)
        imgTakePhoto = findViewById(R.id.imgTakePhoto)
        imgTakePhoto.setOnClickListener { takePhoto() }

        val previewFormat = intent.getIntExtra(EXTRA_PREVIEW_FORMAT, -1)
        val previewWidth = intent.getIntExtra(EXTRA_PREVIEW_WIDTH, 640)
        val previewHeight = intent.getIntExtra(EXTRA_PREVIEW_HEIGHT, 480)
        val cameraId = intent.getStringExtra(EXTRA_CAMERA_ID)
        previewSize = Size(previewWidth, previewHeight)
        val previewFragment = if (previewFormat == -1) {
            Camera2Fragment.Builder(cameraId)
                .setPreviewSize(previewWidth, previewHeight)
                .usePreviewData()   // 调用会初始化 ImageReader 获取图像流数据
                .flipFrontCamera()  // 系统相机前置默认镜像，调用会将前置镜像左右翻转
                .build().also {
                    it.setOnCameraStateChangeCallback(onSysCameraStateChangeCallback)
                    it.setOnImagePreviewCallback(onSysCameraPreviewCallback)
                }
        } else {
            V4L2CameraFragment.Builder()
                .setVideoNode(cameraId)
//                .setUsbDevice() or .setUsbDeviceId()  // 不设置 videoNode 时，通过 usbDevice vid/pid 打开相机
                .setPreviewSize(previewFormat, previewWidth, previewHeight)
                // 默认 MJPEG 30帧、YUYV 10帧
//                .setPreviewSize(previewFormat, previewWidth, previewHeight, fps)
                .build().also {
                    it.setOnCameraStateChangeListener(onV4L2CameraStateChangeListener)
                    it.setOnCameraPreviewDataListener(onV4L2CameraPreviewCallback)
                }
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.framePreview, previewFragment)
        transaction.commit()
    }

    private val onSysCameraStateChangeCallback = OnCameraStateChangeCallback { isOpened, error ->
        imgTakePhoto.isEnabled = isOpened
        if (isOpened) {
            // 默认 fill 参数 false，不填充预览相机容器；可能存在左右留白或上下留白；如果不调用则可能存在画面拉伸
            PreviewResolutionUtils.changeSurfaceViewSize(this, framePreview, previewSize, false)
        } else {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            imageHelper.feedImageFrame(null)
        }
    }

    private val onV4L2CameraStateChangeListener = OnCameraStateChangeListener { isOpened, error ->
        onSysCameraStateChangeCallback.onCameraState(isOpened, error)
    }

    private val onSysCameraPreviewCallback = OnImagePreviewCallback { format, data, width, height, rotation, flip ->
        // 目前 format 只有 NV21，CameraX 支持 RGBA_8888 并未加入
        imageHelper.feedImageFrame(PreviewData(data, width, height, rotation, flip))
    }

    private val onV4L2CameraPreviewCallback = IFrameCallback { format, width, height, data ->
        if (format == CameraAPI.FRAME_FORMAT_MJPEG) {
            val nv21 = PreviewFormat.mjpegToNV21(data, width, height)
            onSysCameraPreviewCallback.onImagePreview(ImageFormat.NV21, nv21, width, height, 0, false)
        } else if (format == CameraAPI.FRAME_FORMAT_YUYV) {
            val nv21 = PreviewFormat.yuyvToNV21(data, width, height)
            onSysCameraPreviewCallback.onImagePreview(ImageFormat.NV21, nv21, width, height, 0, false)
        }
    }

    private fun takePhoto() {
        imageHelper.takePhoto()?.let {
            imgThumbnail.startThumbnailAnimation(it)
            imageHelper.saveImage(it)
        }
    }

}