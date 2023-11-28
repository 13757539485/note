### UI更新相关
onCreate、onResume中可以使用子线程更新ui
```xml
<androidx.appcompat.widget.AppCompatTextView
        android:id="@+id/tvShow"
        android:layout_width="match_parent"
        android:text="xml的文字"
        android:textSize="@dimen/sp_25"
        android:gravity="center"
        android:layout_height="wrap_content" />
```
```kotlin
class ThreadCreateUIActivity : AppCompatActivity() {
    private var textView: AppCompatTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thread_create_uiactivity)
        textView = findViewById(R.id.tvShow)
        Thread {
            textView?.text = "onCreate子线程中修改"
        }.start()
        btnTest?.setOnClickListener {
            thread {
                textView?.text = "click子线程中修改"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Thread {
            textView?.text = "onResume子线程中修改"
        }.start()
    }
}
```
如果textView的layout_width是wrap_content，click刷新ui会报错

子线程中创建的View可以更新
```kotlin
//配置悬浮窗权限
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.SYSTEM_OVERLAY_WINDOW" />
//判断权限，跳转设置中打开
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if (!Settings.canDrawOverlays(this)) {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }
}

private val handlerThread = HandlerThread("thread")
//点击时调用
private fun addFloatView() {
    handlerThread.start()
    val handler = Handler(handlerThread.looper){
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.gravity = Gravity.TOP or Gravity.LEFT
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.x = 50
        layoutParams.y = 50
        manager.addView(it.obj as View, layoutParams)
        false
    }
    Thread {
        TextView(this).apply {
            text = "测试文字"
            textSize = 25.0f
            setTextColor(Color.GRAY)
            val ll = LinearLayout(this@ThreadCreateUIActivity)
            ll.addView(this, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                runOnUiThread {
                    Toast.makeText(this@ThreadCreateUIActivity, "被点击", Toast.LENGTH_LONG).show()
                }
            }
            handler.sendMessage(Message.obtain().let {
                it.obj = ll
                it
            })
        }
    }.start()
}
```
报子线程中不能更新ui的异常是在ViewRootImpl中
```java
ViewRootImpl.java
public void requestLayout() {
    if (!mHandlingLayoutInLayoutRequest) {
        checkThread();
        mLayoutRequested = true;
        scheduleTraversals();
    }
}
void checkThread() {
    if (mThread != Thread.currentThread()) {
        throw new CalledFromWrongThreadException(
                "Only the original thread that created a view hierarchy can touch its views.");
    }
}
```
根据普通[handleLaunchActivity](../android/fws/fws_app_start.md#app_start_launch)可知ViewRootImpl创建是在onResume后，所以在onCreate中不会检查线程是否是UI线程

### DecorView、Window、View、ViewRootImpl

#### Window
Android中管理View的工具，装载View的实体，activity和dialog依赖于window，window的唯一实现类是PhoneWindow，在Activity的attach方法中创建，attach方法在[handleResumeActivity](../android/fws/fws_app_start.md#app_start_resume)调用

#### DecorView

```java
AppCompatActivity中
public void setContentView(View view) {
    initViewTreeOwners();
    getDelegate().setContentView(view);
}
AppCompatDelegateImpl.java
public void setContentView(View v) {
    ensureSubDecor();
    ViewGroup contentParent = mSubDecor.findViewById(android.R.id.content);
    contentParent.removeAllViews();
    contentParent.addView(v);
    //...
}

private void ensureSubDecor() {
    if (!mSubDecorInstalled) {
        mSubDecor = createSubDecor();
    }
    //...
}
```
v是添加到contentParent中，contentParent是通过mSubDecor获取的，而mSubDecor通过createSubDecor创建
```java
private ViewGroup createSubDecor() {
    //...
    ensureWindow();
    mWindow.getDecorView();
    //...
    ViewGroup subDecor = null;
    subDecor = (ViewGroup) LayoutInflater.from(themedContext)
            .inflate(R.layout.abc_screen_toolbar, null);

    mDecorContentParent = (DecorContentParent) subDecor
            .findViewById(R.id.decor_content_parent);
    mDecorContentParent.setWindowCallback(getWindowCallback());
    //...
    mWindow.setContentView(subDecor);
    //...
    return subDecor;
}
```
subDecor最终添加到mWindow，调用PhoneWindow的setContentView
```java
public void setContentView(View view, ViewGroup.LayoutParams params) {
    if (mContentParent == null) {
        installDecor();
    } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
        mContentParent.removeAllViews();
    }
    if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
        view.setLayoutParams(params);
        final Scene newScene = new Scene(mContentParent, view);
        transitionTo(newScene);
    } else {
        mContentParent.addView(view, params);
    }
    //...
}
```
此处installDecor不会调用，而是在mWindow.getDecorView()中会调用mContentParent就不会为null
```java
private void installDecor() {
    if (mDecor == null) {
        mDecor = generateDecor(-1);
        //...
    } else {
        mDecor.setWindow(this);
    }
    if (mContentParent == null) {
        mContentParent = generateLayout(mDecor);
        //...
    }
}

protected DecorView generateDecor(int featureId) {
    //...
    return new DecorView(context, featureId, this, getAttributes());
}
protected ViewGroup generateLayout(DecorView decor) {
    //...
    //加载布局文件到mDecor
    mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
    ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
    //...
    return contentParent;
}
public <T extends View> T findViewById(@IdRes int id) {
    return getDecorView().findViewById(id);
}
```
总结：setContentView的View最终存放到mContentParent中，mContentParent是通过DecorView通过id(com.android.internal.R.id.content)获得

### ViewRootImpl
桥接View和Wms，内部属性mView就是DecorView，绑定过程看[handleResumeActivity](../android/fws/fws_app_start.md#app_start_resume)过程中WindowManagerGlobal调用addView方法

performTraversals方法中重要的测量、布局、绘制步骤(View的绘制流程)
```java
performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
performLayout(lp, mWidth, mHeight);
performDraw()
```

View的invalidate()会调用onDraw，大致流程是invalidate会逐层找parent一直到DecorView即ViewRootImpl中的mView中，然后由ViewRootImpl分发给所有View，不会调用ViewRootImpl的invalidate，而是递归调用父View的invalidateChildInParent，然后触发ViewRootImpl的performTraversals，由于mLayoutRequested为false，onMeasure和onLayout不被调用，只调用onDraw，也会调用computeScroll方法

#### invalidate和postInvalidate
postInvalidate可以在子线程中调用刷新，通过Handler切换线程最终调用invalidate

#### requesstLayout
会调用onMeasure和nLayout方法，不一定触发onDraw

#### View的滑动
1. scrollTo和scrollBy
2. 平移动画
3. layoutParams中margin值修改
4. 调用layout修改值
5. Scroller+invalidate+重写computeScroll

### 事件分发机制
1. dispatcchTouchEvent：事件分发逻辑，返回值super和直接true、false效果是不同的
2. onInterceptTouchEvent：事件拦截，viewgroup专属
3. onTouchEvent：处理事件

FrameLayout包含View1和view2

down事件：

f(dis)->f(intercept=true)->f(onTouch)

f(dis)->f(intercept=false)->v2(dis)->v2(onTouch)->v1(dis)->v1(onTouch)->f(onTouch)

f(dis=true)->f(dis)

f(dis=false)

#### 事件冲突
1. 内部拦截法

根据条件在子view中的dispatcchTouchEvent请求父容器不拦截事件
```kotlin
parent.requestDisallowInterceptTouchEvent(true)
```

2. 外部拦截法
直接在父容器的onInterceptTouchEvent根据条件拦截事件

#### onTouch、onTouchEvent、onClick
onTouch是通过setOnTouchListener回调，返回boolean

V2上设置setOnTouchListener，则调用如下

v2(dis)->onTouch(false)->v2(onTouchEvent)

v2(dis)->onTouch(true)

V2上设置setOnClickListener，则调用如下

v2(dis)->v2(onTouchEvent = false)

v2(dis)->v2(onTouchEvent = true)

v2(dis)->v2(onTouchEvent = super.onTouchEvent)->onClick

总结：
1. 设置setOnTouchListener后onClick肯定不会调用
2. onTouchEvent调用由onTouch的返回值决定，false调用
3. onClick调用由onTouchEvent的返回值决定，调用super.onTouchEvent且是在UP事件后

设计模式：[责任链模式](./design_mode.md#chain)

