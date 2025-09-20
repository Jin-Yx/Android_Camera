package com.jinyx.camera.listener

import android.view.View

interface OnItemClickListener<T> {

    fun onItemClick(v: View, position: Int, item: T)

}