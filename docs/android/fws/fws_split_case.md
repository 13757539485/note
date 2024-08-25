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

libs/WindowManager/Shell/src/com/android/wm/shell/common/split/DividerView.java

分割栏bar：mHandle(R.id.docked_divider_handle)

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

启动分屏：

frameworks/base/libs/WindowManager/Shell/src/com/android/wm/shell/splitscreen/StageCoordinator.java

startTasksWithLegacyTransition

#### 圆角裁剪
SplitWindowManager初始化在

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
        createDecorPointBarView();
        //...
    }
    //...
}

@Override
protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    //...
    createDecorPointBarView();

    //...
}

private PointBarView mPointBarView;

private boolean isActivity() {//不能判断出DialogFragment
    boolean isActivity;
    if ("com.tencent.mm".equals(mContext.getPackageName())) {
        isActivity = mWindow != null && mWindow.getAppToken() != null && !mWindow.isFloating();
    } else {
        isActivity = mWindow != null && mWindow.getAppToken() != null && !mWindow.isFloating() && !mWindow.isTranslucent();
    }
    Log.e(TAG, "isActivity: " + isActivity);
    return isActivity;
}

private void createDecorPointBarView() {
    try {
        Log.e(TAG, "createDecorPointBarView start:" +getContext().getPackageName());
        if (mPointBarView == null) {
            final WindowManager.LayoutParams attrs = mWindow.getAttributes();
            final boolean isApplication = attrs.type == TYPE_BASE_APPLICATION ||
                    attrs.type == TYPE_APPLICATION || attrs.type == TYPE_DRAWN_APPLICATION;
            final WindowConfiguration winConfig = getResources().getConfiguration().windowConfiguration;
            //自定义Launcher
            boolean isLauncher = "xxx.xxx".equals(getContext().getPackageName());
            if (!isLauncher && isActivity() && isApplication && winConfig.getActivityType() != WindowConfiguration.ACTIVITY_TYPE_HOME) {
                LayoutParams lp = new LayoutParams(120, 33,
                        Gravity.CENTER_HORIZONTAL);
                mPointBarView = new PointBarView(getContext());
                if (mPointBarView.getParent() == null) {
                    post(()-> addView(mPointBarView, lp));
                }
                Log.e(TAG, "create PointBarView success");
            }
        }
        if (mPointBarView != null) {
            LayoutParams lp = (LayoutParams) mPointBarView.getLayoutParams();
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
            mPointBarView.hidePopupWindowIfNeeded();
            if (mPointBarView.getParent() == null) {
                addView(mPointBarView, lp);
            } else {
                mPointBarView.setLayoutParams(lp);
            }
        }
    } catch (Exception e) {
        Log.e(TAG, "create DecorPointBarView error: " + e.getMessage());
    }
}
```
状态栏变化处理
```java
@Override
public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    //...
    adjustPointBarLayoutParams(insets);
    return insets;
} 

private void adjustPointBarLayoutParams(WindowInsets insets) {
    Log.e(TAG, "adjustPointBarLayoutParams: " + insets.getSystemWindowInsetsAsRect());
    if (mPointBarView != null && mPointBarView.getLayoutParams() != null) {
        try {
            final WindowConfiguration winConfig = getResources().getConfiguration().windowConfiguration;
            boolean isSplit = winConfig.getBounds().left != 0;
            LayoutParams layoutParams = (LayoutParams) mPointBarView.getLayoutParams();
            boolean statusBarVisible = insets.isVisible(WindowInsets.Type.statusBars())
                    || insets.getSystemWindowInsetTop() > 0;
            layoutParams.topMargin = isSplit ? 0 : getTopHeight(statusBarVisible);
            mPointBarView.post(()->{
                if (mPointBarView.getParent() == null) {
                    addView(mPointBarView, layoutParams);
                } else {
                    mPointBarView.setLayoutParams(layoutParams);
                }
                Log.e(TAG, "adjustPointBarLayoutParams result: " + layoutParams.topMargin);
            });
        } catch (Exception e) {
            Log.e(TAG, "adjustPointBarLayoutParams error: " + e.getMessage());
        }
    }
}

