https://juejin.cn/post/7223192684126535735

### 启动流程
frameworks/base/services/java/com/android/server/SystemServer.java
```java
try {
    startSystemUi(context, windowManagerF);
} catch (Throwable e) {
    reportWtf("starting System UI", e);
}

private static void startSystemUi(Context context, WindowManagerService windowManager) {
    PackageManagerInternal pm = LocalServices.getService(PackageManagerInternal.class);
    Intent intent = new Intent();
    intent.setComponent(pm.getSystemUiServiceComponent());
    intent.addFlags(Intent.FLAG_DEBUG_TRIAGED_MISSING);
    //Slog.d(TAG, "Starting service: " + intent);
    context.startServiceAsUser(intent, UserHandle.SYSTEM);
    windowManager.onSystemUiStarted();
}
```
frameworks/base/services/core/java/com/android/server/pm/PackageManagerInternalBase.java
```java
public final ComponentName getSystemUiServiceComponent() {
    return ComponentName.unflattenFromString(getContext().getResources().getString(
            com.android.internal.R.string.config_systemUIServiceComponent));
}
```
frameworks/base/core/res/res/values/config.xml
```xml
<string name="config_systemUIServiceComponent" translatable="false"
>com.android.systemui/com.android.systemui.SystemUIService</string>
```
frameworks/base/packages/SystemUI/src/com/android/systemui/SystemUIService.java
```java
@Override
    public void onCreate() {
        super.onCreate();
        // Start all of SystemUI
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        //...
}
```
frameworks/base/packages/SystemUI/src/com/android/systemui/SystemUIApplication.java
```java
void startSecondaryUserServicesIfNeeded() {
    String[] names = SystemUIFactory.getInstance().getSystemUIServiceComponentsPerUser(
            getResources());
    startServicesIfNeeded(/* metricsPrefix= */ "StartSecondaryServices", names);
}

public String[] getSystemUIServiceComponentsPerUser(Resources resources) {
        return resources.getStringArray(R.array.config_systemUIServiceComponentsPerUser);
    }
```
config_systemUIServiceComponentsPerUser在
frameworks/base/packages/SystemUI/res/values/config.xml
```xml
<string-array name="config_systemUIServiceComponents" translatable="false">
    <item>com.android.systemui.util.NotificationChannels</item>
    <item>com.android.systemui.keyguard.KeyguardViewMediator</item>
    <item>com.android.systemui.recents.Recents</item>
    <item>com.android.systemui.volume.VolumeUI</item>
    <item>com.android.systemui.statusbar.phone.StatusBar</item>
    <item>com.android.systemui.usb.StorageNotification</item>
    <item>com.android.systemui.power.PowerUI</item>
    <item>com.android.systemui.media.RingtonePlayer</item>
    <item>com.android.systemui.keyboard.KeyboardUI</item>
    <item>com.android.systemui.shortcut.ShortcutKeyDispatcher</item>
    <item>@string/config_systemUIVendorServiceComponent</item>
    <item>com.android.systemui.util.leak.GarbageMonitor$Service</item>
    <item>com.android.systemui.LatencyTester</item>
    <item>com.android.systemui.globalactions.GlobalActionsComponent</item>
    <item>com.android.systemui.ScreenDecorations</item>
    <item>com.android.systemui.biometrics.AuthController</item>
    <item>com.android.systemui.SliceBroadcastRelayHandler</item>
    <item>com.android.systemui.statusbar.notification.InstantAppNotifier</item>
    <item>com.android.systemui.theme.ThemeOverlayController</item>
    <item>com.android.systemui.accessibility.WindowMagnification</item>
    <item>com.android.systemui.accessibility.SystemActions</item>
    <item>com.android.systemui.toast.ToastUI</item>
    <item>com.android.systemui.wmshell.WMShell</item>
</string-array>
```
中定义，将定义的组件遍历创建
```java
private void startServicesIfNeeded(String metricsPrefix, String[] services) {
    //...
    mServices = new SystemUI[services.length];//使用数组存储所有组件
    //...
    final int N = services.length;
    for (int i = 0; i < N; i++) {
        String clsName = services[i];
        //...
        SystemUI obj = mComponentHelper.resolveSystemUI(clsName);
        if (obj == null) {
            Constructor constructor = Class.forName(clsName).getConstructor(Context.class);
            obj = (SystemUI) constructor.newInstance(this);//通过反射创建
        }
        mServices[i] = obj;
        //...
        mServices[i].start();//调用start方法
        //...
        if (mBootCompleteCache.isBootComplete()) {
            mServices[i].onBootCompleted();
        }
    }
    //...
}
```

