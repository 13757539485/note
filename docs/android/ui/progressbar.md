### ProgressBar
白色水平圆角样式
```xml
<ProgressBar
    style="@style/Widget.AppCompat.ProgressBar.Horizontal"
    android:layout_width="180dp"
    android:layout_height="8dp"
    android:max="100"
    android:progress="0"
    android:progressDrawable="@drawable/progress_bar" />
```
资源文件progress_bar.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@android:id/background" android:height="@dimen/dp8">
        <shape>
            <corners android:radius="@dimen/dp8" />
            <solid android:color="#66FFFFFF" />
        </shape>
    </item>

    <item android:id="@android:id/progress" android:height="@dimen/dp8">
        <scale
            android:drawable="@drawable/progress_current"
            android:scaleWidth="100%" />
    </item>
</layer-list>
```
资源文件progress_current.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#FFFFFFFF" />
    <corners android:radius="@dimen/dp8" />
</shape>
```