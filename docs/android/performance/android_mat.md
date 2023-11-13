### Memory Analyzer下载地址
https://eclipse.dev/mat/downloads.php

### hprof文件dump

方式一：通过android studio profiler-memory

方式二：通过代码
```kotlin
private fun dumpHprof() {
    Log.e(tag", "start dump ")
    val cacheDir = applicationContext.externalCacheDir
    val filePath = "${cacheDir?.absolutePath}/${System.currentTimeMillis()}_dump.hprof"
    val file = File(filePath)
    try {
        android.os.Debug.dumpHprofData(file.absolutePath)
        Log.e("tag", "end dump ")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```
### 标准格式转化
工具位置：AndroidSDK\platform-tools
```
hprof-conv -z fileName.hprof newFileName.hprof
```
使用拖拽脚本
```bat
@echo off
Setlocal enabledelayedexpansion
set "file=%~1"
set inputfilename=%~n1
md %inputfilename%
set hpOutFile=%inputfilename%\%inputfilename%_new.hprof
hprof-conv.exe %file% %hpOutFile%
echo success!
endlocal
```
Histogram

with incoming references：查看类被哪些实例引用

with outgoing references: 查看引用哪些外部实例

Shallow Heap: 浅堆

Retained Heap: 深堆，着重查看

左下角有黄点表示有泄漏

