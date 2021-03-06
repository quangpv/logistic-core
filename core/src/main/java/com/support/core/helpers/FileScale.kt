package com.support.core.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.support.core.Inject
import java.io.FileNotFoundException


@Inject(true)
class FileScale(
        private val context: Context,
        private val fileCache: FileCache
) {
    companion object {
        const val MAX_WIDTH = 1280
        const val MAX_HEIGHT = 720
    }

    fun execute(uri: Uri, cacheInGallery: Boolean = false): String {
        val bitmap = getBitmapFrom(uri) ?: error("Can not decode ${uri.path}")
        val bmp = scale(bitmap)
        val newPath = if (cacheInGallery) fileCache.saveToGallery(bmp) else fileCache.saveToCache(bmp)
        bmp.recycle()
        bitmap.recycle()
        return newPath
    }

    private fun getBitmapFrom(uri: Uri): Bitmap? {

        val bmp = FileUtils.getPath(context, uri)?.let { BitmapFactory.decodeFile(it) }
        if (bmp != null) return bmp

        return try {
            val ims = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(ims)
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun execute(bitmap: Bitmap, recycle: Boolean = false, cacheInGallery: Boolean = false): String {
        val bmp = scale(bitmap)
        val newPath = if (cacheInGallery) fileCache.saveToGallery(bmp) else fileCache.saveToCache(bmp)
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