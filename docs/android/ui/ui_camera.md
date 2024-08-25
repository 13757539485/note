## CameraX
### 基本配置
添加权限
```xml
<uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```
添加依赖
```gradle
def cameraxVersion = "1.3.0"
implementation("androidx.camera:camera-core:${cameraxVersion}")
implementation("androidx.camera:camera-camera2:${cameraxVersion}")
implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
implementation("androidx.camera:camera-video:${cameraxVersion}")
implementation("androidx.camera:camera-view:${cameraxVersion}")
implementation("androidx.camera:camera-extensions:${cameraxVersion}")
```
添加预览view
```xml
<androidx.camera.view.PreviewView
    android:id="@+id/cameraPreView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
### 权限申请
```kotlin
companion object {
    private const val TAG: String = "CameraXActivity"
    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).toTypedArray()
}
```
点击预览时申请权限
```kotlin
getBinding().btnCamera.click {
    if (checkPermission())
        startCamera()
    else ActivityCompat.requestPermissions(
        this,
        REQUIRED_PERMISSIONS,
        REQUEST_CODE_PERMISSIONS
    )
}

private fun checkPermission() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(
        baseContext,
        it
    ) == PackageManager.PERMISSION_GRANTED
}

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
        if (checkPermission())
            startCamera()
    }
}
```

### 开启预览
```kotlin
private val imageCapture: ImageCapture by lazy {
    ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setTargetResolution(Size(1200, 2652)) // 方法过时但不设置最终照片和预览大小不一致
        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
        .build()
}
private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
private var cameraProvider: ProcessCameraProvider? = null
private fun startCamera() {
    // 获取相机单例
    ProcessCameraProvider.getInstance(this).apply {
        // 添加监听
        addListener(
            {
                cameraProvider = get() // 获取相机
                // 指定预览的相机
                bindPreview(cameraProvider!!, imageCapture, cameraSelector)
            }, ContextCompat.getMainExecutor(this@CameraXActivity)
        )
    }
}
```

### 控制闪光灯
```kotlin
private var flashCount: Int = 0
private fun changeFlash() {
    val imageCapture = imageCapture ?: return
    if (!turnCamera) return
    flashCount++
    val flashMode = when (flashCount % 3) {
        0 -> {
            getBinding().btnFlash.text = "闪光灯(关闭)"
            ImageCapture.FLASH_MODE_OFF
        }
        1 -> {
            getBinding().btnFlash.text = "闪光灯(打开)"
            ImageCapture.FLASH_MODE_ON
        }
        else -> {
            getBinding().btnFlash.text = "闪光灯(自动)"
            ImageCapture.FLASH_MODE_AUTO
        }
    }
    imageCapture.flashMode = flashMode
}
```

### 切换摄像头
```kotlin
private fun turnCamera() {
    val cameraProvider = cameraProvider ?: return
    cameraSelector = if (turnCamera) {
        getBinding().btnTurnCamera.text = "摄像头(前置)"
        turnCamera = false
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        getBinding().btnTurnCamera.text = "摄像头(后置)"
        turnCamera = true
        CameraSelector.DEFAULT_BACK_CAMERA
    }
    bindPreview(cameraProvider, imageCapture, cameraSelector)
}

private fun bindPreview(
    cameraProvider: ProcessCameraProvider,
    imageCapture: ImageCapture,
    cameraSelector: CameraSelector
) {
    try {
        cameraProvider.unbindAll() // 绑定之前先解绑
        // 创建并设置相机预览窗口
        val preview = Preview.Builder().build().also {
            // 绑定布局中的Preview进行预览
            it.setSurfaceProvider(getBinding().cameraPreView.surfaceProvider)
        }
        // 多个摄像头同时使用
//            val viewPort = ViewPort.Builder(Rational(1080, 1920), display!!.rotation).build()
//            val useCaseGroup = UseCaseGroup.Builder()
//                .addUseCase(preview)
//                .addUseCase(imageCapture)
//                .setViewPort(viewPort)
//                .build()
//            val selectors = cameraSelector.map {
//                ConcurrentCamera.SingleCameraConfig(it, useCaseGroup, this)
//            }
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
    } catch (e: Exception) {
        Log.e("tag", "startCamera: ${e.message}")
    }
}
```

### 拍照并保存到相册
```kotlin
private fun takePhoto() {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .setMetadata(ImageCapture.Metadata().also {
            it.isReversedHorizontal = !turnCamera
        })
        .build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }

            override fun
                    onImageSaved(output: ImageCapture.OutputFileResults) {
                lifecycleScope.launch {
                    it.transformRotation(contentResolver, !turnCamera)
                }
            }
        }
    )
}
```
由于前置摄像头拍摄的照片会左右相反，需要手动转换后再重新保存
```kotlin
suspend fun Uri.transformRotation(
    contentResolver: ContentResolver,
    isReversedHorizontal: Boolean
) {
    if (!isReversedHorizontal) return
    withContext(Dispatchers.IO) {
        contentResolver.openInputStream(this@transformRotation)?.use { input ->
            val originalBitmap = BitmapFactory.decodeStream(input)
            val preMatrix = Matrix().apply { preScale(-1f, 1f) }
            val flippedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalBitmap.width,
                originalBitmap.height, preMatrix, true
            )
            contentResolver.openOutputStream(this@transformRotation)?.let { output ->
                output.use { useOutput ->
                    flippedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, useOutput)
                }
            }
        }
    }
}
```
### 获取照片真实路径
```kotlin
suspend fun Uri.getFilePath(context: Context): String? {
    return withContext(Dispatchers.IO) {
        var filePath: String? = null
        val cursor = context.contentResolver.query(this@getFilePath, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(MediaStore.Images.Media.DATA)
                filePath = it.getString(index)
            }
        }
        if (filePath.isNullOrEmpty()) {
            filePath = if (DocumentsContract.isDocumentUri(context, this@getFilePath)) {
                // 处理 Document 类型的 URI
                val docId = DocumentsContract.getDocumentId(this@getFilePath)
                val split = docId.split(":").toTypedArray()
                if (split.size > 1 && "primary".equals(split[0], ignoreCase = true)) {
                    "${context.getExternalFilesDir(null)}/${split[1]}"
                } else {
                    null
                }
            } else if ("content".equals(this@getFilePath.scheme, ignoreCase = true)) {
                // 处理 Content 类型的 URI
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                context.contentResolver.query(this@getFilePath, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        cursor.getString(columnIndex)
                    } else {
                        null
                    }
                }
            } else if ("file".equals(this@getFilePath.scheme, ignoreCase = true)) {
                // 处理 File 类型的 URI
                this@getFilePath.path
            } else {
                null
            }
        }
        filePath
    }
}
```
调用
```kotlin
lifecycleScope.launch {
    it.transformRotation(contentResolver, !turnCamera)
    val path = it.getFilePath(this@CameraXActivity)
    Toast.makeText(
        this@CameraXActivity,
        "保存完毕:$path",
        Toast.LENGTH_LONG
    ).show()
}
```
### 图片相关函数
获取图片旋转度数
```kotlin
fun Uri.getPictureRotation(contentResolver: ContentResolver): Int {
    return contentResolver.openInputStream(this)?.use { input ->
        val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ExifInterface(input)
        } else {
            ExifInterface(this.path!!)
        }
        val rotationFlag = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        when (rotationFlag) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } ?: 0
}
```