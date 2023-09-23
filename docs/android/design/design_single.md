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