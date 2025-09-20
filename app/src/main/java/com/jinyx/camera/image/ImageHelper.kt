package com.jinyx.camera.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Environment
import com.jinyx.camera.bean.PreviewData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

class ImageHelper {

    private var previewData: PreviewData? = null

    fun feedImageFrame(previewData: PreviewData?) {
        this.previewData = previewData
    }

    fun takePhoto(): Bitmap? {
        val preview = previewData ?: return null

        val yuvImage = YuvImage(preview.nv21, ImageFormat.NV21, preview.width, preview.height, null)
        val baos = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, preview.width, preview.height), 100, baos)
        val jpegData = baos.toByteArray()
        var bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size) ?: return null
        if (preview.rotation != 0 || preview.flip) {
            val matrix = Matrix()
            if (preview.rotation != 0) {
                matrix.postRotate(preview.rotation.toFloat())
            }
            if (preview.flip) {
                matrix.postScale(-1f, 1f)
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        }
        return bitmap
    }

    @SuppressLint("SimpleDateFormat")
    fun saveImage(bitmap: Bitmap): File? {
        val name = "${SimpleDateFormat("yyyyMMddHHmmssSSS").format(Date())}.jpg"
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val picture = File(dir, name)
        var fos: OutputStream? = null
        try {
            fos = FileOutputStream(picture)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            return picture
        } catch (ignore: IOException) {
        } finally {
            fos?.let {
                try {
                    it.close()
                } catch (ignore: IOException) { }
            }
        }
        return null
    }

}