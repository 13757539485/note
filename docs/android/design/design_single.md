### <a id="lazy_instance">懒汉式(双重校验)</a>
```java
public class Singleton {
    private static Singleton INSTANCE;
    private Singleton() {
    }
    public static Singleton getInstance() {
        if (INSTANCE == null) {
            synchronized (Singleton.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Singleton();
                }
            }
        }
        return INSTANCE;
    }
}
```
### 静态内部类单例
```java
class Singleton {
    private Singleton() {

    }

    public static Singleton getInstance() {
        return Holder.singleton;
    }
    private static class Holder{
        private static Singleton singleton = new Singleton();
    }
}
```
### kotlin的单例
object关键字[单例](../kotlin/kotlin_class.md#single)
#### 懒汉式(双重校验)
```kotlin
class Singleton private constructor() {
    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            Singleton()
        }
    }
}
```
#### 静态内部类单例
```kotlin
class Singleton private constructor() {
    companion object {
        val instance = Holder.instance
    }

    private object Holder {
        val instance = Singleton()
    }
}
```