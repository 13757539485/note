在[启动SystemServer](./fws_system_server.md)时会启动AMS和ATMS服务
```java
startBootstrapServices(t);
```
通过辅助类SystemServiceManager来启动
```java
ActivityTaskManagerService atm = mSystemServiceManager.startService(
                ActivityTaskManagerService.Lifecycle.class).getService();
mActivityManagerService = ActivityManagerService.Lifecycle.startService(
        mSystemServiceManager, atm);
mActivityManagerService.setSystemServiceManager(mSystemServiceManager);
mActivityManagerService.setInstaller(installer);
mWindowManagerGlobalLock = atm.getGlobalLock();
```
其中ams和atms启动方式是相同的，ams保存了atms为成员变量

```java
frameworks/base/services/core/java/com/android/server/SystemServiceManager.java

Constructor<T> constructor = serviceClass.getConstructor(Context.class);
service = constructor.newInstance(mContext);
startService(service);
```
通过反射的方式调用构造方法创建Lifecycle实例
```java
mServices.add(service);
service.onStart();
```
将创建的实例对象保存到ArrayList<SystemService>中，Lifecycle继承于SystemService，构造方法中创建ActivityTaskManagerService实例，接着通过getService获得实例对象
```java
frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerService.java

public static final class Lifecycle extends SystemService {
    private final ActivityTaskManagerService mService;

    public Lifecycle(Context context) {
        super(context);
        mService = new ActivityTaskManagerService(context);
    }

    @Override
    public void onStart() {
        publishBinderService(Context.ACTIVITY_TASK_SERVICE, mService);
        mService.start();
    }

    public ActivityTaskManagerService getService() {
        return mService;
    }
}
```
new ActivityTaskManagerService时会创建mInternal的实例
```java
mInternal = new LocalService();
```
onStart方法主要用于保存Binder对象到ServicesManager中，mService.start()主要是把mInternal保存起来使用ArrayMap数据结构
```java
LocalServices.addService(ActivityTaskManagerInternal.class, mInternal);
```