### 开机自启广播
#### 声明权限和注册
```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver
    android:name=".bridge.BridgeBroadcastReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```
#### 回调监听
开启自启后可以启动服务
```kotlin
class BridgeBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            when (it.action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    ServiceManager.bindService()
                }
            }
        }
    }
}
```