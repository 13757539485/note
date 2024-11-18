适用于应用内通信
```
object FlowBus {
    private const val TAG = "FlowBus"
    private val busMap = mutableMapOf<String, EventBus<*>>()
    private val busStickMap = mutableMapOf<String, StickEventBus<*>>()

    @Synchronized
    fun <T> with(key: String): EventBus<T> {
        var eventBus = busMap[key]
        if (eventBus == null) {
            eventBus = EventBus<T>(key)
            busMap[key] = eventBus
        }
        return eventBus as EventBus<T>
    }

    @Synchronized
    fun <T> withStick(key: String): StickEventBus<T> {
        var eventBus = busStickMap[key]
        if (eventBus == null) {
            eventBus = StickEventBus<T>(key)
            busStickMap[key] = eventBus
        }
        return eventBus as StickEventBus<T>
    }

    private fun removeEventBus(key: String, isSticky: Boolean) {
        if (isSticky) {
            busStickMap.remove(key)
        } else {
            busMap.remove(key)
        }
    }

    //真正实现类
    open class EventBus<T>(private val key: String) : DefaultLifecycleObserver {

        //私有对象用于发送消息
        private val _events: MutableSharedFlow<T> by lazy {
            obtainEvent()
        }

        //暴露的公有对象用于接收消息
        val events = _events.asSharedFlow()

        open fun obtainEvent(): MutableSharedFlow<T> =
            MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)

        //主线程接收数据
        fun register(lifecycleOwner: LifecycleOwner, action: (t: T) -> Unit) {
            lifecycleOwner.lifecycle.addObserver(this)
            lifecycleOwner.lifecycleScope.launch {
                events.collect {
                    try {
                        action(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, "FlowBus - Error:$e")
                    }
                }
            }
        }

        //协程中发送数据
        suspend fun post(event: T) {
            _events.emit(event)
        }

        //主线程发送数据
        fun post(scope: CoroutineScope, event: T) {
            scope.launch {
                _events.emit(event)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Log.w(TAG, "FlowBus - 自动onDestroy")
            val subscriptCount = _events.subscriptionCount.value
            if (subscriptCount <= 0)
                removeEventBus(key, this is StickEventBus)
        }
    }

    class StickEventBus<T>(key: String) : EventBus<T>(key) {
        override fun obtainEvent(): MutableSharedFlow<T> =
            MutableSharedFlow(1, 1, BufferOverflow.DROP_OLDEST)
    }
}
```

### 基本使用
kotlin中发送消息
```kotlin
FlowBus.with<String>("xxx").post(mainScope, "aaa")
```
xxx作为唯一标志

String类型对应aaa

mainScope为协程环境

接收消息(一般在Activity或者Fragment中使用)

```kotlin
FlowBus.with<String>("xxx").register(this) { content->
    //处理消息
}
```
this为LifecycleOwner
java中
```java
FlowBus.INSTANCE.with("xxx").register(this, o -> {
    int x = (Integer) o;
    return null;
});
```

[View中接收使用](../ui/ui_view.md#view_lifecycle)