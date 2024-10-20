#### 使用百分比
百分比控制组件大小
```xml
<style name="ParentDefaultStyle">
    <item name="android:layout_width">0dp</item>
    <item name="android:layout_height">0dp</item>
    <item name="layout_constraintWidth_default">percent</item>
    <item name="layout_constraintHeight_default">percent</item>
</style>
```
设置组件宽高1：1
```xml
<androidx.constraintlayout.widget.Guideline
    android:id="@+id/albumGuidelineL"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    app:layout_constraintGuide_percent="0.097" />

<androidx.constraintlayout.widget.Guideline
    android:id="@+id/albumGuidelineR"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    app:layout_constraintGuide_percent="0.321" />

<androidx.appcompat.widget.AppCompatImageView
    android:id="@+id/album"
    style="@style/ParentDefaultStyle"
    android:src="@mipmap/default_album"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintDimensionRatio="1:1"
    app:layout_constraintEnd_toStartOf="@id/albumGuidelineR"
    app:layout_constraintStart_toEndOf="@+id/albumGuidelineL"
    app:layout_constraintTop_toTopOf="parent" /> 

```

#### Group
控制多个组件显示和隐藏
```xml
<androidx.constraintlayout.widget.Group
    android:id="@+id/groupLayout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="gone"
    app:constraint_referenced_ids="id1,id2,id3" />
```
使用：

bindng.groupLayout.isVisible = true/false