package com.support.core.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.support.core.Inject

@Inject(true)
class FileScale(
    private val context: Context,
    private val fileCache: FileCache
) {
    companion object {
        const val MAX_WIDTH = 1280
        const val MAX_HEIGHT = 720
    }

    fun execute(uri: Uri): String {
        val path = FileUtils.getPath(context, uri)!!
        val bitmap = BitmapFactory.decodeFile(path)
        val bmp = scale(bitmap)
        val newPath = fileCache.save(bmp)
        bmp.recycle()
        bitmap.recycle()
        return newPath
    }

    fun execute(bitmap: Bitmap, recycle: Boolean = false): String {
        val bmp = scale(bitmap)
        val newPath = fileCache.save(bmp)
        bmp.recycle()
        if (recycle) bitmap.recycle()
        return newPath
    }

    fun scale(bitmap: Bitmap): Bitmap {
        val size = bitmap.getExpectSize()
        return Bitmap.createScaledBitmap(bitmap, size.width(), size.height(), false)
    }

    private fun Bitmap.getExpectSize(): Rect {
        var bmpWidth: Int = width
        var bmpHeight: Int = height

        var maxWidth = MAX_WIDTH
        var maxHeight = MAX_HEIGHT

        val ratio = width.toFloat() / height
        if (ratio < 1) {
            maxWidth = MAX_HEIGHT
            maxHeight = MAX_WIDTH
        }
        if (width > maxWidth) {
            bmpWidth = maxWidth
            bmpHeight = (bmpWidth / ratio).toInt()
        } else if (height > maxHeight) {
            bmpHeight = maxHeight
            bmpWidth = (bmpHeight * ratio).toInt()
        }
        return Rect(0, 0, bmpWidth, bmpHeight)
    }

}