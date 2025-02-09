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

mDisplayCutout = getRootWindowInsets().getDisplayCutout()
Rect bounds = mDisplayCutout.getBoundingRectTop();