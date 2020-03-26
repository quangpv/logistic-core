package com.support.core.helpers

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.support.core.Inject
import java.io.IOException


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
        val bitmap = getBitmapFrom(uri) ?: error("Can not decode ${uri.path}")
        val bmp = scale(bitmap)
        val newPath = fileCache.save(bmp)
        bmp.recycle()
        bitmap.recycle()
        return newPath
    }

    private fun getBitmapFrom(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT < 29) {
            val path = FileUtils.getPath(context, uri)!!
            return BitmapFactory.decodeFile(path)
        }

        val projection = arrayOf(
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.DISPLAY_NAME
        )
        var bitmap: Bitmap? = null

        context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0))
            try {
                context.contentResolver.openFileDescriptor(imageUri, "r").use { pfd ->
                    bitmap = pfd?.let { BitmapFactory.decodeFileDescriptor(it.fileDescriptor) }
                }
            } catch (ex: IOException) {
            }
        }

        return bitmap
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