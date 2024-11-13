### 支持数据类型
java的基本数据类型：int、long、boolean、float、double、char、byte

对象类型：String、CharSequence、ArrayList、HashMap

自定义类型：Parcelable

### 方向标记
kotlin数据类
```kotlin
data class Person(var name: String, var age: Int)
```
aidl数据类
```aidl
package xxx;
parcelable Person;
```
数据接口
```aidl
package xxx;
import xxx.Person;
interface IRemote {
    void sendPerson(in Person person);
    void sendPerson(out Person person);
    void sendPerson(inout Person person);
    oneway void sendPerson(in Person person);
}
```
in：不写默认此值，由cilent端流向server端，如client端发送Person("A", 20)，server修改后client不会受到影响

out：由server端流向client端，如client端发送Person("A", 20)，server得到的是Person(null, 0)，server修改后client同步修改

inout：对象可以双向流动

oneway: 异步

### 进程间通信
#### 不同应用
![aidl1](../../img/aidl/aidl1.png)

##### ipc模块
在aidl目录下package目录下创建两个aidl文件
```aidl
package xxx;
interface IRemoteCallback {
    void onMessageReceived(String message);
}

package xxx;
import xxx.IRemoteCallback;
interface IRemote {
    void sendMsg(String msg);
    void registerCallback(in IRemoteCallback callback);
    void unregisterCallback(in IRemoteCallback callback);
}
```
创建管理类，采用java代码是为了兼容性，由于此模块是提供出去的，若是kotlin，目标项目kotlin版本可能与当前模块不同导致编译出错
```java
public class IpcManager {
    public static final String TAG = "IpcManager";

    private IpcManager() {
    }

    public static IpcManager getInstance() {
        return Holder.singleton;
    }

    private static class Holder {
        private static final IpcManager singleton = new IpcManager();
    }

    private IRemote binder = null;
    private ILeLinkCallback callback = null;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = IRemote.Stub.asInterface(service);
            Log.d(TAG, "onServiceConnected binder=" + binder);
            if (callback != null) {
                try {
                    binder.registerCallback(callback);
                    Log.d(TAG, "onServiceConnected register callback");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "onServiceConnected error: " + e.getMessage());
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (callback != null) {
                try {
                    binder.unregisterCallback(callback);
                    Log.d(TAG, "onServiceDisconnected unregister callback");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "onServiceDisconnected error: " + e.getMessage());
                }
            }
            binder = null;
        }
    };

    public void init(Context context) {
        if (binder != null) {
            Log.e(TAG, "IRemote already init");
            return;
        }
        Intent intent = new Intent();
        intent.setAction("xxx.xxx.xxx");//Server端保持一致
        intent.setClassName("packageName",
            "serviceClassName");//Server端Service的信息
        context.getApplicationContext().bindService(intent, serviceConnection,
            Context.BIND_AUTO_CREATE);
    }

    public void sendMsg(String msg) {
        try {
            if (binder != null) {
                binder.sendMsg(msg);
                Log.d(TAG, "sendMsg msg:" + msg);
            } else {
                Log.e(TAG, "sendMsg() error: binder is null");
            }
        } catch (Exception e) {
            Log.d(TAG, "sendMsg() error: " + e.getMessage());
        }
    }

    public void subscribe(IRemoteCallback callback) {
        this.callback = callback;
        if (binder != null) {
            try {
                binder.registerCallback(callback);
                Log.d(TAG, "subscribe() called");
            } catch (Exception e) {
                Log.e(TAG, "subscribe() error: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "call init()");
        }
    }

    public void unSubscribe() {
        if (binder != null && callback != null) {
            try {
                binder.unregisterCallback(callback);
                callback = null;
                Log.d(TAG, "unSubscribe() called");
            } catch (Exception e) {
                Log.e(TAG, "unSubscribe() error: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "call init()");
        }
    }

    public void destroy(Context context) {
        if (binder == null) {
            Log.e(TAG, "IRemote not init");
            return;
        }
        context.getApplicationContext().unbindService(serviceConnection);
    }
}
```
##### server端
依赖上面ipc模块
```kts
api(project(":ipc"))
```
创建管理类
```kotlin
object AidlManager {
    private val TAG = "AidlManager"

    private val callbackList by lazy { RemoteCallbackList<IRemoteCallback>() }

    fun onMessageReceived(msg: String?) {
        val callbackCount = callbackList.beginBroadcast()
        for (i in 0 until callbackCount) {
            try {
                log(TAG, "aidl onMessageReceived() called $msg")
                callbackList.getBroadcastItem(i).onMessageReceived(msg)
            } catch (e: RemoteException) {
                log(TAG, "onReceive: ${e.message}")
            }
        }
        callbackList.finishBroadcast()
    }

    val binder: IRemote.Stub = object : IRemote.Stub() {
        override fun sendMsg(msg: String) {
            //处理收到client的消息
        }

        override fun registerCallback(callback: IRemoteCallback) {
            try {
                callbackList.register(callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun unregisterCallback(callback: IRemoteCallback) {
            try {
                callbackList.unregister(callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
```
创建服务
```kotlin
class AidlServerService : Service() {

    override fun onBind(intent: Intent): IBinder = AidlManager.binder

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
}
```
清单文件配置
```xml
<service android:name="xxx"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="service.aidlserver" />
    </intent-filter>
</service>
```
server端主动发消息给client
```kotlin
AidlManager.onMessageReceived("xxx")
```
##### client端
依赖上面ipc模块
```kts
api(project(":ipc"))
```
或者[依赖aar](../android_studio.md#kts_aar)

配置清单文件，manifest标签下添加应用感知能力
```xml
<queries>
    <package android:name="server包名" />
</queries>
```
使用
```kotlin
private val remoteCallback = object: IRemoteCallback(){
    override fun onMessageReceived(msg: String?) {
        //处理收到server端的消息
    }
}
IpcManager.getInstance().init(applicationContext)
IpcManager.getInstance().subscribe(remoteCallback)

//取消回调
IpcManager.getInstance().unSubscribe()
```
注：

1. registerCallback的意义在于server也可主动sendMsg给client(也可使用异步)，否则直接利用返回值即可
2. 获取server或者client标记可通过getCallingPid()确认
#### 同应用不同进程
和不同应用类似，不需要复制aidl(aar)和配置queries
### 其他
android studio不能创建aidl[无法新建AIDL文件](../android_studio.md#aidl-create)