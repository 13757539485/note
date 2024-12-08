package com.cariad.m2.sharebar.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import com.cariad.m2.ICariadDragListener
import com.cariad.m2.sharebar.core.ShareFileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
@RequiresApi(Build.VERSION_CODES.FROYO)
object MediaUtil {
    private val TAG = "MediaUtil"

    private fun getDownloadDir(): File {
        val publicDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        if (!publicDir.exists()) {
            publicDir.mkdirs()
        }
        return publicDir
    }

    private fun getDownloadCacheDir(): File {
        val dragDir = File(getDownloadDir(), "DragDrop")
        if (!dragDir.exists()) {
            dragDir.mkdirs()
        }
        return dragDir
    }

    private fun getCacheDir(context: Context): File {
        val dragDir = File(context.externalCacheDir, "DragDrop")
        if (!dragDir.exists()) {
            dragDir.mkdirs()
        }
        return dragDir
    }

    private fun getCacheFile(context: Context): File {
        val outputFile =
            File(getDownloadCacheDir(), "drag_drop_bitmap.jpeg")
        if (outputFile.exists()) {
            val file = outputFile.delete()
            Log.e(TAG, "delete file: $file")
        }
        return outputFile
    }

    fun saveBitmap(bitmap: Bitmap, context: Context, listener: ICariadDragListener?) {
        val outputFile = getCacheFile(context)
        val os: OutputStream = FileOutputStream(outputFile, false)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
        os.flush()
        os.close()
        MediaScannerConnection.scanFile(context, arrayOf(outputFile.path), arrayOf("image/jpeg")
        ) { _, uri -> listener?.callBack(uri) }
        /*listener?.callBack(
            ShareFileProvider.getUriForFile(
                context,
                "com.cariad.m2.share",
                outputFile
            )
        )*/
    }
}