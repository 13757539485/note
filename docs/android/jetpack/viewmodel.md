用来解决横竖屏切换或者其他导致界面重新创建时数据的保存与恢复

### Activity
```kotlin
private val viewModel by lazy { ViewModelProvider(this)[BlankViewModel::class.java] }

class BlankViewModel : ViewModel() {
    private var data: String? = null

    fun getData(): String? {
        return data
    }

    fun setData(data: String?) {
        this.data = data
    }
}
```
onCreate使用
```kotlin
if (viewModel.getData() != null) {
    binding.showContent.text = viewModel.getData()
} else {
    viewModel.setData("123456")
    binding.showContent.text = viewModel.getData()
}
```
#### 创建fragment
```kotlin
private val blankFragment by lazy { BlankFragment.newInstance() }

val bt = supportFragmentManager.beginTransaction()
val fragment = supportFragmentManager.findFragmentByTag("blankFragment")
if (fragment == null) {
    bt.add(R.id.fragmentContainer, blankFragment, "blankFragment")
        .show(blankFragment)
        .commitNowAllowingStateLoss()
} else {
    supportFragmentManager.beginTransaction()
        .show(blankFragment)
        .commitNowAllowingStateLoss()
}
```
### Fragment
单独使用
```kotlin
private val viewModel by lazy { ViewModelProvider(this)[BlankViewModel::class.java] }
```
与Activity共用
```kotlin
private val viewModel by lazy { ViewModelProvider(requireActivity())[BlankViewModel::class.java] }
```