### SystemBar创建
```java
public void start() {
    //...
    mBarService = IStatusBarService.Stub.asInterface(
        ServiceManager.getService(Context.STATUS_BAR_SERVICE));
    RegisterStatusBarResult result = null;
    try {
        result = mBarService.registerStatusBar(mCommandQueue);
    } catch (RemoteException ex) {
        ex.rethrowFromSystemServer();
    }

    createAndAddWindows(result);
    //...
}
```
先在StatusBarManagerService中注册监听获取result，然后创建视图
```java
public void createAndAddWindows(@Nullable RegisterStatusBarResult result) {
    makeStatusBarView(result);
    mNotificationShadeWindowController.attach();
    mStatusBarWindowController.attach();
}
protected void makeStatusBarView(@Nullable      RegisterStatusBarResult result) {
    //...
    inflateStatusBarWindow();
    //...
}
```
makeStatusBarView中创建整个SystemUI视图，状态栏、下拉栏、导航栏、锁屏等，inflateStatusBarWindow中初始化StatusBarWindowModule
```java
private void inflateStatusBarWindow() {
    mStatusBarComponent = mStatusBarComponentFactory.create();
    //...
    mNotificationShadeWindowView = mStatusBarComponent.getNotificationShadeWindowView();
    mNotificationShadeWindowViewController = mStatusBarComponent
            .getNotificationShadeWindowViewController();
    mNotificationShadeWindowController.setNotificationShadeView(mNotificationShadeWindowView);
    mNotificationShadeWindowViewController.setupExpandedStatusBar();
    mNotificationPanelViewController = mStatusBarComponent.getNotificationPanelViewController();
    mStatusBarComponent.getLockIconViewController().init();
    mStackScrollerController = mStatusBarComponent.getNotificationStackScrollLayoutController();
    mStackScroller = mStackScrollerController.getView();

    mNotificationShelfController = mStatusBarComponent.getNotificationShelfController();
    //...
}
```
#### ExpandedStatusBar
其中getNotificationShadeWindowView对应StatusBarWindowModule中的
```java
public static NotificationShadeWindowView providesNotificationShadeWindowView(
        LayoutInflater layoutInflater) {
    NotificationShadeWindowView notificationShadeWindowView = (NotificationShadeWindowView)
            layoutInflater.inflate(R.layout.super_notification_shade, /* root= */ null);
    if (notificationShadeWindowView == null) {
        throw new IllegalStateException(
                "R.layout.super_notification_shade could not be properly inflated");
    }

    return notificationShadeWindowView;
}
```
然后再mNotificationShadeWindowController.attach()中添加Window
```java
public void attach() {
    mLp = new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
        LayoutParams.TYPE_NOTIFICATION_SHADE,
        LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                | LayoutParams.FLAG_SPLIT_TOUCH
                | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        PixelFormat.TRANSLUCENT);
    //...
}
```
#### StatusBar
```java
protected void makeStatusBarView(@Nullable RegisterStatusBarResult result) {
    //...
    mStatusBarWindowController.getFragmentHostManager()
        .addTagListener(CollapsedStatusBarFragment.TAG, (tag, fragment) -> {
            StatusBarFragmentComponent statusBarFragmentComponent =
                    ((CollapsedStatusBarFragment) fragment).getStatusBarFragmentComponent();
            //...
            mStatusBarView = statusBarFragmentComponent.getPhoneStatusBarView();
            mPhoneStatusBarViewController =
                    statusBarFragmentComponent.getPhoneStatusBarViewController();
            mNotificationPanelViewController.updatePanelExpansionAndVisibility();
            setBouncerShowingForStatusBarComponents(mBouncerShowing);

            mLightsOutNotifController.setLightsOutNotifView(
                    mStatusBarView.findViewById(R.id.notification_lights_out));
            mNotificationShadeWindowViewController.setStatusBarView(mStatusBarView);
            checkBarModes();
        }).getFragmentManager()
        .beginTransaction()
        .replace(R.id.status_bar_container,
                mStatusBarComponent.createCollapsedStatusBarFragment(),
                CollapsedStatusBarFragment.TAG)
        .commit();
    //...
```
mStatusBarWindowController是从StatusBar中传入，在StatusBarWindowController构造方法中有StatusBarWindowModule，这个Module会加载SystemUI的根布局
```kotlin
fun providesStatusBarWindowView(layoutInflater: LayoutInflater): StatusBarWindowView {
    return layoutInflater.inflate(
        R.layout.super_status_bar,
        /* root= */null
    ) as StatusBarWindowView?
        ?: throw IllegalStateException(
            "R.layout.super_status_bar could not be properly inflated"
        )
}
```
super_status_bar.xml布局如下
```xml
<com.android.systemui.statusbar.window.StatusBarWindowView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:sysui="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">

    <FrameLayout
        android:id="@+id/status_bar_launch_animation_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    <!--状态栏容器-->
    <FrameLayout
        android:id="@+id/status_bar_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/system_bar_background" />
</com.android.systemui.statusbar.window.StatusBarWindowView>
```
CollapsedStatusBarFragment创建走生命周期方法
```java
public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        Bundle savedInstanceState) {
    return inflater.inflate(R.layout.status_bar, container, false);
}
```
添加到Window中mStatusBarWindowController.attach();
```java
public void attach() {
    mLp = getBarLayoutParams(mContext.getDisplay().getRotation());

    mWindowManager.addView(mStatusBarWindowView, mLp);
    mLpChanged.copyFrom(mLp);

    mContentInsetsProvider.addCallback(this::calculateStatusBarLocationsForAllRotations);
    calculateStatusBarLocationsForAllRotations();
}
```
其中mLp
```java
private WindowManager.LayoutParams getBarLayoutParamsForRotation(int rotation) {
    int height = mBarHeight;
    //...
    WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_STATUS_BAR,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT);
    lp.privateFlags |= PRIVATE_FLAG_COLOR_SPACE_AGNOSTIC;
    lp.token = new Binder();
    lp.gravity = Gravity.TOP;
    lp.setFitInsetsTypes(0 /* types */);
    lp.setTitle("StatusBar");
    lp.packageName = mContext.getPackageName();
    lp.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
    return lp;
}
```
#### NavigationBar
也在StatusBar中创建但添加到Window比StatusBar早，NavigationBar在makeStatusBarView就创建并添加到Window上，而StatusBar是在执行mStatusBarWindowController.attach()才添加
```java
protected void makeStatusBarView(@Nullable RegisterStatusBarResult result) {
    //...
    try {
        boolean hide_systembar = Settings.System.getInt(mContext.getContentResolver(),Settings.System.ALWAYS_HIDE_BAR,0) != 0;

        if (DEBUG) Log.v(TAG, "hasNavigationBar=" + !hide_systembar);
        if (!hide_systembar) {
            createNavigationBar(result);
        }
    } catch (Exception ex) {
        // no window manager? good luck with that
    }
    //...
}
protected void createNavigationBar(@Nullable RegisterStatusBarResult result) {
    mNavigationBarController.createNavigationBars(true /* includeDefaultDisplay */, result);
}

void createNavigationBar(Display display, Bundle savedState, RegisterStatusBarResult result) {
    //...
    NavigationBar navBar = mNavigationBarFactory.create(context);

    mNavigationBars.put(displayId, navBar);
    View navigationBarView = navBar.createView(savedState);
    //...
}
public View createView(Bundle savedState) {
    mFrame = (NavigationBarFrame) LayoutInflater.from(mContext).inflate(
            R.layout.navigation_bar_window, null);
    View barView = LayoutInflater.from(mFrame.getContext()).inflate(
            R.layout.navigation_bar, mFrame);
    barView.addOnAttachStateChangeListener(this);
    mNavigationBarView = barView.findViewById(R.id.navigation_bar_view);
    mWindowManager.addView(mFrame,
            getBarLayoutParams(mContext.getResources().getConfiguration().windowConfiguration
                    .getRotation()));
    //...
    return barView;
}
```
getBarLayoutParams看到熟悉的窗口type
```java
private WindowManager.LayoutParams getBarLayoutParamsForRotation(int rotation) {
    //对方向处理
    WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
        WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                | WindowManager.LayoutParams.FLAG_SLIPPERY,
        PixelFormat.TRANSLUCENT);
    //其他参数设置
}
```

