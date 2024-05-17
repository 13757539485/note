### 前台服务
#### 声明权限和注册
其中电量优化用于保活相关，WAKE_LOCK用来保证传输数据时保证cpu活跃也用于保持常亮
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.NOTIFICATION_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<service
    android:name=".bridge.BridgeService"
    android:enabled="true"
    android:exported="true"
    android:foregroundServiceType="dataSync" />

```
#### 启动服务
```kotlin
private fun startService() {
    val intent = Intent(ProxyApplication.application, BridgeService::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ProxyApplication.application.startForegroundService(intent)
    } else {
        ProxyApplication.application.startService(intent)
    }
}
```
#### 创建通知
前台服务必须创建通知
```kotlin
private val BRIDGE_CHANNEL_ID = "channel_xx"
private val BRIDGE_CHANNEL_NAME = "BridgeService"
private val BRIDGE_NOTIFICATION_ID = 1

private val notificationManager by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

 override fun onCreate() {
    super.onCreate()
    createNotificationForForegroundService()
}

override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.i(TAG, "onStartCommand: $intent")
    intent?.let {

    }
    return START_STICKY
}

@SuppressLint("RemoteViewLayout")
fun createNotificationForForegroundService() {
    // 适配8.0及以上
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        notificationManager.getNotificationChannel(BRIDGE_CHANNEL_ID) == null
    ) {
        val channel = NotificationChannel(
            BRIDGE_CHANNEL_ID,
            BRIDGE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    // 适配12.0及以上
    val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    // 添加自定义通知view
    val views = RemoteViews(packageName, R.layout.notification_bridge)

    // 创建Builder
    val builder: NotificationCompat.Builder = NotificationCompat.Builder(
        this, BRIDGE_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setAutoCancel(true)
        .setCustomContentView(views)
        .setPriority(1000)

    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    } else {
        0
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(BRIDGE_NOTIFICATION_ID, builder.build(), type)
    }
}
```