### 内存溢出(OOM)
分类：Java堆内存溢出(常见)、无足够连续内存空间、FD数量超出限制、线程数超出限制、虚拟内存不足

#### 常见内存溢出
- 内存抖动
- 内存泄漏
- 文件数达上限
- 线程数达上限
- 内存不足

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