frameworks/base/core/java/com/android/internal/policy/DecorView.java
```java
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    // ...
    if (changed) {
        WindowConfiguration wc = mContext.getResources().getConfiguration().windowConfiguration;
        if (wc.getActivityType() == ACTIVITY_TYPE_HOME) {
            return;
        }
        Rect bounds = wc.getBounds();
        if (bounds.left != 0) {
            // split
            Log.e(TAG, "onLayout split");
            mWindow.toggleWallpaper(true);
        } else {
            // full
            Log.e(TAG, "onLayout full");
            mWindow.toggleWallpaper(false);
        }
    }
}
```
frameworks/base/core/java/com/android/internal/policy/PhoneWindow.java
```java
private boolean mOldShowWallpaper;

public void toggleWallpaper(boolean showWallpaper) {
    if (showWallpaper) {
        mOldShowWallpaper = isShowingWallpaper();
        setFlags(FLAG_SHOW_WALLPAPER, FLAG_SHOW_WALLPAPER&(~getForcedWindowFlags()));
    } else {
        if (!mOldShowWallpaper) {
            clearFlags(FLAG_SHOW_WALLPAPER);
        }
    }
}
```
frameworks/base/

分割栏布局：libs/WindowManager/Shell/res/layout/split_divider.xml

分割栏背景：DockedDividerBackground

libs/WindowManager/Shell/res/values-land/styles.xml
libs/WindowManager/Shell/res/values/styles.xml

分割栏：
libs/WindowManager/Shell/res/values/dimen.xml

白条宽度：split_divider_handle_height

总宽度：split_divider_bar_width

圆角裁剪相关：
libs/WindowManager/Shell/src/com/android/wm/shell/common/split/SplitLayout.java

updateBounds：设置分屏Rect
```java
private void updateBounds(int position, Rect bounds1, Rect bounds2, Rect dividerBounds,
            boolean setEffectBounds) {
    // ...
    mStatusBarHeight = getStatusBarHeight(mContext);
    mNavigationBarHeight = getNavigationBarHeight(mContext);
    final boolean isLandscape = isLandscape(mRootBounds);
    if (isLandscape) {
        //...
        bounds1.left += mNotchRect != null ? mNotchRect.width() : 128;
        bounds1.right = position;
        bounds1.top += mStatusBarHeight;
        bounds1.bottom -= 30;
        bounds2.top += mStatusBarHeight;
        bounds2.left = bounds1.right + mDividerSize;
        bounds2.right -= mNavigationBarHeight;
        bounds2.bottom -= 30;
    } else {
        //...
        bounds1.top += mStatusBarHeight;
        bounds1.left += 30;
        bounds1.right -= 30;
        bounds2.top = bounds1.bottom + mDividerSize;
        bounds2.bottom -= mNavigationBarHeight;
        bounds2.left += 30;
        bounds2.right -= 30;
    }
}
public int getStatusBarHeight(Context context) {
    int result = 0;
    int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
        result = context.getResources().getDimensionPixelSize(resourceId);
    }
    return result;
}

public int getNavigationBarHeight(Context context) {
    Resources resources = context.getResources();
    int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    int result = 0;
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId);
    }
    return result;
}
```
applySurfaceChanges: 圆角裁剪
```java
public void applySurfaceChanges(SurfaceControl.Transaction t, SurfaceControl leash1,
            SurfaceControl leash2, SurfaceControl dimLayer1, SurfaceControl dimLayer2,
            boolean applyResizingOffset) {
    //...
    t.setPosition(leash1, mTempRect.left, mTempRect.top)
            .setWindowCrop(leash1, mTempRect.width(), mTempRect.height())
            .setCornerRadius(leash1, 30);
    getRefBounds2(mTempRect);
    t.setPosition(leash2, mTempRect.left, mTempRect.top)
            .setWindowCrop(leash2, mTempRect.width(), mTempRect.height())
            .setCornerRadius(leash1, 30);
    //...
}
```
加载分割栏布局：
libs/WindowManager/Shell/src/com/android/wm/shell/common/split/SplitWindowManager.java

导航栏背景修改：
packages/SystemUI/src/com/android/systemui/navigationbar/NavigationBarTransitions.java
```java
public NavigationBarTransitions(
        NavigationBarView view,
        IWindowManager windowManagerService,
        LightBarTransitionsController.Factory lightBarTransitionsControllerFactory) {
    super(view, R.drawable.nav_background);
    mView = view;
    view.setBackgroundColor(Color.TRANSPARENT);
    //...
}
```

横屏时获取侧边刘海宽度
```kotlin
val rootWindowInsets: WindowInsets = window.decorView.rootWindowInsets
    if (rootWindowInsets != null) {
        val displayCutout = rootWindowInsets.displayCutout
        if (displayCutout != null) {
            val boundingRects: List<Rect> = displayCutout.boundingRects
            if (boundingRects.isNotEmpty()) {
                val rect: Rect = boundingRects[0]
            }
        }
    }
```
待解决：返回桌面动画消失