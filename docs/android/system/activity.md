### 拦截返回手势
```kotlin
open class BackActivity : AppCompatActivity() {
    private val onBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!onBackEvent()) {
                    Log.d("BackActivity", "BaseBackActivity handle back event.")
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    open fun onBackEvent(): Boolean = false
}
```
使用场景：Fragment拦截back手势
```kotlin
class MainActivity : BackActivity() {
    override fun onBackEvent(): Boolean {
        if (XxxFragment.onBackEvent()) {
            Log.d(TAG, "XxxFragment handle back event.")
            return true
        }
        return super.onBackEvent()
    }
}
```
Fragment简单封装
```kotlin
open class BackFragment: Fragment() {
    open fun onBackEvent(): Boolean = false
}
```
使用时注意判断isAdded
```kotlin
override fun onBackEvent(): Boolean {
    if (isAdded) {
        //todo
        return true
    }
    return false
}
```