### Bitmap
图片占用内存大小计算：分辨率x像素点大小

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

### 内存抖动
短时间内存反复发生增长和回收，可能会导致卡顿和oom

卡顿：gc回收中会有stw使得工作线程暂停

oom：如果垃圾回收采用的时标记-清除法会出现大量内存碎片，此时创建一个比较大的对象(如数组)时由于没有连续内存空间导致oom

### 内存泄漏
当对象不能被回收导致内存越来越少，最终可能会oom

[java中使用的回收算法](./java/java_jvm.md#obj_live)

常见泄漏：单例context(applicationContext或者手动将单例置null)、handler(弱引用+销毁removeMessage操作)、webview(通过动态创建+addView/removeView的形式或独立进程退出调用System.exit)、非静态类有静态实例引用(改成静态引用)

### 分配大内存
AndroidManifest中android:largeHeap="true"，获取当前配置内存大小和最大内存大小
```kotlin
getSystemService(Context.ACTIVITY_SERVICE).let {it as ActivityManager
    val info = ActivityManager.MemoryInfo()// 获取系统内存
    it.getMemoryInfo(info)
    Log.e("tag", "memory: ${it.memoryClass}, large memory： ${it.largeMemoryClass}, info: ${info.availMem} ${info.totalMem}")
}
```

### 监听释放内存响应
根据内存level处理
```kotlin
class MainActivity : ComponentActivity(), ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }
```