### Bitmap
图片占用内存大小计算：分辨率x像素点大小

编码模式：

RGB_565：16位，2个字节(byte)

ARGB_8888：32位，4个字节(byte)

100*100的图片占用内存：100x100x2(4) = 20000(40000)byte

注意：占用大小和文件大小不相等

Android中加载Bitmap使用BitmapFactory.decodeResource从资源文件中加载时会进行一次转化

|密度|ldpi|mdpi|hdpi|xhdpi|xxhdpi|xxxhdpi|
|--|--|--|--|--|--|--|
|密度值|120|160(基线)|240|320|480|640|
|分辨率|240x320|320x480|480x800/480x854|1280x720|1920x1080|3840x2160|
|图片大小|36x36|48x48|72x72|96x96|144x144|192x192|

转化公式：分辨率x(设备dpi/目录dpi)

如设备dpi为480，xhdpi目录120x160的图片内存大小为

120x(480/320)x160(480/320) = 43200byte=42kb

https://developer.android.com/topic/performance/graphics/cache-bitmap?hl=zh-cn

https://developer.android.com/topic/performance/graphics?hl=zh-cn

### 高效加载大型位图
#### 读取位图尺寸和类型
```kotlin
val options = BitmapFactory.Options().apply {
    inJustDecodeBounds = true//可避免内存分配，为位图对象返回 null
}
BitmapFactory.decodeResource(resources, R.id.myimage, options)
val imageHeight: Int = options.outHeight
val imageWidth: Int = options.outWidth
val imageType: String = options.outMimeType
```
进行解码前先读取尺寸防止OOM

#### 按比例压缩
```kotlin
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
```
#### 整体流程
将任意大尺寸的位图加载到显示 100x100 像素缩略图的 ImageView 中
```kotlin
fun decodeSampledBitmapFromResource(
        res: Resources,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, this)

        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        inJustDecodeBounds = false

        BitmapFactory.decodeResource(res, resId, this)
    }
}

imageView.setImageBitmap(
    decodeSampledBitmapFromResource(resources, R.id.xxx, 100, 100)
)
```

### 缓存位图
#### 内存缓存
LruCache 类，不建议使用软引用或弱引用容易被回收

选择策略

1. 当前app内存占用情况(可能即将达到进程所能申请的最大内存)
2. 屏幕显示图片数量(列表中显示场景)
3. 屏幕尺寸和密度(分辨率和dpi影响计算内存大小)
4. 位图尺寸和配置(原图大小，编码模式，压缩)
5. 图片访问频率
6. 质量和数量之间的平衡

使用例子，分配最大内存的1/8
```kotlin
private lateinit var memoryCache: LruCache<String, Bitmap>

override fun onCreate(savedInstanceState: Bundle?) {
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

    val cacheSize = maxMemory / 8
    memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
}
```
#### 磁盘缓存
DiskLruCache

### 图片加载框架设计思路
- 采样率压缩(inSampleSize)，尺寸压缩
- bitmap编码格式ARGB_8888等
- 加载方式：协程用来异步加载和线程切换
- 多级缓存：LruCache、对象复用池、DiskLruCache
- 内存泄漏OOM：生命周期监听，低内存时释放
- 列表加载：错乱问题

[Glide]()已经完成优化