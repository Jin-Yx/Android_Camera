package com.hsj.camera.utils;

import com.hsj.camera.CameraAPI;
import com.hsj.camera.bean.PreviewSize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraFinder {

	/**
	 * 通过 vid, pid 查找 video 节点，一般 Android 平台需要关闭 selinux 权限
	 * adb shell
	 * su
	 * setenforce 0
	 * cat /sys/class/video4linux/video?/device/modalias
	 * 输出如下，其中 usb: 开头，之后是 v{vendorId}、p{productId}
	 * usb:v1D6Bp0102d0409dcEFdsc02dp01ic0Eisc01ip00in02
	 */
	public static List<File> findVideoNode(int vendorId, int productId) {
		List<File> videoNodeList = new ArrayList<>();
		File dir = new File("/sys/class/video4linux");
		File[] videoNodes = dir.listFiles();
		if (videoNodes != null) {
			for (File videoNode : videoNodes) {
				File modalias = new File(videoNode, "device/modalias");
				if (modalias.exists()) {
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new FileReader(modalias));
						String usbInfo = reader.readLine();
						if (usbInfo != null && usbInfo.startsWith("usb:") && usbInfo.charAt(9) == 'p') {
							String vId = usbInfo.substring(5, 9);
							String pId = usbInfo.substring(10, 14);
							if (String.format("%04x", vendorId).equalsIgnoreCase(vId) && String.format("%04x", productId).equalsIgnoreCase(pId)) {
								videoNodeList.add(new File("/dev/" + videoNode.getName()));
							}
						}
					} catch (IOException ignore) {
						return null;
					} finally {
						if (reader != null) {
							try {
								reader.close();
							} catch (IOException ignore) {}
						}
					}
				}
			}
		}
		return videoNodeList;
	}

	public static List<PreviewSize> getPreviewSize(File videoNode) {
		if (videoNode == null || !videoNode.exists()) {
			return null;
		} else if (!videoNode.canRead() || !videoNode.canWrite()) {
			return null;
		}
		CameraAPI cameraAPI = new CameraAPI();
		cameraAPI.create(videoNode.getAbsolutePath());
		List<PreviewSize> previewSizeList = cameraAPI.getPreviewSize();
		cameraAPI.destroy();
		return previewSizeList;
	}

}
