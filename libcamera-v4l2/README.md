> 常用打开 USB 相机的库，uvc 相机，不需要 /dev/video 节点权限，但是打开相机会使对应 /dev/video 节点掉线；如果依赖 /dev/video 节点与相机进行通信，则需要使用 v4l2 相机，通过 /dev/video 节点打开相机，需要 666 读写权限

&emsp;&emsp;v4l2 通过 `/dev/videoX` 节点来打开摄像头，因此应用需要对节点有读写权限：

~~~shell
adb shell
su
chmod 666 /dev/video*
~~~

&emsp;&emsp;部分 Android 系统可能给予 video 节点读写权限，对于应用依然不可读写，则需要关闭 selinux

~~~
adb shell
su
setenforce 0
getenforce  # 查看 selinux 状态; 输出 Permissive 表示关闭
~~~
- [x] UVC 相机: [UVCCamera](https://github.com/saki4510t/UVCCamera)、[AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera)
- [x] V4L2 相机: [AnV4L2Camera](https://github.com/yizhongliu/AnV4L2Camera)、本模块改自 [android_camera_v4l2](https://github.com/shengjunhu/android_camera_v4l2)，修改内容如下


### 1、[增加 nativeCreate2() 接口](./src/main/java/com/hsj/camera/CameraAPI.java)

~~~java
private native int nativeCreate(long nativeObj, int productId, int vendorId);

private native int nativeCreate2(long nativeObj, String videoNode);
~~~

&emsp;&emsp;原接口为 `int nativeCreate(long, int, int)` 传入 USB 设备的 vid 和 pid；
通过遍历 `/dev/video[0-99]` 节点，读取对应 `/sys/class/video4linux/video[X]/device/modalias` 配置匹配 vid 和 pid;

&emsp;&emsp;但是 modalias 配置文件一般需要关闭 selinux 之后才能读取；因此增加 `nativeCreate2(long, String)` 接口，直接传入需要打开的节点，如 `/dev/video0`


### 2、[修改 nativePreviewSize() 接口](./src/main/java/com/hsj/camera/CameraAPI.java)

&emsp;&emsp;修改获取图像预览分辨率接口，原先返回的分辨率为二维整型数组，但是并不知道对应分辨率是什么格式；因此参考 [AnV4L2Camera](https://github.com/yizhongliu/AnV4L2Camera) 修改接口，返回对应图像格式的分辨率信息与帧率；

~~~java
private native ArrayList<PreviewSize> nativePreviewSize(long nativeObj);

public class PreviewSize {
    private final int pixelFormat;
    private final int width;
    private final int height;
    private final List<FrameRate> frameRates;
}

public class FrameRate {
    private final int numerator;
    private final int denominator;
}
~~~

### 3、[修改 nativeFrameSize() 接口](./src/main/java/com/hsj/camera/CameraAPI.java)

&emsp;&emsp;设置预览图像分辨率；增加 `frameRate` 参数设置帧率，原先 cpp 中 MJPEG 格式默认帧率为 30，YUYV 默认帧率为 10；
设置帧率与实际帧率并不是一致，例如设置帧率为 30，但是因为设备性能、分辨率等原因，渲染和编解码后，实际帧率可能更低；

~~~java
private native int nativeFrameSize(long nativeObj, int width, int height, int pixelFormat, int frameRate);
~~~