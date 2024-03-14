### 项目简介
跨应用拖拽视图，比如文字、图片等元素，具体由应用A长按View触发，将View拖拽到另一个应用B并携带数据

场景：微信图片拖拽到便签、地址拖拽到地图

#### 知识储备
##### Android的触摸事件传递
1. 硬件层：用户手指接触屏幕时，触摸传感器检测到触摸点的位置、压力等信，并将其转化为原始的硬件事件

2. Linux内核：这些原始硬件事件被传递到Linux内核的输入子系统，经过初步处理后，通过/dev/input/eventX设备节点暴露给用户空间

3. InputManagerService：Android系统的System Server进程中的InputManagerService作为中介，监听这些设备节点，并通过Binder机制将触摸事件发送给需要处理的客户端，通常是ActivityManagerService

4. ActivityManagerService：接收到触摸事件后，ActivityManagerService负责确定哪个应用程序（App）应该接收这个事件。它根据当前的焦点（focus）和窗口层级结构，找到最上层且可交互的窗口（Window），进而确定对应的Activity

5. WindowManagerService：ActivityManagerService将触摸事件转发给WindowManagerService，后者负责管理所有窗口的布局和事件分发。WindowManagerService找到与触摸事件相关的顶层窗口，并通知该窗口的Window.Callback对象（通常是Activity的内部类）

6. DecorView：顶层窗口通常是一个DecorView，它是PhoneWindow的一个内部类，继承自FrameLayout。DecorView作为顶级View，接收到触摸事件后，开始按照Android的事件分发机制将事件向下传递

7. ViewRootImpl：DecorView所在的ViewRootImpl负责将触摸事件进一步分发给View层次结构。ViewRootImpl实现了ViewParent接口，其中包含了处理触摸事件的方法，如dispatchTouchEvent()

8. ViewGroup & View：触摸事件最终从ViewRootImpl逐级向下分发到具体的ViewGroup和View对象。事件首先传递到ViewGroup.dispatchTouchEvent()，在这里可能会触发onInterceptTouchEvent()方法决定是否截断事件。如果事件没有被截断，将继续传递给子View的dispatchTouchEvent()，并在适当的时候调用onTouchEvent()。这个过程沿着View树一直向下传播，直到某个View消费了事件或者事件被完全传递到树的底层
##### 系统拖拽api使用和源码理解
[源码分析](./fws_drag.md)

##### Transaction使用结合动画框架

### 实战案例
#### 关键点：触摸事件挂钩

frameworks/base/core/java/android/view/View.java

在View源码中的dispatchTouchEvent方法挂钩子，接收触摸事件
```java
public boolean dispatchTouchEvent(MotionEvent event) {
    HfcDragViewHelper.getInstance().dispatchHfcTouchEvent(this, event);
    //...
}
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
#### 关键点：拖拽视图
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
frameworks/base/core/java/android/view/View.java

其中hasdrag标志位在startDragAndDrop方法中
```java
/**
 * @hide
 */
protected boolean hasDrag;

