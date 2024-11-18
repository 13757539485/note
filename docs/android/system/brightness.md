### 封装亮度使用类
添加权限
```xml
<uses-permission
        android:name="android.permission.WRITE_SETTINGS"
        tools:ignore="ProtectedPermissions" />
```
源码
```kotlin
class BrightnessManager : DefaultLifecycleObserver {
    private val TAG = "BrightnessManager"
    private var lastScreenManualMode: Int = -1
    private lateinit var activity: AppCompatActivity
    private var isSystemSet: Boolean = true
    private var requestWriteSettings: ActivityResultLauncher<Intent>? = null
    private var brightnessLiveData = MutableLiveData<Int>()

    fun init(activity: AppCompatActivity) {
        this.activity = activity
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        registerContentObserver { brightness ->
            Log.d(TAG, "onCreate() called with: brightness = $brightness $isSystemSet")
            if (!isSystemSet) {
                isSystemSet = true
                return@registerContentObserver
            }
            leLinkSendMsg(JsonHelper.createBrightnessChangeAction(brightness))
        }
        requestWriteSettings =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (Settings.System.canWrite(activity)) {
                    brightnessLiveData.value?.let { setScreenBrightness(it) }
                }
                leLinkSendMsg(JsonHelper.createBrightnessChangeAction(getScreenBrightness()))
            }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        unregisterContentObserver()
        activity.lifecycle.removeObserver(this)
    }

    private fun goSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${activity.packageName}")
        )
        requestWriteSettings?.launch(intent)
    }

    /**
     * 设置系统屏幕亮度，影响所有页面和app
     * 注意：这种方式是需要手动权限的（android.permission.WRITE_SETTINGS）
     */
    fun setBrightnessForCheck(brightness: Int) {
        try {
            //先检测调节模式
//            setScreenManualMode(activity)
            //再设置
            brightnessLiveData.value = brightness
            setScreenBrightness(brightness)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun setScreenBrightness(brightness: Int) {
        if (Settings.System.canWrite(activity)) {
            /*val layoutParams = activity.window.attributes
            layoutParams.screenBrightness = brightness / 255.0f
            activity.window.attributes = layoutParams*/
            isSystemSet = false
            Settings.System.putInt(
                activity.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                if (brightness == 0) 1 else brightness
            )
            val percent = brightness * 100 / getBrightnessMax()
            FlowBus.with<Int>(BRIGHTNESS_CHANGE_UI).post(ActionJsonAnalysisHelper.mainScope, percent)
        } else {
            // 用户没有授予 WRITE_SETTINGS 权限
            // 执行相应的操作
            goSettings()
        }
    }

    fun getScreenBrightness(): Int {
        return try {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 监听系统亮度变化
     */
    private val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        var callback: ((brightness: Int) -> Unit)? = null
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            callback?.invoke(getScreenBrightness())
        }
    }

    /**
     * 注册监听 系统屏幕亮度变化
     */
    private fun registerContentObserver(callback: (brightness: Int) -> Unit) {
        activity.contentResolver?.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            true,
            brightnessObserver.also {
                it.callback = callback
            }
        )
    }

    private fun unregisterContentObserver() {
        brightnessObserver.callback = null
        activity.contentResolver.unregisterContentObserver(brightnessObserver)
    }

    private fun getOSBaseBrightness(): Int {
        return when (Build.BRAND) {
            "Xiaomi" -> 32
            else -> 16
        }
    }

    fun getBrightnessMax(): Int {
        val brightnessSettingMaximumId: Int = activity.getResources().getIdentifier(
            "config_screenBrightnessSettingMaximum",
            "integer",
            "android"
        )
        return activity.getResources().getInteger(brightnessSettingMaximumId)
            .let {
                if (it > 255) {
                    (it + 1) / getOSBaseBrightness()
                } else {
                    if (it == 0) 255 else it
                }
            }
    }

    fun getBrightnessMin(): Int {
        /*val brightnessSettingMinimumId: Int = activity.getResources().getIdentifier(
            "config_screenBrightnessSettingMinimum",
            "integer",
            "android"
        )
        return activity.getResources().getInteger(brightnessSettingMinimumId)*/
        return 1
    }

    /**
     * 设置系统亮度调节模式(SCREEN_BRIGHTNESS_MODE)
     * SCREEN_BRIGHTNESS_MODE_MANUAL 手动调节
     * SCREEN_BRIGHTNESS_MODE_AUTOMATIC 自动调节
     */
    private fun setScreenManualMode(activity: Activity) {
        try {
            //获取当前系统亮度调节模式
            lastScreenManualMode = Settings.System.getInt(
                activity.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
            //如果是自动，则改为手动
            if (lastScreenManualMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(
                    activity.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
            }
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
    }
}
```
### Activity中使用
初始化
```kotlin
private val brightnessManager = BrightnessManager().also { it.init(this) }
```
设置亮度
```kotlin
brightnessManager.setBrightnessForCheck(brightness)
```