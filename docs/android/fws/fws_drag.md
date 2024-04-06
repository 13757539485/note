#### 项目简介
跨应用拖拽视图，比如文字、图片等元素，具体由应用A长按View触发，将View拖拽到另一个应用B并携带数据

场景：微信图片拖拽到便签、地址拖拽到地图

#### 系统拖拽API
startDragAndDrop(ClipData data, DragShadowBuilder shadowBuilder, Object myLocalState, int flags)

ClipData：用于存储传送的数据

DragShadowBuilder：拖动时展示的视图或阴影

myLocalState：这个参数可以用作Activity内部一种轻量级的数据传输机制。监听方通过DragEvent#getLocalState()方法来获取数据。它不能跨Activity，如果在其他Activity调用getLocalState()方法会返回null

flags：设置为0表示不设置flag

1. DRAG_FLAG_GLOBAL表示可以跨window拖拽，典型的是分屏状态下的拖拽
2. DRAG_FLAG_GLOBAL_PERSISTABLE_URI_PERMISSION
3. DRAG_FLAG_GLOBAL_PREFIX_URI_PERMISSION
4. DRAG_FLAG_GLOBAL_URI_READ
5. DRAG_FLAG_GLOBAL_URI_WRITE
6. DRAG_FLAG_OPAQUE
7. DRAG_FLAG_ACCESSIBILITY_ACTION

拖拽简单文本
```kotlin
val shadow = LayoutInflater.from(this).inflate(R.layout.xxx, null)

val builder = MyDragShadowBuilder(shadow)
view.setOnLongClickListener(object : OnLongClickListener {
    //长按事件触发拖拽
    override fun onLongClick(v: View?): Boolean {
        val data = ClipData.newPlainText("drag text", content)
        imageView.startDragAndDrop(
            data,
            builder, // 如果不是自定义阴影直接new View.DragShadowBuilder(view)
            null,
            0 or View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE
        )

        return true
    }
})

class MyDragShadowBuilder(private var mShadow: View) :
    View.DragShadowBuilder() {
    private val width: Int = 400
    private val height: Int = 400

    override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
        //拖动图像的宽和高
        outShadowSize?.set(width, height)

        //手指在拖动图像的位置 中点
        outShadowTouchPoint?.set(width / 2, height / 2)
    }

    override fun onDrawShadow(canvas: Canvas) {
        mShadow.measure(width, height)
        mShadow.layout(0, 0, width, height)
        mShadow.draw(canvas)
    }
}
```

拖拽处理通过setOnDragListener
```kotlin
binding.root.setOnDragListener { v, event ->
    if (event.action == DragEvent.ACTION_DROP) {
        event.clipData?.let { clipData ->
            if (clipData.description == null) return@let
            val mimeType: String = clipData.description.getMimeType(0)
            clipData.getItemAt(0)?.let {
                if (mimeType.startsWith("image/")) {
                    contentResolver.openInputStream(it.uri)?.use {input->
                        BitmapFactory.decodeStream(input)?.let { bitmap->
                            // 处理图片
                        }
                    }
                } else if (mimeType.startsWith("text/")) {
                    // 处理文本
                } else {
                    // 处理其他内容
                }
            }
        }
    }
    true
}
```

#### 实战案例
##### 关键点：触摸事件挂钩

在View源码中的dispatchTouchEvent方法挂钩子，接收触摸事件
```java
HfcDragViewHelper.getInstance().dispatchHfcTouchEvent(this, event);
```
添加长按事件
```java
private static final float MOVE_IGNORE_THRESHOLD_DP = 10.0f;
private static final long LONG_PRESS_THRESHOLD_MS = ViewConfiguration.getLongPressTimeout();
private long mDownTime;
private float mDownX;
private float mDownY;
public void dispatchHfcTouchEvent(View view, MotionEvent event) {
    final int action = event.getAction();
    switch (action) {
        case MotionEvent.ACTION_DOWN:
            // 记录按下时间和位置
            mDownTime = System.currentTimeMillis();
            mDownX = event.getX();
            mDownY = event.getY();
            break;
        case MotionEvent.ACTION_MOVE:
            // 判断是否移动距离超过长按忽略阈值
            float moveDistance = (float) Math.sqrt(Math.pow(event.getX() - mDownX, 2) + Math.pow(event.getY() - mDownY, 2));
            if (moveDistance > MOVE_IGNORE_THRESHOLD_DP * view.mContext.getResources().getDisplayMetrics().density) {
                // 移动距离过大，取消长按检测
                mDownTime = 0L;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            // 清除长按检测相关数据
            mDownTime = 0L;
            break;
    }
    // 检查是否达到长按阈值
    if (mDownTime > 0 && System.currentTimeMillis() - mDownTime >= LONG_PRESS_THRESHOLD_MS) {
        // 处理长按事件
        onLongPress(view);
        mDownTime = 0L; // 清除长按检测相关数据
    }
}

private void onLongPress(View view) {
    //拖拽逻辑
    dragView(view);
}
```
##### 关键点：拖拽视图
1. 判断应用是否设置拖拽startDragAndDrop，通过加hasDrag区分
2. 避免应用不兼容，可以设置白名单PM_WRITE_LIST
3. 对于简单如TextView和Imageview可以直接处理，其他暂时只能具体分析
```java
public void dragView(View view) {
    if (view == null) {
        Log.e(TAG, "drag view is null");
        return;
    }
    if (view.hasDrag) {
        Log.e(TAG, "already has drag view");
        return;
    }
    String packageName = view.mContext.getPackageName();
    if (!PM_WRITE_LIST.contains(packageName)) {
        Log.e(TAG, "drag pkg is not in write list");
        return;
    }
    if (view instanceof TextView) {
        CharSequence content = ((TextView) view).getText();
        drawText(view, content);
    } else if (view instanceof ImageView) {
        drawImage(view);
    } else {
        drawOther(view);
    }
}
```

##### 关键点：拖拽结束处理
在ViewRootImpl源码中挂钩子handleDragEvent
```java
private void handleDragEvent(DragEvent event) {
    // ...
    HfcDragViewHelper.getInstance().handleDragEvent(event, mBasePackageName);
    // Now dispatch the drag/drop event
    boolean result = mView.dispatchDragEvent(event);
    // ...
}
```
```java
public void handleDragEvent(DragEvent event, String basePackageName) {
    if (DragEvent.ACTION_DROP == event.mAction) {
        //判断是否是自身应用拖拽接收
        if (mCurrentDragView != null && mCurrentDragView.mContext != null && checkPkg(mCurrentDragView.mContext.getPackageName(), basePackageName)) {
            Log.e(TAG, "mClipData set null");
            event.mClipData = null;
        }
    } else if (DragEvent.ACTION_DRAG_ENDED == event.mAction) {
        if (mCurrentDragView != null) {
            showBar(mCurrentDragView.mContext, false); // 显示其他视图如dockbar
            mCurrentDragView.hasDrag = false;
            mCurrentDragView = null;
            Log.e(TAG, "hasDrag reset false");
        }
    }
}
```

源码工具类

[HfcDragViewHelper](./code/HfcDragViewHelper.java)