private int getTopHeight(boolean statusBarVisible) {
    boolean isWx = "com.tencent.mm".equals(mContext.getPackageName());
    int topHeight = isWx ? 86 : 99;
    return statusBarVisible ? topHeight : topHeight - 86;
}

private boolean isStatusBarVisible() {
    return getRootWindowInsets() != null && getRootWindowInsets().isVisible(WindowInsets.Type.statusBars());
}
```
#### 源码以及资源配置
添加引用，能在java代码中调用资源id
```xml
<java-symbol type="layout" name="point_bar_popup" />
<java-symbol type="drawable" name="icon_point_bar_left" />
<java-symbol type="drawable" name="icon_point_bar_right" />
<java-symbol type="drawable" name="icon_point_bar_float" />
<java-symbol type="drawable" name="dots" />
<java-symbol type="drawable" name="drag_text_bg" />
<java-symbol type="style" name="Animation.PopupWindow.HfcBar" />
```
PopupWindow动画
```xml
<style name="Animation.PopupWindow.HfcBar">
	<item name="windowEnterAnimation">@anim/cariad_popup_enter</item>
	<item name="windowExitAnimation">@anim/cariad_popup_exit</item>
</style>
```
View源码

[PointBarView](./code/fw/PointBarView.java)

[PointBarLinearLayout](./code/fw/PointBarLinearLayout.java)

[PointBarImageView](./code/fw/PointBarImageView.java)

### 高斯模糊
#### 分屏应用拖拽模糊以及添加应用名称
frameworks/base/libs/WindowManager/Shell/src/com/android/wm/shell/common/split/SplitLayout.java
```java
private int mTempPosition;
public int getTempPosition() {
    return mTempPosition - mDividePosition;
}
private void updateBounds(int position) {  
  mTempPosition = position;
  //...
}

@Override
public void onLayoutSizeChanging(SplitLayout layout) {
    mSyncQueue.runInSync(t -> {
        updateSurfaceBounds(layout, t);
        mMainStage.onResizing(getMainStageBounds(), t, layout.getTempPosition());
        mSideStage.onResizing(getSideStageBounds(), t, layout.getTempPosition());
    });
}
```

frameworks/base/libs/WindowManager/Shell/src/com/android/wm/shell/common/split/SplitDecorManager.java

布局
frameworks/base/libs/WindowManager/Shell/res/layout/split_decor.xml
```xml
<TextView
    android:id="@+id/split_resizing_name"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="25dp"
    android:textColor="#FF000000"
    android:visibility="gone"
    android:maxLines="1"
    android:ellipsize="end"
    android:textSize="27sp" />
```
在java代码中mResizingIconView初始化的地方初始化文本组件

```java
private static final int ICON_RADIUS = 17;
private TextView mResizingTextView;
private SurfaceControl mScreenshotLeash;
private int mIconLeashWidth, mIconLeashHeight;

mResizingTextView = rootLayout.findViewById(R.id.split_resizing_name);

