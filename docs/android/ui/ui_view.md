### <a id="view_lifecycle">自定义view之lifecycle</a>
```kotlin
abstract class BaseConstraintLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), LifecycleOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
```
### 自定义view之裁剪区域
自定义属性：
```xml
<declare-styleable name="CutStatusBarView">
    // 布局中是否排除状态栏
    <attr name="statusBarAdapter" format="boolean" />
    // 布局中是否排除导航栏
    <attr name="navigationBarAdapter" format="boolean" />
    // 布局中是否排除刘海区
    <attr name="cutoutAdapter" format="boolean" />
</declare-styleable>
```
源码：
```kotlin
open class CutStatusBarView(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs, 0) {
    private var statusBarAdapter: Boolean = true
    private var navigationBarAdapter: Boolean = true
    private var cutoutAdapter: Boolean = true

    init {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, R.styleable.CutStatusBarView)
        statusBarAdapter =
            obtainStyledAttributes.getBoolean(R.styleable.CutStatusBarView_statusBarAdapter, true)
        navigationBarAdapter = obtainStyledAttributes.getBoolean(
            R.styleable.CutStatusBarView_navigationBarAdapter,
            true
        )
        cutoutAdapter =
            obtainStyledAttributes.getBoolean(R.styleable.CutStatusBarView_cutoutAdapter, true)
        obtainStyledAttributes.recycle()
    }

    override fun onApplyWindowInsets(insets: WindowInsets?): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets?.let {
                setPadding(
                    if (cutoutAdapter) it.getInsets(WindowInsets.Type.displayCutout()).left else 0,
                    if (statusBarAdapter) it.getInsets(WindowInsets.Type.statusBars()).top else 0,
                    0,
                    if (navigationBarAdapter) it.getInsets(WindowInsets.Type.navigationBars()).bottom else 0
                )
            }
        } else {
            setPadding(
                if (cutoutAdapter && context is Activity) ImmersionBar.getNotchHeight(context as Activity) else 0,
                if (statusBarAdapter) ImmersionBar.getStatusBarHeight(context) else 0,
                0,
                if (navigationBarAdapter) ImmersionBar.getNavigationBarHeight(context) else 0
            )
        }
        return super.onApplyWindowInsets(insets)
    }
}
```
兼容低版本需要添加状态栏[immersionbar](../android_github.md#immersionbar)处理库

### Viewbindng使用
[开启viewbinding](../android_studio.md#viewbinding)

如果XXXBinding是merge作为根布局
```kotlin
private val binding: XXXBinding =
    XXXBinding.inflate(LayoutInflater.from(context), this)
```

如果XXXBinding是非merge作为根布局
```kotlin
private val binding: XXXBinding =
    XXXBinding.inflate(LayoutInflater.from(context), this, true)
```

#### Activity/Fragment中include的使用
IncludeXXXBinding根布局是非merge
```xml
<include
    android:id="@+id/includeLayout"
    layout="@layout/include_xxx_device" />
```
可以使用binding.includeLayout.xxx来调用include_xxx_device中的组件

IncludeXXXBinding根布局是merge情况下只能以下方式
```kotlin
private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
private lateinit var includeXXXBinding: IncludeXXXBinding
includeNXXXBinding = IncludeXXXBinding.bind(binding.root)
```
include_xxx_device不需要申明id，使用includeNXXXBinding.xxx来调用include_xxx_device中的组件，也使用于非merge

### 常见拓展
禁止快速点击
```kotlin
fun View.click(listener: (view: View) -> Unit) {
    val minTime = 500L
    var lastTime = 0L
    this.setOnClickListener {
        val tmpTime = System.currentTimeMillis()
        if (tmpTime - lastTime > minTime) {
            lastTime = tmpTime
            listener.invoke(this)
        } else {
            Log.d("UI", "点击过快，取消触发")
        }
    }
}
```
单位转化
```kotlin
fun View.dp2px(dpValue: Float): Int = context.dp2px(dpValue)

fun View.px2dp(dpValue: Float): Int = context.px2dp(dpValue)

fun Context.dp2px(dipValue: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dipValue, resources.displayMetrics
    ).toInt()
}

fun Context.px2dp(pxValue: Float): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        TypedValue.deriveDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            pxValue, resources.displayMetrics
        ).toInt() else (pxValue / resources.displayMetrics.density).toInt()
}
```