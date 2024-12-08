### replace方式
显示：
```kotlin
supportFragmentManager.beginTransaction()
.hide(this)
.add(
    R.id.xxx_fragment_container,
    xxxFragment,
    XXXFragment.TAG
)
.addToBackStack(null)
.commitAllowingStateLoss()
```
回退：
```kotlin
requireActivity().supportFragmentManager.popBackStack()
```
### show/hide方式

显示：
```kotlin
supportFragmentManager.beginTransaction().also {
    if (xxxFragment.isAdded) {
        it.show(musicFragment)
    } else {
        it.add(
            R.id.xxx_fragment_container,
            xxxFragment,
            XXXFragment.TAG
        )
    }
}.hide(this)
 .commitAllowingStateLoss()
```
隐藏：
```kotlin
supportFragmentManager.beginTransaction()
    .hide(xxxFragment)
    .show(this)
    .commitAllowingStateLoss()
```