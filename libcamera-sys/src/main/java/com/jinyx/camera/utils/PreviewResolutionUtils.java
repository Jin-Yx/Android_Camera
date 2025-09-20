package com.jinyx.camera.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;

public class PreviewResolutionUtils {

	public static void changeSurfaceViewSize(Context context, ViewGroup previewContainer, Size previewSize) {
		changeSurfaceViewSize(context, previewContainer, previewSize, false);
	}

	public static void changeSurfaceViewSize(Context context, ViewGroup previewContainer, Size previewSize, boolean fill) {
		previewContainer.postDelayed(new Runnable() {
			@Override
			public void run() {
				int containerWidth = previewContainer.getWidth();
				int containerHeight = previewContainer.getHeight();
				if (containerWidth > 0 && containerHeight > 0) {
					View previewView = previewContainer.getChildAt(0);
					chanceSurfaceViewSize(context, previewView, new Size(containerWidth, containerHeight), previewSize, fill);
				} else {
					Log.e("qtk", "change preview size error");
				}
			}
		}, 20);
	}

	private static void chanceSurfaceViewSize(Context context, View previewView, Size containerSize, Size previewSize, boolean fill) {
		ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
		if (layoutParams != null) {
			int containerWidth, containerHeight;
			double scaleContainer;
			if (containerSize != null) {
				containerWidth = containerSize.getWidth();
				containerHeight = containerSize.getHeight();
				scaleContainer = BigDecimalUtils.chu(containerWidth, containerHeight);
			} else {
				DisplayMetrics metrics = context.getResources().getDisplayMetrics();
				containerWidth = metrics.widthPixels;
				containerHeight = metrics.heightPixels;
				scaleContainer = BigDecimalUtils.chu(metrics.widthPixels, metrics.heightPixels);
			}
			double scalePreview = BigDecimalUtils.chu(previewSize.getWidth(), previewSize.getHeight());
			if (fill) {		// 画面填充，横向或纵向可能会有画面超出容器
				if (scaleContainer > scalePreview) {
					layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
					layoutParams.height = (int) (containerWidth / scalePreview);
				} else {
					layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
					layoutParams.width = (int) (scalePreview * containerHeight);
				}
			} else {	// 画面适应，宽高都在容器内，可能横向或纵向会有留白区域
				if (scaleContainer > scalePreview) {
					layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
					layoutParams.width = (int) (scalePreview * containerHeight);
				} else {
					layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
					layoutParams.height = (int) (containerWidth / scalePreview);
				}
			}
			previewView.setLayoutParams(layoutParams);
		}
	}

}
