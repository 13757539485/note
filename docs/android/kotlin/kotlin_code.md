## Android应用案例

### lateinit应用
```kotlin
class MainActivity2 : AppCompatActivity() {
    private lateinit var fragment: Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.findFragmentById(R.id.xxx)?.let { 
                fragment = it
            }
        }
        if (!::fragment.isInitialized) {
            fragment = MyFragment()
        }
        setContentView(R.layout.xxx)
        showFragment(supportFragmentManager, fragment)
    }
}
```

### Log封装

```kotlin
const val TAG = "XXX"
const val TAG_ON_OFF = "XXX_"//有开关的tag

fun log(flag: String, msg: String, level: Int = Log.DEBUG, tag: String = TAG) {
    when (level) {
        Log.INFO -> Log.i(tag, "$flag-->$msg")
        Log.VERBOSE -> Log.v(tag, "$flag-->$msg")
        Log.DEBUG -> Log.d(tag, "$flag-->$msg")
        Log.WARN -> Log.d(tag, "$flag-->$msg")
        Log.ERROR -> Log.e(tag, "$flag-->$msg")
    }
}

/**
 * 使用命令开启日志，adb shell setprop log.tag.PadController_ level(V,D,I,W,E)
 * 如果level是INFO级别，则命令处需要大于等于INFO级别即(I/W/E)
 */
fun logByOnOff(flag: String, msg: String, level: Int = Log.INFO) {
    if (Log.isLoggable(TAG_ON_OFF, level)) {
        log(flag, msg, level, TAG_ON_OFF)
    }
}
```
### [定时相关](kotlin_schedule.md)