### 下拉栏高斯模糊

frameworks/base/packages/SystemUI/src/com/android/systemui/scrim/ScrimView.java

禁止绘制，屏蔽onDraw代码即可，不然会绘制圆角背景

修改最大模糊值

frameworks/base/packages/SystemUI/res/values/dimens.xml
```xml
<dimen name="max_window_blur_radius">50px</dimen>
```

frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/BlurUtils.kt
```kotlin
fun applyBlur(viewRootImpl: ViewRootImpl?, radius: Int, opaque: Boolean) {
    //...
    createTransaction().use {
        if (supportsBlursOnWindows()) {
            //添加透明度模糊效果更佳
            it.setAlpha(viewRootImpl.surfaceControl, 0.85F)
            it.setBackgroundBlurRadius(viewRootImpl.surfaceControl, radius)
            //...
        }
        it.setOpaque(viewRootImpl.surfaceControl, opaque)
        it.apply()
    }
}
```
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBar.java

屏蔽对ScrimView显示隐藏监听
```kotlin
mScrimController.setScrimVisibleListener(scrimsVisible -> {
//   mNotificationShadeWindowController.setScrimsVisibility(scrimsVisible);
});
```
mDisplayCutout = getRootWindowInsets().getDisplayCutout()
Rect bounds = mDisplayCutout.getBoundingRectTop();

https://blog.csdn.net/hnlgzb/article/details/124837633?utm_medium=distribute.pc_relevant.none-task-blog-2~default~baidujs_baidulandingword~default-0-124837633-blog-145063188.235^v43^pc_blog_bottom_relevance_base3&spm=1001.2101.3001.4242.1&utm_relevant_index=2

http://toolman.ddnsfree.com:9192/#/main/csdnPaper

3281770邮箱  sese1234 hfc1234

https://blog.csdn.net/yang_study_first/article/details/136598168

https://blog.csdn.net/abc6368765/article/details/127967681

https://blog.csdn.net/ItJavawfc/article/details/144143621

https://blog.csdn.net/weixin_44021334/article/details/139912125

https://juejin.cn/column/6966496441099517982

https://blog.csdn.net/weixin_65101089/article/details/140323108

https://juejin.cn/user/1538972009657384/posts

https://download.csdn.net/blog/column/2120739/123222195