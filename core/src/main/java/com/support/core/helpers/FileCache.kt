package com.support.core.helpers

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.io.File
import java.io.FileOutputStream

class FileCache(private val context: Context, private val folderName: String) {
    fun delete(signaturePath: String) {
        File(signaturePath).delete()
    }

    fun saveToCache(it: Bitmap, quality: Int = 80): String {
        val folder = onCreateCacheFolder(folderName).apply { mkdirs() }
        return doSave(it, quality, folder)
    }

    fun saveToGallery(it: Bitmap, quality: Int = 80): String {
        val folder = onCreateGalleryFolder(folderName).apply { mkdirs() }
        return doSave(it, quality, folder)
    }

    private fun doSave(it: Bitmap, quality: Int, folder: File): String {
        val currentTime = System.currentTimeMillis()
        val file = File(folder, "$currentTime.jpg")
        FileOutputStream(file).use { out ->
            it.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return file.path
    }

    private fun onCreateGalleryFolder(folderName: String): File {
        val folder = File("${context.cacheDir}/$folderName")
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_GRANTED) {
            return try {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
            } catch (e: Throwable) {
                context.getExternalFilesDir(folderName) ?: folder
            }
        }
        return folder
    }

    private fun onCreateCacheFolder(folderName: String): File {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_GRANTED) return File("${context.externalCacheDir}/$folderName")
        return File("${context.cacheDir}/$folderName")
    }

}