public void onResizing(ActivityManager.RunningTaskInfo resizingTask, Rect newBounds,
            Rect sideBounds, SurfaceControl.Transaction t, int offsetX, int offsetY,
            boolean immediately, int tempPosition) {
    //...
    boolean isLeftLeash = newBounds.left == SystemBarUtils.SPLIT_LEFT_RIGHT_PADDING;
    if (mScreenshotLeash == null) { //添加应用截图层
        Rect temp = new Rect(newBounds);
        if (isLeftLeash) {
            temp.offset(-SystemBarUtils.SPLIT_LEFT_RIGHT_PADDING, -newBounds.top);
        } else {
            temp.offset(-newBounds.left, -newBounds.top);
        }
        mScreenshotLeash = ScreenshotUtils.takeScreenshot(t, mHostLeash, temp, SPLIT_DIVIDER_LAYER - 2);
    }
    t.setPosition(mScreenshotLeash, 0, 0);
    int oriWidth = isLeftLeash ? newBounds.width() - tempPosition : newBounds.width() + tempPosition;
    if (oriWidth != 0) {
        float scaleX = newBounds.width() * 1.0f / oriWidth;
        float bgRadius = SystemBarUtils.SPLIT_WINDOW_CORNER_RADIUS * 2 / scaleX;
        t.setMatrix(mScreenshotLeash, scaleX, 0, 0, 1.0f);
        t.setCornerRadius(mScreenshotLeash, bgRadius);
    }
    if (mBackgroundLeash == null) {
        //...
        t.setColor(mBackgroundLeash, Color.valueOf(Color.WHITE).getComponents())
                .setAlpha(mBackgroundLeash, 0.85f)
                .setBackgroundBlurRadius(mBackgroundLeash, 50)
    }
    if (mIcon == null && resizingTask.topActivityInfo != null) {
        mResizingIconView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        ICON_RADIUS * view.getContext().getResources().getDisplayMetrics().density);
            }
        });
        mResizingIconView.setClipToOutline(true);
        String appName = getAppNameFromActivityInfo(mResizingIconView.getContext(),
                resizingTask.topActivityInfo);
        mResizingTextView.setText(appName);
        mResizingTextView.setVisibility(View.VISIBLE);
        //...
        DisplayMetrics displayMetrics = mResizingIconView.getContext().getResources().getDisplayMetrics();
        lp.width = mIconLeashWidth = displayMetrics.widthPixels / 2;

        TextPaint paint = mResizingTextView.getPaint();
        paint.setTextSize(mResizingTextView.getTextSize());
        int textViewWidth = newBounds.width() - mResizingTextView.getPaddingLeft() - mResizingTextView.getPaddingRight();

        StaticLayout staticLayout = StaticLayout.Builder.obtain(appName, 0, appName.length(), paint, textViewWidth)
                .setMaxLines(mResizingTextView.getMaxLines())
                .setLineSpacing(mResizingTextView.getLineSpacingExtra(), mResizingTextView.getLineSpacingMultiplier())
                .setIncludePad(mResizingTextView.getIncludeFontPadding())
                .build();

        int totalHeight = staticLayout.getHeight() + mResizingTextView.getPaddingTop() + mResizingTextView.getPaddingBottom();
        mIconLeashHeight = (int) (100 * displayMetrics.density + totalHeight);
        lp.height = mIconLeashHeight;
        //...
    }
    t.setPosition(mIconLeash, newBounds.width() / 2.0f - mIconLeashWidth / 2.0f,
        newBounds.height() / 2.0f - mIconLeashHeight / 2.0f);
}

private String getAppNameFromActivityInfo(Context context, ActivityInfo activityInfo) {
    PackageManager packageManager = context.getPackageManager();
    String name = activityInfo.applicationInfo.loadLabel(packageManager).toString();
    return name;
}

public void onResized(Rect newBounds, SurfaceControl.Transaction t) {
   if (mScreenshotLeash != null) {
            t.remove(mScreenshotLeash);
            mScreenshotLeash = null;
        }
   if (mIcon != null) {
        //...    
            mResizingTextView.setVisibility(View.GONE);
            mResizingTextView.setText("");
       //... 
   }
}

public void release(SurfaceControl.Transaction t) {
    //...
    if (mScreenshotLeash != null) {
            t.remove(mScreenshotLeash);
            mScreenshotLeash = null;
    }
    //...
}
```

#### 分屏拖动icon不正确问题
frameworks/base/libs/WindowManager/Shell/src/com/android/wm/shell/splitscreen/StageTaskListener.java

通过包名处理
```java
void onResizing(Rect newBounds, SurfaceControl.Transaction t, int tempPosition) {
    if (mSplitDecorManager != null && mRootTaskInfo != null && mRootTaskInfo.topActivity != null) {
        if ("xxx.xxx.xxx".equals(mRootTaskInfo.topActivity.getPackageName())) {
            int size = mChildrenTaskInfo.size();
            for (int i = 0; i < size; i++) {
                ActivityManager.RunningTaskInfo runningTaskInfo = mChildrenTaskInfo.valueAt(i);
                if (runningTaskInfo != null && runningTaskInfo.topActivity != null) {
                    if (!"xxx.xxx.xxx".equals(runningTaskInfo.topActivity.getPackageName())) {
                        Log.e(TAG, "onResizing child: " + runningTaskInfo.topActivity.toShortString() );
                        mSplitDecorManager.onResizing(runningTaskInfo, newBounds, t, tempPosition);
                        break;
                    }
                }
            }
        } else {
            mSplitDecorManager.onResizing(mRootTaskInfo, newBounds, t, tempPosition);
        }
    }
}
```
#### 分屏界面背景模糊
原理：在壁纸图层上添加一层遮罩层

源码路径：frameworks/base/core/java/android/service/wallpaper/WallpaperService.java

##### 方式一(耗性能)
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
原理：获取壁纸然后进行模糊处理，再添加图层

frameworks/base/core/java/android/service/wallpaper/WallpaperService.java
```java
SurfaceControl mMaskSurfaceControl;

SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();

public class Engine {
/**
 * @hide
 */
public void onMaskData() {

}

void detach() {
    //...
    if (mCreated) {
    destroyMaskSurface();
    }
}
   
private void destroyMaskSurface() {
    if (mMaskSurfaceControl != null) {
        mTransaction.remove(mMaskSurfaceControl).apply();
        mMaskSurfaceControl = null;
    }
}
/**
     * Show mask on wallpaper when enter split mode
     * @hide
     * @param show whether show mask
     */
    public void showOrHideMaskSurfaceControl(boolean show) {
        if (mMaskSurfaceControl == null) {
            Log.e(TAG, "showOrHideMaskSurfaceControl error due to mMaskSurfaceControl is null: show = [" + show + "]");
            return;
        }
        if (show) {
            mTransaction.show(mMaskSurfaceControl);
        } else {
            mTransaction.hide(mMaskSurfaceControl);
        }
        mTransaction.apply();
    }
    
    void updateMaskData(Bitmap bitmap) {
        Bitmap blur = SystemBarUtils.blurBitmap(mDisplayContext, bitmap, 0.3f, 25.0f);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int format = PixelFormat.RGBA_8888;
        int usage = GraphicBuffer.USAGE_HW_TEXTURE | GraphicBuffer.USAGE_SW_WRITE_OFTEN;
        GraphicBuffer graphicBuffer = GraphicBuffer.create(width, height, format, usage);

        Canvas canvas = graphicBuffer.lockCanvas();
        canvas.drawBitmap(blur, 0, 0, null);
        graphicBuffer.unlockCanvasAndPost(canvas);
        mTransaction.setBuffer(mMaskSurfaceControl, graphicBuffer);
        mTransaction.apply();

        onMaskData();
    }
    private void createMaskSurfaceControl() {
        Log.e(TAG, "createMaskSurfaceControl: " + mMaskSurfaceControl);
        if (mMaskSurfaceControl == null) {
            mMaskSurfaceControl = new SurfaceControl.Builder()
                    .setName("Wallpaper mask layer")
                    .setCallsite("Wallpaper.Mask.Layer")
                    .setParent(mSurfaceControl)
                    .setFormat(PixelFormat.TRANSLUCENT)
                    .setBLASTLayer()
                    .build();
            mTransaction.setColorSpace(mMaskSurfaceControl, ColorSpace.get(ColorSpace.Named.SRGB))
                    .apply();
        }
    }
    
    private Surface getOrCreateBLASTSurface(int width, int height, int format) {
        //...
        if (mBlastBufferQueue == null) {
            createMaskSurfaceControl();
        }
        //...
    }
    
    public void reportShown() {
        mConnection.engineShown(this);

        if (mEngine != null) {
            mEngine.updateMaskData(mWallpaperManager.getBitmap(false));
        }
    }
}
```

在子类中每次壁纸切换的时候更新模糊图层

frameworks/base/packages/SystemUI/src/com/android/systemui/ImageWallpaper.java
```java
class GLEngine extends Engine implements DisplayListener {
    @Override
    public void onMaskData() {
        super.onMaskData();
        if (isShowMask) {
            showOrHideMaskSurfaceControl(true);
        }
    }
}
```
模糊实现api：

https://blog.csdn.net/abc6368765/article/details/127657069
https://blog.csdn.net/liaosongmao1/article/details/130820665

```java
/**
 * Blur the bitmap
 * @param context the context
 * @param bitmap pending bitmap
 * @param scale scale the bitmap to achieve more blur
 * @param radius blur radius between 0~25
 * @return
 */
