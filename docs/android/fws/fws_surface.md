## Android之图形系统
### View
### ViewRootImpl
### Window
### Display
### SurfaceView
### TextureView
#### 显示一张图片
```kotlin
binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        drawBitmapToTextureView(surfaceTexture, width, height)
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {}

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
}

private fun drawBitmapToTextureView(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
    // 创建一个 Surface 对象
    val surface = Surface(surfaceTexture)

    val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.test)

    // 获取 Canvas
    var canvas: Canvas? = null
    val matrix = Matrix()
    try {
        canvas = surface.lockCanvas(null)
        if (canvas != null) {
            // 清空 Canvas
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            // 计算变换矩阵以适应 TextureView 的大小
            val srcRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL)

            // 应用变换矩阵并绘制图片
            canvas.drawBitmap(bitmap, matrix, null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        if (canvas != null) {
            // 解锁 Canvas 并提交绘制
            surface.unlockCanvasAndPost(canvas)
        }
    }

    // 释放 Surface
    surface.release()
}
```
其中铺满Textureview还可以通过scale形式
```kotlin
// 计算缩放比例以适应 TextureView 的大小
val scaleX = width.toFloat() / bitmap.width
val scaleY = height.toFloat() / bitmap.height
val scale = scaleX.coerceAtLeast(scaleY) // 使用最大缩放比例以确保图片占满 TextureView

// 设置变换矩阵
matrix.setScale(scale, scale)
```
#### 截图
获取bitmap
```kotlin
val bitmap =
    binding.textureView.getBitmap(binding.textureView.width, binding.textureView.height)
```
截取部分区域
```kotlin
bitmap?.let {
    val regionBitmap = Bitmap.createBitmap(
        bitmap, 0, 0,
        100,
        50)
    )
}
```
### Canvas
Android中的画布
### Surface
绘制图形的载体，显示内容如View，bitmap等，真正的画布，会关联一个Canvas

每个Suface都会在SurfaceFlinger中有对应的Layer图层

### SurfaceFlinger
负责把Layer按需混合处理后输出到Frame Buffer中，再由Display设备（屏幕或显示器）把Frame Buffer里的数据呈现到屏幕上
```shell
adb shell dumpsys SurfaceFlinger
```
### SurfaceControl
控制Surface的属性和层次结构，一个SurfaceControl关联一个Surface，底层会创建Surface一般不需要开发者创建和销毁
#### API介绍

### SurfaceControl.Transaction
用来操作SurfaceControl，比如透明度、大小、位置、圆角等属性，调用apply提交才会生效
#### API介绍

<a id="setBackgroundBlurRadius">setBackgroundBlurRadius</a>

设置图层高斯模糊，硬件要求高(可能会导致设备卡顿，android12以上默认开启)

开启方式

源码编译方式：

device/品牌/型号/xxx.mk中添加
```mk
PRODUCT_PROPERTY_OVERRIDES += \
    ro.surface_flinger.supports_background_blur=1
```
root方式：vendor/build.prop文件中添加即可

### 通过View创建Surface图层
```java
final SurfaceSession session = new SurfaceSession();
final SurfaceControl surfaceControl = new SurfaceControl.Builder(session)
        .setName("drag surface")
        .setParent(root.getSurfaceControl())
        .setBufferSize(view.getWidth(), view.getHeight())
        .setFormat(PixelFormat.TRANSLUCENT)
        .setCallsite("View.startDragAndDrop")
        .build();
final Surface surface = new Surface();
surface.copyFrom(surfaceControl);
final Canvas canvas = isHardwareAccelerated()
        ? surface.lockHardwareCanvas()
        : surface.lockCanvas(null);
try {
    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
    view.draw(canvas);
} finally {
    surface.unlockCanvasAndPost(canvas);
}

// 一般系统中已有Transaction如wms中mService.mTransactionFactory.get()
SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
transaction.show(surfaceControl);
// 重新设置新的父级SurfaceControl
transaction.reparent(surfaceControl, getDisplayContent().getOverlayLayer())
transaction.apply();

// surface需要销毁
surface.destory();
session.kill();
```

### Surface添加图层
```java
private SurfaceControl mAddLeash;

// 在当前mSurfaceControl上添加+图标
private void makeAddLeash() {
    try (SurfaceControl.Transaction transaction =
                    mService.mTransactionFactory.get()) {
        if (mAddLeash != null) {
            transaction.show(mAddLeash);
            transaction.apply();
            return;
        }
        mAddLeash = new SurfaceControl.Builder()
                .setName("drag add layer")
                .setFormat(PixelFormat.TRANSLUCENT)
                .setParent(mSurfaceControl)
                .setCallsite("DragState.createShowAnimationLocked")
                .setBLASTLayer()
                .build();
        //bitmap转成HardwareBuffer需要配置为HARDWARE格式
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.HARDWARE;

        HardwareBuffer hardwareBuffer = BitmapFactory.decodeResource(mService.mContext.getResources(),
                com.android.internal.R.drawable.ic_input_add, options).getHardwareBuffer();
        // android13推荐直接传HardwareBuffer
        transaction.setBuffer(mAddLeash, GraphicBuffer.createFromHardwareBuffer(hardwareBuffer));
        transaction.setColorSpace(mAddLeash, ColorSpace.get(ColorSpace.Named.SRGB));
        transaction.show(mAddLeash);
        transaction.setPosition(mAddLeash,
                mSurfaceControl.getWidth() - hardwareBuffer.getWidth(), 0);
        //生效必须调用apply
        transaction.apply();
    }
}

// 控制+图标显示和隐藏
private void visibleAddLeash(boolean isShow){
    try (SurfaceControl.Transaction transaction =
                    mService.mTransactionFactory.get()) {
        if (mAddLeash != null) {
            if (isShow) {
                transaction.show(mAddLeash);
            } else {
                transaction.hide(mAddLeash);
            }
            transaction.apply();
        }
    }
}
```