package com.cariad.m2.netty.pack

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

data class CustomMessage(
    val name: String,
    val type: String,
    val content: String,
    val bitmap: Bitmap?
) {
    fun getBitmapBytes(): ByteArray? {
        return bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }

    companion object {
        fun bytesToBitmap(bytes: ByteArray): Bitmap? {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}