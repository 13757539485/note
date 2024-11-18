### 屏幕适配
https://github.com/JessYanCoding/AndroidAutoSize

引入：
```kts
maven { url "https://jitpack.io" }

implementation("me.jessyan:autosize:1.2.1")
```

小米设备crash问题
```kotlin
override fun getResources(): Resources {
    val resources = super.getResources()
    if (ThreadUtils.isMainThread()) {
        AutoSizeCompat.autoConvertDensityOfGlobal(resources)
    }
    return resources
}
```

dialog问题
```kotlin
abstract class AutoSizeDialog : Dialog {
    constructor(context: Context) : super(context)
    constructor(context: Activity, themeResId: Int) : super(context, themeResId)
    constructor(
        context: Activity, cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener
    ) : super(context, cancelable, cancelListener)

    override fun show() {
        window?.let {
            it.attributes.width = initWinWidth()
            it.attributes.height = initWinHeight()
        }
        super.show()
    }
    abstract fun initWinWidth(): Int
    abstract fun initWinHeight(): Int
}
```