public final boolean startDragAndDrop(ClipData data, DragShadowBuilder shadowBuilder,
            Object myLocalState, int flags) {
    //...
    if (data != null) {
        data.prepareToLeaveProcess((flags & View.DRAG_FLAG_GLOBAL) != 0);
    }
    // add start
    if (HfcDragViewHelper.getInstance().hasDrag(this)) {
        hasDrag = true;
        HfcDragViewHelper.getInstance().dragStart(mContext, data);
    }
    //...
```

#### 关键点：拖拽结束处理

frameworks/base/core/java/android/view/ViewRootImpl.java

在ViewRootImpl源码中挂钩子handleDragEvent
```java
private void handleDragEvent(DragEvent event) {
    // ...
    boolean customResult = HfcDragViewHelper.getInstance().handleDragEvent(event, mBasePackageName);
    // Now dispatch the drag/drop event
    boolean result = mView.dispatchDragEvent(event);
    if (customResult) {
        // 不执行拖拽返回动画，应用假装支持拖拽接收
        result = true;
        CariadDragHelper.getInstance().sendAllowDragApp(mContext, event.mClipData, mBasePackageName);
        Log.e("HfcDragViewHelper", "replace result to custom result:" + mBasePackageName);
    }

    // ...
}
```
用来处理拖拽结束逻辑
```java
public boolean handleDragEvent(DragEvent event, String basePackageName) {
    if (event == null) {
        Log.e(TAG, "handleDragEvent event is null");
        return false;
    }
    boolean result = false;
    if (DragEvent.ACTION_DROP == event.mAction) {
        if (mCurrentDragView != null && mCurrentDragView.mContext != null &&
                checkPkg(mCurrentDragView.mContext.getPackageName(), basePackageName)) {
            Log.e(TAG, "mClipData set null");
            if (!"com.hfc.manager".equals(basePackageName)) {
                event.mClipData = null;//是否允许自身应用拖拽接收
            } else {
                result = true;
            }
        } else {
            // 高德和美图接收拖拽
            if ("com.mt.mtxx.mtxx".equals(basePackageName) ||
                    "com.autonavi.minimap".equals(basePackageName)) {
                Log.e(TAG, "simulate drag and drop:" + mCurrentDragView);
                result = true;
            }
        }
    } else if (DragEvent.ACTION_DRAG_ENDED == event.mAction) {
        if (mCurrentDragView != null) {
            Log.e(TAG, "hasDrag reset false: " + mCurrentDragView);
            showBar(mCurrentDragView.mContext, false, event.mClipData);
            mCurrentDragView.hasDrag = false;
            mCurrentDragView = null;
        }
    }
    return result;
}
```

源码工具类

[HfcDragViewHelper](./code/HfcDragViewHelper.java)

#### 关键点：拖拽动画、大小

frameworks/base/services/core/java/com/android/server/wm/DragDropController.java

```java
IBinder performDrag(int callerPid, int callerUid, IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
    //...
    final SurfaceControl.Transaction transaction = mDragState.mTransaction;
    transaction.setAlpha(surfaceControl, 1); //修改成1
    transaction.show(surfaceControl);
    displayContent.reparentToOverlay(transaction, surfaceControl);
    mDragState.updateDragSurfaceLocked(true, touchX, touchY);
    mDragState.startDragLocked(); //启动动画
    //...
}
```

frameworks/base/services/core/java/com/android/server/wm/DragState.java

执行拖拽居中缩放动画以及alpha动画
```java
void startDragLocked() {
    mAnimator = createShowAnimationLocked();
}

private float mScale = 1.0f;

private ValueAnimator createShowAnimationLocked() {
    int maxValue = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200,
            mService.mContext.getResources().getDisplayMetrics());
    int width = mSurfaceControl.getWidth();
    int height = mSurfaceControl.getHeight();
    if (width > height) {
        mScale = width > maxValue ? maxValue * 1.0f / width : 1.0f;
    } else {
        mScale = height > maxValue ? maxValue * 1.0f / height : 1.0f;
    }

    Matrix matrix = new Matrix();
    float[] matrixValues = new float[9];
    final ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(
            PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, 1, 1.2f, mScale),
            PropertyValuesHolder.ofFloat(
                    ANIMATED_PROPERTY_ALPHA, 1, mOriginalAlpha));
    animator.addUpdateListener(animation -> {
        try (SurfaceControl.Transaction transaction =
                        mService.mTransactionFactory.get()) {
            transaction.setAlpha(
                    mSurfaceControl,
                    (float) animation.getAnimatedValue(ANIMATED_PROPERTY_ALPHA));
            float tmpScale = (float) animation.getAnimatedValue(ANIMATED_PROPERTY_SCALE);
            float scaleCenterX = mCurrentX;
            float scaleCenterY = mCurrentY;
            matrix.setScale(tmpScale, tmpScale, scaleCenterX, scaleCenterY);
            matrix.postTranslate((scaleCenterX - (float) width / 2) * tmpScale,
                    (scaleCenterY - (float) height / 2) * tmpScale);
            transaction.setMatrix(mSurfaceControl, matrix, matrixValues);
            transaction.apply();
        }
    });
    animator.setDuration(200);
    animator.setInterpolator(mCubicEaseOutInterpolator);
    animator.addListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {}

        @Override
        public void onAnimationCancel(Animator animator) {}

        @Override
        public void onAnimationRepeat(Animator animator) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            mAnimator = null;
        }
    });

    mService.mAnimationHandler.post(() -> animator.start());
    return animator;
}
```
修改拖动时视图不居中问题
```java
void updateDragSurfaceLocked(boolean keepHandling, float x, float y) {
    if (mAnimator != null) {
        return;
    }
    //...
    float width = mScale == 1.0f ? mThumbOffsetX : mSurfaceControl.getWidth() * mScale / 2;
    float height = mScale == 1.0f ? mThumbOffsetY : mSurfaceControl.getHeight() * mScale / 2;
    mTransaction.setPosition(mSurfaceControl, x - width,
            y - height).apply();
    //...
}
```
修改拖拽取消/返回动画
```java
private ValueAnimator createReturnAnimationLocked() {
    //...
    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, mScale, 1),
    //...
}

private ValueAnimator createCancelAnimationLocked() {
    //...
    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, mScale, 0),
    //...
}
```