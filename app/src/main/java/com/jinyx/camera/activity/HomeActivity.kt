package com.jinyx.camera.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.jinyx.camera.R
import com.jinyx.camera.fragment.SysCameraResolutionFragment
import com.jinyx.camera.fragment.V4L2CameraResolutionFragment

class HomeActivity : AppCompatActivity() {

    private companion object {
        private const val REQUEST_CODE_CAMERA = 0x01
        private const val REQUEST_CODE_STORAGE = 0x02

        private const val INDEX_TAB_SYS = 0
        private const val INDEX_TAB_V4L2 = 1
    }

    private lateinit var txtCameraSys: TextView
    private lateinit var txtCameraV4L2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        txtCameraSys = findViewById(R.id.txtCameraSys)
        txtCameraV4L2 = findViewById(R.id.txtCameraV4L2)
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        requestPermission(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CODE_CAMERA) {
            onCameraPermissionGranted()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Environment.isExternalStorageManager()) {
                onStoragePermissionGranted()
            } else {
                startActivityForResult(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).also {
                    it.setData(Uri.parse("package:$packageName"))
                }, REQUEST_CODE_STORAGE)
            }
        } else {
            requestPermission(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), REQUEST_CODE_STORAGE) {
                onStoragePermissionGranted()
            }
        }
    }

    private fun requestPermission(permissions: Array<String>, requestCode: Int, permissionGrantedFunc: (() -> Unit)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, requestCode)
                    return
                }
            }
        }
        permissionGrantedFunc.invoke()
    }

    private fun onCameraPermissionGranted() {
        requestStoragePermission()
    }

    private fun onStoragePermissionGranted() {
        txtCameraSys.setOnClickListener { switchTab(INDEX_TAB_SYS) }
        txtCameraV4L2.setOnClickListener { switchTab(INDEX_TAB_V4L2) }
        switchTab(INDEX_TAB_SYS)
    }

    private fun switchTab(index: Int) {
        val fragment: Fragment? = when (index) {
            INDEX_TAB_SYS -> {
                txtCameraSys.setTextColor(ContextCompat.getColor(this, R.color.purple_500))
                txtCameraV4L2.setTextColor(ContextCompat.getColor(this, R.color.black))
                txtCameraSys.isEnabled = false
                txtCameraV4L2.isEnabled = true
                SysCameraResolutionFragment()
            }
            INDEX_TAB_V4L2 -> {
                txtCameraSys.setTextColor(ContextCompat.getColor(this, R.color.black))
                txtCameraV4L2.setTextColor(ContextCompat.getColor(this, R.color.purple_500))
                txtCameraSys.isEnabled = true
                txtCameraV4L2.isEnabled = false
                V4L2CameraResolutionFragment()
            }
            else -> null
        }
        fragment?.let {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.frameResolution, it)
            transaction.commit()
        }
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "没有相机权限", Toast.LENGTH_SHORT).show()
    }

    private fun onStoragePermissionDenied() {
        Toast.makeText(this, "没有存储权限", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isGranted = true
        for (grantResult in grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                isGranted = false
                break
            }
        }
        when (requestCode) {
            REQUEST_CODE_CAMERA -> if (isGranted) onCameraPermissionGranted() else onCameraPermissionDenied()
            REQUEST_CODE_STORAGE -> if (isGranted) onStoragePermissionGranted() else onStoragePermissionDenied()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Environment.isExternalStorageManager()) {
                onStoragePermissionGranted()
            } else {
                onStoragePermissionDenied()
            }
        }
    }

}