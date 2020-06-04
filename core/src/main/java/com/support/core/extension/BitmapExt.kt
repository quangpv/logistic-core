package com.support.core.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream


fun Bitmap.toBase64(recycle: Boolean): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
    val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
    if (recycle) recycle()
//    return "data:image/jpeg;base64,${Base64.encodeToString(byteArray, Base64.DEFAULT)}"
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}


fun Bitmap.toBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun bitmapFrom(byteArray: ByteArray): Bitmap? {
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}