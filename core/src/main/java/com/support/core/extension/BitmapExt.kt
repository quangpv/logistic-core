package com.support.core.extension

import android.graphics.Bitmap
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