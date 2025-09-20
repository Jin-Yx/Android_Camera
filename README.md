Android 相机预览 —— Camera2、V4L2Camera
--

#### [apk 下载](./imgs/app-debug.apk) 或扫码安装

![](./imgs/Xnip2025-09-20_14-42-08.png)

- [系统相机](./libcamera-sys): 使用 Camera2 打开被系统识别的 前置、后置、外置相机

![](./imgs/Xnip2025-09-20_01-11-35.png)

- [V4L2 相机](./libcamera-v4l2): 使用 V4L2 Camera 通过 /dev/video 节点打开 USB 相机，需要赋予节点 `666` 读写权限

![](./imgs/Xnip2025-09-20_01-11-26.png)

<hr/>

![](./imgs/Xnip2025-09-20_01-11-12.png)

<hr/>

&emsp;&emsp;示例代码中增加拍照功能，系统相机返回 NV21 格式图像数据；V4L2 相机返回 MJPEG、YUYV 也同样提供接口转成 NV21；[nv21 转 bitmap 保存](./app/src/main/java/com/jinyx/camera/image/ImageHelper.kt)

&emsp;&emsp;如果需要保存视频数据，可考虑使用 [javacv](https://github.com/bytedeco/javacv) 进行图像帧录制，将 NV21 转 Frame
