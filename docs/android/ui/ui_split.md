官方案例：

https://developer.android.google.cn/guide/topics/large-screens/large-screen-canonical-layouts?hl=zh-cn&authuser=2
### xml适配
新建layout目录，会根据显示大小找对应的layout目录
```
layout default
layout-w100dp 1/3
layout-w600dp 1/2
layout-w840dp 2/3
layout-w960dp full
```

### 数据保存/恢复
#### Activity
传统方案：
```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState, outPersistentState)
    outState.putString("xxx",xxx)
}

override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState, persistentState)
    val str = savedInstanceState.getString("xxx")
}
```
[ViewModel方案](../jetpack/viewmodel.md#activity)
#### Fragment
传统方案：
```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState, outPersistentState)
    outState.putString("xxx",xxx)
}

override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onRestoreInstanceState(savedInstanceState, persistentState)
    val str = savedInstanceState?.getString("xxx")
}
```
[ViewModel方案](../jetpack/viewmodel.md#fragment)
```
#### View
以自定义SeekBar为例(系统自带的SeekBar不会保存max属性)
```kotlin
override fun onSaveInstanceState(): Parcelable {
    val superState = super.onSaveInstanceState()
    val ss = SavedState(superState)
    ss.progress = progress
    ss.secondaryProgress = secondaryProgress
    ss.max = max
    return ss
}

override fun onRestoreInstanceState(state: Parcelable?) {
    val ss = state as SavedState
    super.onRestoreInstanceState(ss.superState)
    max = ss.max
    progress = ss.progress
    secondaryProgress = ss.secondaryProgress
}

class SavedState : BaseSavedState {
    var progress: Int = 0
    var secondaryProgress: Int = 0
    var max: Int = 0

    internal constructor(superState: Parcelable?) : super(superState)
    private constructor(parcel: Parcel) : super(parcel) {
        progress = parcel.readInt()
        secondaryProgress = parcel.readInt()
        max = parcel.readInt()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeInt(progress)
        out.writeInt(secondaryProgress)
        out.writeInt(max)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState {
                return SavedState(source)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun describeContents(): Int {
        return 0
    }
}
```