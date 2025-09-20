package com.jinyx.camera.widget
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ThumbnailImageView(context: Context, attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {

    private companion object {
        private const val DURATION_SCALE = 200L
        private const val DURATION_ALPHA = 300L
    }

    fun startThumbnailAnimation(
        thumbnailImage: Bitmap,
        scaleDuration: Long = DURATION_SCALE,
        alphaDuration: Long = DURATION_ALPHA
    ) {
        setImageBitmap(thumbnailImage)
        visibility = VISIBLE
        animate().scaleX(0.8F).scaleY(0.8F).setDuration(scaleDuration).withEndAction {
            animate().alpha(0F).setDuration(alphaDuration).withEndAction {
                visibility = GONE
                animate().scaleX(1F).scaleY(1F).alpha(1F).setDuration(0)
            }
        }
    }

}