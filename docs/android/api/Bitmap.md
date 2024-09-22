### Bitmap保存File
Android10及以上使用MediaStore，不需要申请权限
删除图片
```kotlin
fun deleteImageFromGallery(context: Context, displayName: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Images.Media.DISPLAY_NAME + "=?"
        val selectionArgs = arrayOf(displayName)
        val deleted = resolver.delete(collection, selection, selectionArgs)
        if (deleted > 0) {
            //删除成功
        } else {
            //删除失败
        }
    }
```
保存图片到本地
```kotlin
fun saveBitmap(context: Context, displayName: String, bitmap: Bitmap) :Uri?{
    val values = ContentValues()
    values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    } else {
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val path = storageDir.absolutePath + File.separator + displayName
        values.put(MediaStore.Images.Media.DATA, path)
    }

    val resolver: ContentResolver = context.contentResolver
    val uri = try {
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    } catch (e: Exception) {
        null
    }
    if (uri != null) {
        resolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
        }
    }
    return uri
}
```

### Bitmap转HardwareBuffer
仅限系统使用
```java
BitmapFactory.Options options = new BitmapFactory.Options();
options.inPreferredConfig = Bitmap.Config.HARDWARE;

HardwareBuffer hardwareBuffer = BitmapFactory.decodeResource(mService.mContext.getResources(),
    com.android.internal.R.drawable.xxx, options).getHardwareBuffer();
```

### Bitmap之性能优化
见[Bitmap内存优化](../performance/android_bitmap.md)