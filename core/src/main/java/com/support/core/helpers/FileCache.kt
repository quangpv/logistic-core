package com.support.core.helpers

import android.content.Context
import android.graphics.Bitmap
import com.support.core.Inject
import java.io.File
import java.io.FileOutputStream

@Inject(true)
class FileCache(private val context: Context) {
    fun delete(signaturePath: String) {
        File(signaturePath).delete()
    }

    fun save(it: Bitmap, quality: Int = 80): String {
        val currentTime = System.currentTimeMillis()
        val path = "${context.cacheDir}/$currentTime.jpg"
        FileOutputStream(path).use { out ->
            it.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return path
    }

}