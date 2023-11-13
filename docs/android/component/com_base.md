### 配置依赖

build.gradle中添加注解解释器和auto-service

[见as配置](../android_studio.md#kapt)

### 使用Auto-Service
```kotlin
object ServiceLoader {
    fun <T> load(service: Class<T>): T? {
        return try {
            ServiceLoader.load(service).iterator().next()
        } catch (e: Exception) {
            null
        }
    }
}
```
主要用来加载接口方法
```kotlin
interface XXX{
    fun xx()
}

@AutoService(XXX::class)
class XXXImpl: XXX {
}

ServiceLoader.load(XXX::class.java)
```
使用案例WebView封装