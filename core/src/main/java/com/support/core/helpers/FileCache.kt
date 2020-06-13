package com.support.core.helpers

import android.content.Context
import android.graphics.Bitmap
import com.support.core.Inject
import java.io.File
import java.io.FileOutputStream

@Inject(true)
class FileCache(private val context: Context, private val folderName: String = "photos") {
    fun delete(signaturePath: String) {
        File(signaturePath).delete()
    }

    fun save(it: Bitmap, quality: Int = 80): String {
        val currentTime = System.currentTimeMillis()

        val folder = File("${context.cacheDir}/$folderName")
        folder.mkdirs()
        val file = File(folder, "$currentTime.jpg")
        FileOutputStream(file).use { out ->
            it.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return file.path
    }

}