public static Bitmap blurBitmap(Context context, Bitmap bitmap, float scale, float radius) {
    if (bitmap == null) {
        return null;
    }
    Log.i("TAG", "blurBitmap bitmap: " + bitmap.getByteCount());
    int oriWidth = bitmap.getWidth();
    int oriHeight = bitmap.getHeight();
    int scaleWidth = Math.round(oriWidth * scale);
    int scaleHeight = Math.round(oriHeight * scale);
    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, false);
    // 用需要创建高斯模糊bitmap创建一个空的bitmap
    Bitmap outBitmap = Bitmap.createBitmap(scaledBitmap);
    // 初始化Renderscript，该类提供了RenderScript context，创建其他RS类之前必须先创建这个类，其控制RenderScript的初始化，资源管理及释放
    RenderScript rs = RenderScript.create(context);
    // 创建高斯模糊对象
    ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
    // 创建Allocations，此类是将数据传递给RenderScript内核的主要方法，并制定一个后备类型存储给定类型
    Allocation allIn = Allocation.createFromBitmap(rs, scaledBitmap);
    Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
    //设定模糊度(注：Radius最大只能设置25.f)
    blurScript.setRadius(radius);
    // Perform the Renderscript
    blurScript.setInput(allIn);
    blurScript.forEach(allOut);
    // Copy the final bitmap created by the out Allocation to the outBitmap
    allOut.copyTo(outBitmap);
    // recycle the original bitmap
    bitmap.recycle();
    // After finishing everything, we destroy the Renderscript.
    rs.destroy();
    Bitmap recBitmap = Bitmap.createScaledBitmap(outBitmap, oriWidth, oriHeight, true);
    return applyDarkness(recBitmap, 0.2f);
}

public static Bitmap applyDarkness(Bitmap image, float darkness) {
    if (image == null) {
        return null;
    }
    Bitmap outputBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig());
    Canvas canvas = new Canvas(outputBitmap);
    canvas.drawBitmap(image, 0, 0, null);

    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.setAlpha((int) (darkness * 255));
    canvas.drawRect(0, 0, image.getWidth(), image.getHeight(), paint);

    return outputBitmap;
}
```

#### 动态控制模糊
frameworks/base/packages/SystemUI/src/com/android/systemui/ImageWallpaper.java
```java
private Engine mEngine;

private WallpaperReceive mWallpaperReceive;

boolean isShowMask;

@Override
public void onCreate() {
    //...
    mWallpaperReceive = new WallpaperReceive();
    IntentFilter filter = new IntentFilter();
    filter.addAction("action.wallpaper.mask_visibility");
    registerReceiver(mWallpaperReceive, filter, "com.android.systemui.permission.WALLPAPER_MASK", null);
}

@Override
public Engine onCreateEngine() {
    return mEngine = new GLEngine();
}

@Override
public void onDestroy() {
    //...
    if (mWallpaperReceive != null) {
        unregisterReceiver(mWallpaperReceive);
    }
}

private class WallpaperReceive extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called with: context = [" + context + "], intent = [" + intent + "]"+ this);
        if ("action.wallpaper.mask_visibility".equals(intent.getAction())) {
            if (mEngine != null) {
                boolean maskShow = intent.getBooleanExtra("mask", false);
                if (isShowMask == maskShow) {
                    Log.i(TAG, "onReceive not update surface");
                    return;
                }
                mEngine.showOrHideMaskSurfaceControl(maskShow);
                isShowMask = maskShow;
            }
        }
    }
}
```
用于广播仅限同签名应用使用，保证只能系统应用控制

frameworks/base/packages/SystemUI/AndroidManifest.xml

<permission android:name="com.android.systemui.permission.WALLPAPER_MASK"
    android:protectionLevel="signature" />

具体控制代码
```java
private void showOrHideWallpaperMask(boolean isShow) {
    Intent intent = new Intent("action.wallpaper.mask_visibility");
    intent.putExtra("mask", isShow);
    context.sendBroadcast(intent);
}
```