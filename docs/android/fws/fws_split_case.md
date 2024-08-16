### 分屏UI定制
#### 分屏背景透明显示壁纸
方案一：

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
方案二：

frameworks/base/services/core/java/com/android/server/wm/LetterboxUiController.java
```java
private void updateWallpaperForLetterbox(WindowState mainWindow) {
    @LetterboxBackgroundType int letterboxBackgroundType =
            mLetterboxConfiguration.getLetterboxBackgroundType();
    //...
    wallpaperShouldBeShown |= mainWindow.getWindowingMode() == WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW;
    //...
}
```
#### 分隔栏修改
frameworks/base/

分割栏布局：libs/WindowManager/Shell/res/layout/split_divider.xml

分割栏背景：DockedDividerBackground

libs/WindowManager/Shell/res/values-land/styles.xml
libs/WindowManager/Shell/res/values/styles.xml

分割栏：
libs/WindowManager/Shell/res/values/dimen.xml

白条宽度：split_divider_handle_height

总宽度：split_divider_bar_width

加载分割栏布局：
libs/WindowManager/Shell/src/com/android/wm/shell/common/split/SplitWindowManager.java

#### 圆角裁剪
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

#### 导航栏背景修改
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

### 应用添加bar
DecorView中
```java
void onResourcesLoaded(LayoutInflater inflater, int layoutResource) {
    if (mDecorCaptionView != null) {
        //...
    } else {
        createDecorThreeDotsView();
        //...
    }
    //...
}

@Override
protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    //...
    createDecorThreeDotsView();

    //...
}

private ThreeDotsView mThreeDotsView;

private void createDecorThreeDotsView() {
    try {
        if (mThreeDotsView == null) {
            final WindowManager.LayoutParams attrs = mWindow.getAttributes();
            final boolean isApplication = attrs.type == TYPE_BASE_APPLICATION ||
                    attrs.type == TYPE_APPLICATION || attrs.type == TYPE_DRAWN_APPLICATION;
            final WindowConfiguration winConfig = getResources().getConfiguration().windowConfiguration;
            Rect bounds = winConfig.getBounds();
            boolean isActivity = mWindow != null && mWindow.getAppToken() != null && !mWindow.isFloating();
            if (isApplication && isActivity && winConfig.getActivityType() != ACTIVITY_TYPE_HOME) {
                mThreeDotsView = new ThreeDotsView(getContext());
                LayoutParams lp = new LayoutParams(200, 50, Gravity.CENTER_HORIZONTAL);
                addView(mThreeDotsView, lp);
            }
        }
        if (mThreeDotsView != null) {
            LayoutParams lp = (LayoutParams) mThreeDotsView.getLayoutParams();
            final WindowConfiguration winConfig = getResources().getConfiguration().windowConfiguration;
            Rect bounds = winConfig.getBounds();
            Rect maxBounds = winConfig.getMaxBounds();
            boolean isLandscape = maxBounds.width() > maxBounds.height();
            if (isLandscape) {
                lp.topMargin = bounds.left != 0 ? 0 : SystemBarUtils.getStatusBarHeight(getContext());
                lp.height = bounds.left != 0 ? 80 : 50;
            } else {
                lp.topMargin = bounds.top != 0 ? 0 : SystemBarUtils.getStatusBarHeight(getContext());
                lp.height = bounds.top != 0 ? 80 : 50;
            }
            if (mThreeDotsView.getParent() == null) {
                addView(mThreeDotsView, lp);
            } else {
                mThreeDotsView.setLayoutParams(lp);
            }
        }
    } catch (Exception e) {
        Log.e(TAG, "create DecorThreeDotsView error: " + e.getMessage());
    }
}
```

### 高斯模糊
#### 分屏应用拖拽模糊
frameworks/base/libs/WindowManager/Shell/src/com/android/wm/shell/common/split/SplitDecorManager.java
```java
public void onResizing(ActivityManager.RunningTaskInfo resizingTask, Rect newBounds,
            Rect sideBounds, SurfaceControl.Transaction t, int offsetX, int offsetY,
            boolean immediately) {
    if (mBackgroundLeash == null) {
        mBackgroundLeash = new SurfaceControl.Builder(mSurfaceSession)
                .setParent(mHostLeash)
                .setFormat(PixelFormat.TRANSLUCENT)
                .setName(RESIZING_BACKGROUND_SURFACE_NAME)
                .setCallsite("SurfaceUtils.makeColorLayer")
                .setEffectLayer()
                .build();

        t.setBackgroundBlurRadius(mBackgroundLeash, 50)
        .setLayer(mBackgroundLeash, Integer.MAX_VALUE - 1);
    }

    if (mGapBackgroundLeash == null && !immediately) {
        //...
        .setAlpha(mGapBackgroundLeash, 0.9f)
        .setBackgroundBlurRadius(mGapBackgroundLeash, 50)
    }            
}
```
应用没有被拉伸，需要进行scale结合crop？
```java
public void applySurfaceChanges(SurfaceControl.Transaction t, SurfaceControl leash1,
            SurfaceControl leash2, SurfaceControl dimLayer1, SurfaceControl dimLayer2,
            boolean applyResizingOffset) {
    //...
    int offset = mTempPosition - mDividePosition;
    Log.e("iiii", "applySurfaceChanges offset=" + offset);

    getRefBounds1(mTempRect);
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
#### 分屏界面背景模糊
原理：在壁纸图层上添加一层遮罩层

源码路径：frameworks/base/core/java/android/service/wallpaper/WallpaperService.java

##### 方式一
直接使用SurfaceControl.Transaction中的[setBackgroundBlurRadius](./fws_surface.md#setBackgroundBlurRadius)
```java
SurfaceControl mMaskSurfaceControl;
SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();
private Surface getOrCreateBLASTSurface(int width, int height, int format) {
    Surface ret = null;
    if (mBlastBufferQueue == null) {
        //...
        createMaskSurfaceControl();
    } else {
        //...
    }

    return ret;
}
private void createMaskSurfaceControl() {
    if (mMaskSurfaceControl == null) {
        mMaskSurfaceControl = new SurfaceControl.Builder()
                .setName("Wallpaper mask layer")
                .setCallsite("Wallpaper.Mask.Layer")
                .setParent(mBbqSurfaceControl)
                .setColorLayer()
                .build();
        mTransaction.setColor(mMaskSurfaceControl, Color.valueOf(Color.WHITE).getComponents())
                .setAlpha(mMaskSurfaceControl, 0.2f)
                .setBackgroundBlurRadius(mMaskSurfaceControl, 50)
                .apply();
    }
}

public void showOrHideMaskSurfaceControl(boolean show) {
    if (show) {
        mTransaction.show(mMaskSurfaceControl);
    } else {
        mTransaction.hide(mMaskSurfaceControl);
    }
    mTransaction.apply();
}
```
##### 方式二

适当的地方调用showOrHideMaskSurfaceControl

模糊实现api：

https://blog.csdn.net/abc6368765/article/details/127657069
https://blog.csdn.net/liaosongmao1/article/details/130820665