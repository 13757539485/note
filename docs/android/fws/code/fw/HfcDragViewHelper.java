package android.view;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.R;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.text.View;

/**
 * @hide
 */
public class HfcDragViewHelper {
    private static final String TAG = "CariadDragHelper";
    private static final String TAG_DRAW = "DragDraw";
    private static final String TAG_TOUCH = "DragTouch";
    private static final String TAG_FIND_VIEW = "DragFindView";
    private static final float MOVE_IGNORE_THRESHOLD_DP = 10.0f;
    private static final long LONG_PRESS_THRESHOLD_MS = ViewConfiguration.getLongPressTimeout() + 100L;

    private static final ArrayList<String> PM_WRITE_LIST = new ArrayList<>();
    private static final ArrayList<String> EXCLUDE_PKG_LIST = new ArrayList<>();
    // private View mCurrentDragView;
    private String mCurrentDragDropPkg;
    private long mDownTime;
    private float mDownX;
    private float mDownY;

    private final Handler mMainHandler;
    private final Handler mThreadHandler;

    private final Rect rect = new Rect();
    private final int[] parentLocation = new int[2];
    private final int[] childLocation = new int[2];

    private static final HfcDragViewHelper instance = new HfcDragViewHelper();

    private HfcDragViewHelper() {
        EXCLUDE_PKG_LIST.add("com.android.systemui");
        EXCLUDE_PKG_LIST.add("com.android.settings");
        EXCLUDE_PKG_LIST.add("com.android.launcher3");
        PM_WRITE_LIST.add("com.tencent.mm"); // 微信
        PM_WRITE_LIST.add("com.autonavi.minimap");// 高德地图手机版
        PM_WRITE_LIST.add("com.autonavi.amapauto");// 高德地图车机版
        PM_WRITE_LIST.add("com.sankuai.meituan");  // 美团
        PM_WRITE_LIST.add("com.dianping.v1");  // 大众点评
        PM_WRITE_LIST.add("com.mt.mtxx.mtxx");  // 美图秀秀
        PM_WRITE_LIST.add("com.example.drop1"); // 测试应用
        HandlerThread imageThread = new HandlerThread("thread-image");
        imageThread.start();
        mThreadHandler = new Handler(imageThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public static HfcDragViewHelper getInstance() {
        return instance;
    }

    public void dispatchTouchEvent(View view, MotionEvent event) {
        if (view == null) {
            Log.e(TAG_TOUCH, "dispatchTouchEvent view is null");
            return;
        }
        if (mCurrentDragDropPkg != null) {
            Log.e(TAG_TOUCH, "already has drag view");
            return;
        }
        if (event == null) {
            Log.e(TAG_TOUCH, "dispatchTouchEvent event is null");
            return;
        }
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 记录按下时间和位置
                view.getLocationInWindow(parentLocation);
                mDownTime = System.currentTimeMillis();
                mDownX = event.getX();
                mDownY = event.getY() + parentLocation[1];
                Log.d(TAG_TOUCH, "down mDownX=" + mDownX + "," + mDownY + ",lx=" +
                        parentLocation[0]+ ",ly=" + parentLocation[1]);
                getTouchScaleAnim(view, true);
                break;
            case MotionEvent.ACTION_MOVE:
                // 判断是否移动距离超过长按忽略阈值
                float moveDistance = (float) Math.sqrt(Math.pow(event.getX() - mDownX, 2) + Math.pow(event.getY() - mDownY, 2));
                if (moveDistance > MOVE_IGNORE_THRESHOLD_DP) {
                    // 移动距离过大，取消长按检测
                    mDownTime = 0L;
                    Log.d(TAG_TOUCH, "move mDownTime=" + mDownTime);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 清除长按检测相关数据
                mDownTime = 0L;
                mCurrentDragDropPkg = null;
                Log.d(TAG_TOUCH, "cancel mDownTime=" + mDownTime);
                getTouchScaleAnim(view, false);
                break;
        }
        Log.d(TAG_TOUCH, "dispatchTouchEvent mDownTime=" + mDownTime + ",action=" + action + ",view=" + view);
        // 检查是否达到长按阈值
        if (mDownTime > 0 && System.currentTimeMillis() - mDownTime >= LONG_PRESS_THRESHOLD_MS) {
            // 处理长按事件
            Log.d(TAG_TOUCH, "touch point(" + mDownX + "," + mDownY + ")");
            onLongPress(view);
            mDownTime = 0L; // 清除长按检测相关数据
            mCurrentDragDropPkg = null;
        }
    }

    private void onLongPress(View view) {
        Log.e(TAG_TOUCH, "onLongPress: " + view + "," + view.mContext.getPackageName());
        dragView(view);
    }

    /**
     * 添加全局按压动画，结合View添加属性控制(搜索mEnablePressedAnim)
     */
    private void getTouchScaleAnim(View view, boolean isDown) {
        if (!view.mEnablePressedAnim) return;
        view.clearAnimation();
        float from = isDown ? 1.0f : 0.8f;
        float to = isDown ? 0.8f : 1.0f;
        ScaleAnimation scale = new ScaleAnimation(from, to, from, to,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(200);
//        scale.setInterpolator(new CariadSpringInterpolator(0.5, 5.0));
        scale.setFillAfter(true);
        view.startAnimation(scale);
    }

    public void preHandleDragEvent(DragEvent event, String basePackageName) {
        if (DragEvent.ACTION_DROP == event.mAction) {
            if (mCurrentDragDropPkg != null && mCurrentDragDropPkg.equals(basePackageName)) {
                event.mClipData = null;
                Log.d(TAG, "drag clear data by self pkg=" + basePackageName);
            }
        }
    }

    public boolean handleDragEvent(DragEvent event, String basePackageName,
                                   boolean oriResult, IWindowSession windowSession,
                                   ViewRootImpl.W window) {
        if (event == null) {
            Log.e(TAG, "handleDragEvent event is null");
            return false;
        }
        boolean result = false;
        if (DragEvent.ACTION_DROP == event.mAction) {
            //判断是否是自身应用拖拽接收
            try {
                result = windowSession.notifyDropStatus(oriResult, mCurrentDragDropPkg == null,
                        window, event, basePackageName);
                if (mCurrentDragDropPkg != null && mCurrentDragDropPkg.equals(basePackageName)) {
                    result = false;
                }
                if (!oriResult && result) {
                    sendAllowDragApp(windowSession, event.mClipData, basePackageName);
                    Log.e(TAG, "replace result to custom result:" + basePackageName);
                }
                Log.e(TAG, "action drop result:" + result);
            } catch (Exception e) {
                Slog.e(TAG, "Unable to note trigShareOrShopIfNeed");
            }
        } else if (DragEvent.ACTION_DRAG_LOCATION == event.mAction) {
            // +图标: oriResult=true or share action size = 1
            // share图标: share action size > 1
            // 不支持图标：
            try {
                windowSession.notifyDropStatus(oriResult, mCurrentDragDropPkg == null,
                        window, event, basePackageName);
            } catch (Exception e) {
                Slog.e(TAG, "Unable to note VirtualDropResult");
            }
        } else if (DragEvent.ACTION_DRAG_ENDED == event.mAction) {
            dragEnd(event.mClipData, windowSession);
        }
        return result;
    }

    /**
     * 传递自定义可拖拽的应用的拖拽数据
     */
    private void sendAllowDragApp(IWindowSession session, ClipData clipData, String pkg) {
        try {
            session.shareBarShowOrHide("action.sharebar.custom.pkg_drag", pkg, clipData);
        } catch (RemoteException e) {
            Log.e(TAG, "sendAllowDragApp: " + e.getMessage());
        }
    }

    public void dragStart(IWindowSession session, Context context, ClipData data) {
        showBar(context, session, true, data);
    }

    public void dragEnd(ClipData clipData, IWindowSession windowSession){
        Log.e(TAG, "dragEnd: " + mCurrentDragDropPkg);
        showBar(mCurrentDragDropPkg, windowSession, false, clipData);
        if (mCurrentDragDropPkg != null) {
            mCurrentDragDropPkg = null;
            Log.e(TAG, "Drag reset");
        }
    }

    public boolean hasDrag(View view) {
        String pkg = view.getContext().getPackageName();
        if (mCurrentDragDropPkg == null) {
            mCurrentDragDropPkg = pkg;
            return false;
        }
        return true;
    }

    private void showBar(Context context, IWindowSession session, boolean show, ClipData data) {
        if (context == null || session == null) {
            Log.e(TAG, "showBar context or session is null");
            return;
        }
        Log.e(TAG, "showBar: " + show + "," + context.getPackageName());
        mMainHandler.postDelayed(() -> {
            try {
                session.shareBarShowOrHide(show ? "action.sharebar.show" : "action.sharebar.hide",
                        context.getPackageName(), data);
            } catch (RemoteException e) {
                Log.e(TAG, "showBar: " + e.getMessage());
            }
        }, 200);
    }

    private Intent getShareBarIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.cariad.m2.car_link_launcher",
                "com.cariad.m2.sharebar.core.ShareReceiver"));
        return intent;
    }

    private void dragView(View view) {
        if (view == null) {
            Log.e(TAG, "drag view is null");
            return;
        }
        if (mCurrentDragDropPkg != null) {
            Log.e(TAG_TOUCH, "already has drag view");
            return;
        }
        String packageName = view.mContext.getPackageName();
        if (EXCLUDE_PKG_LIST.contains(packageName)) {
            Log.e(TAG, "current drag package is in exclude list");
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

    private void drawOther(View view) {
        try {
            adapterApps(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adapterApps(View view) {
        String packageName = view.mContext.getPackageName();
        if ("com.tencent.mm".equals(packageName)) {
            adapterWx(view);
        } else if ("com.cariad.m2.app.gallery".equals(packageName)) {
            adapterAlbum(view);
        } else if ("com.sankuai.meituan".equals(packageName)) {
            adapterMeituan(view);
        } else {
            iterateView(view);
        }
    }

    private boolean findTargetViewInViewGroup(ViewGroup group) {
        Log.d(TAG_FIND_VIEW, "findTargetView group=" + group + ",child count=" + group.getChildCount());
        // 获取group的屏幕位置，判断触摸点是否在group的区域
        group.getLocationInWindow(childLocation);
        rect.set(childLocation[0], childLocation[1], childLocation[0] + group.getWidth(),
                childLocation[1] + group.getHeight());

        // 如果触摸点不在group内，直接返回false，避免不必要的递归
        if (!rect.contains((int) mDownX, (int) mDownY)) {
            Log.d(TAG_FIND_VIEW, "findTargetView not in group");
            return false;
        }

        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);

            // 如果子view是ViewGroup，递归查找
            if (child instanceof ViewGroup) {
                boolean result = findTargetViewInViewGroup((ViewGroup) child);
                if (result) {
                    Log.d(TAG_FIND_VIEW, "recursive find");
                    return true;
                }
            } else {
                // 获取子view的位置并检查触摸点是否在子view内
                child.getLocationInWindow(childLocation);
                rect.set(childLocation[0], childLocation[1], childLocation[0] + child.getWidth(), childLocation[1] + child.getHeight());
                Log.d(TAG_FIND_VIEW, "rect=" + rect +
                        ",childLocation:(" + childLocation[0] + "," + childLocation[1] + ")" +
                        ",parentLocation:(" + parentLocation[0] + "," + parentLocation[1] + ")");
                // 比较触摸点是否在子视图的范围内
                if (rect.contains((int) mDownX, (int) mDownY)) {
                    handleTouchedChild(child);
                    return true;
                }
            }
        }
        return false;
    }

    private void handleTouchedChild(View child) {
        Log.d(TAG_FIND_VIEW, "Touched child view ID: " + child);
        dealView(child);
    }

    private void dealView(View view) {
        if (view instanceof TextView) {
            CharSequence content = ((TextView) view).getText();
            drawText(view, content);
        } else if (view instanceof ImageView) {
            drawImage(view);
        } else {
            Log.d(TAG_FIND_VIEW, "need extra deal: " + view);
        }
    }

    private void iterateView(View view) {
        ViewGroup rootContent = view.findViewById(android.R.id.content);
        if (rootContent != null) {
            Log.e(TAG_FIND_VIEW, "iterate from ContentView" + view);
            findTargetViewInViewGroup(rootContent);
        } else {
            if (view instanceof ViewGroup) {
                Log.e(TAG_FIND_VIEW, "iterate from root view" + view);
                findTargetViewInViewGroup((ViewGroup) view);
            }
        }
    }

    private IWindowSession getWindowSession(View view) {
        return view.getViewRootImpl().mWindowSession;
    }

    private final int MAX_VALUE = 400;
    private void drawBitmap(View view, Bitmap bitmap) {
        mThreadHandler.post(() -> {
            try {
                ParcelFileDescriptor bitmapPfd = getBitmapPfd(bitmap);
                getWindowSession(view).savaBitmap(bitmapPfd, new RemoteCallback(result -> {
                    if (result != null) {
                        Uri uri = result.getParcelable("drop_uri");
                        Log.e(TAG_DRAW, "drawBitmap uri: " + uri);
                        ClipData.Item item = new ClipData.Item(uri);
                        ClipData data = new ClipData("image drag",
                                new String[]{"image/jpeg"}, item);
                        int oriWidth = view.getWidth();
                        int oriHeight = view.getHeight();
                        if (oriWidth > oriHeight) {
                            if (oriWidth > MAX_VALUE) {
                                oriHeight = oriHeight * MAX_VALUE / oriWidth;
                                oriWidth = MAX_VALUE;
                            }
                        } else {
                            if (oriHeight > MAX_VALUE) {
                                oriWidth = oriWidth * MAX_VALUE / oriHeight;
                                oriHeight = MAX_VALUE;
                            }
                        }
                        ImageView tempView = new ImageView(view.mContext);
                        ViewGroup.LayoutParams imageViewLayoutParams = new ViewGroup.LayoutParams(
                                oriWidth,
                                oriHeight
                        );
                        tempView.setLayoutParams(imageViewLayoutParams);
                        tempView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        tempView.setImageBitmap(bitmap);
                        view.startDragAndDrop(data,
                                new ImageShadow(tempView, oriWidth, oriHeight), null,
                                View.DRAG_FLAG_GLOBAL);
                    }
                }, mMainHandler));
            } catch (Exception e) {
                Log.e(TAG_DRAW, "drawBitmap error: " + e.getMessage());
            }
        });
    }

    private ParcelFileDescriptor getBitmapPfd(Bitmap bitmap) {
        ParcelFileDescriptor pfd = null;

        //将Bitmap转成字节数组
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] byteArray = stream.toByteArray();
        Log.e(TAG_DRAW, "getBitmapPfd: " + byteArray.length + ",w=" +
                bitmap.getWidth() + ",h=" + bitmap.getHeight());
        try {
            MemoryFile memoryFile = new MemoryFile("dragTemp", bitmap.getByteCount());
//            Method method = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
//            FileDescriptor des = (FileDescriptor) method.invoke(memoryFile);
            FileDescriptor des = memoryFile.getFileDescriptor();
            pfd = ParcelFileDescriptor.dup(des);
            //向内存中写入字节数组
            memoryFile.getOutputStream().write(byteArray);
            //关闭流
            memoryFile.getOutputStream().close();
            memoryFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG_DRAW, "getPfd() pfd:" + pfd);
        return pfd;
    }

    private void drawImage(View view) {
        ImageView iv = (ImageView) view;
        Log.e(TAG_DRAW, "ImageView: " + view);
        Drawable drawable = iv.getDrawable();
        Log.e(TAG_DRAW, "drawable: " + drawable);
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = tryExtract(iv);
        }
        if (bitmap != null) {
            drawBitmap(view, bitmap);
        }
    }

    private Bitmap tryExtract(View view) {
        Bitmap bitmap = null;
        try {
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            if (view.getDrawingCache() != null) {
                bitmap = Bitmap.createBitmap(view.getDrawingCache());
            }
            view.setDrawingCacheEnabled(false);
        } catch (Exception e) {
            Log.e(TAG, "createBitmap second error: " + e.getMessage());
        }

        if (bitmap == null) {
            try {
                bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                        Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                view.draw(canvas);
            } catch (Exception e) {
                Log.e(TAG, "createBitmap once error: " + e.getMessage());
            }
        }
        return bitmap;
    }

    private void drawText(View view, CharSequence content) {
        String realText = content.toString();
        TextView textView = new TextView(view.mContext);
        textView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));
        textView.setPadding(21, 0, 21, 0);
        textView.setGravity(Gravity.CENTER);
        textView.setMaxWidth(256);
        textView.setMaxLines(1);
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textView.setTextSize(21);
        textView.setText(realText);
        textView.setTextColor(0xFF412FD7);

        Paint paint = new Paint();
        paint.setTextSize(textView.getTextSize());
        Rect bounds = new Rect();
        paint.getTextBounds(realText, 0, realText.length(), bounds);

        FrameLayout vp = new FrameLayout(view.mContext);
        vp.setBackgroundResource(R.drawable.drag_text_bg);
        int textViewWidth = Math.min(bounds.width() + 42, 256);
        vp.setLayoutParams(new FrameLayout.LayoutParams(textViewWidth, TextShadow.HEIGHT, Gravity.CENTER));
        vp.addView(textView);
        view.startDragAndDrop(ClipData.newPlainText("drag text", realText),
                new TextShadow(vp, textViewWidth), null, View.DRAG_FLAG_GLOBAL);
    }

    private boolean checkIdName(View view, String expect) {
        if (view == null || expect == null) {
            return false;
        }
        int id = view.getId();
        String target = view.getResources().getResourceEntryName(id);
        return expect.equals(target);
    }

    private boolean checkViewName(View view, String expect) {
        if (view == null || expect == null) {
            return false;
        }
        return expect.equals(view.getClass().getName());
    }

    private boolean checkPkg(String ori, String exp) {
        return ori != null && ori.equals(exp);
    }

    private void adapterWx(View view) {
        Log.e(TAG_FIND_VIEW, "adapterWx start: " + view);
        if (checkIdName(view, "bkg") && view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            Log.e(TAG_FIND_VIEW, "adapterWx bkg:" + vg + "," + vg.getChildCount());
            if (vg.getChildCount() > 0) {
                View childAt = vg.getChildAt(0);
                Log.e(TAG_FIND_VIEW, "adapterWx count:" + childAt);
                if (childAt instanceof ViewGroup) {
                    // 微信聊天界面地址
                    Log.e(TAG_FIND_VIEW, "adapterWx childAt group: " + childAt);
                    View vgf = ((ViewGroup) childAt).getChildAt(0);
                    Log.e(TAG_FIND_VIEW, "adapterWx vgl: " + vgf);
                    if (vgf instanceof ViewGroup) {
                        View vgl = ((ViewGroup) vgf).getChildAt(0);
                        Log.e(TAG_FIND_VIEW, "adapterWx vgf: " + vgf);
                        if (vgl instanceof ViewGroup) {
                            if (((ViewGroup) vgl).getChildCount() > 1) {
                                StringBuilder address = new StringBuilder();
                                View vglv1 = ((ViewGroup) vgl).getChildAt(0);
                                Log.e(TAG_FIND_VIEW, "adapterWx vgflv1: " + vglv1);
                                if (checkIdName(vglv1, "bp8") && vglv1 instanceof TextView) {
                                    address.append(((TextView) vglv1).getText());
                                }
                                View vglv2 = ((ViewGroup) vgl).getChildAt(1);
                                Log.e(TAG_FIND_VIEW, "adapterWx vgflv2: " + vglv2);
                                if (checkIdName(vglv2, "bp6") && vglv2 instanceof TextView) {
                                    address.append(((TextView) vglv2).getText());
                                }
                                Log.e(TAG_FIND_VIEW, "adapterWx address: " + address);
                                view.startDragAndDrop(ClipData.newPlainText("drag text", address),
                                        new View.DragShadowBuilder(view), null, View.DRAG_FLAG_GLOBAL);
//                                drawText(view, address);
                                return;
                            }
                        }
                    }
                } else {
                    Log.e(TAG_FIND_VIEW, "adapterWx childAt: " + childAt);
                    // 微信聊天界面图片
                    if (checkIdName(childAt, "bkm") && childAt instanceof ImageView) {
                        drawImage(childAt);
                    }
                }
            }
        } else if (checkViewName(view, "com.tencent.mm.ui.widget.MMNeat7extView")
                && checkIdName(view, "bkl")) {
            // 表情和文本
            try {
                Class<?> clazz = view.getClass();
                Log.e(TAG_FIND_VIEW, "adapterWx clazz: " + clazz);
                Field xField = clazz.getField("x");
//                Field zField = clazz.getField("z");
                CharSequence xValue = (CharSequence) xField.get(view);
                drawText(view, xValue);
//                CharSequence zValue = (CharSequence) zField.get(view);
               /* Field textViewField = clazz.getField("f");
                TextView textView = (TextView) textViewField.get(view);
                if (textView != null) {
                    CharSequence textContent = textView.getText();
                    printLog("", "TextView的text内容: " + textContent);
                }*/
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (checkViewName(view, "com.tencent.mm.pluginsdk.ui.emoji.RTChattingEmojiViewFrom")
                && checkIdName(view, "bkm") && view instanceof ViewGroup) {
            // 动图
            ViewGroup vg = (ViewGroup) view;
            if (vg.getChildCount() > 0) {
                View childAt = vg.getChildAt(2);
                if (checkViewName(childAt, "com.tencent.mm.pluginsdk.ui.emoji.ChattingEmojiView")
                        && childAt instanceof ImageView) {
                    Drawable drawable = ((ImageView) childAt).getDrawable();
                    Class<?> clazz = drawable.getClass();
                    Log.e(TAG_FIND_VIEW, "adapterWx childAt---drawable: " + drawable);
                    if ("com.tencent.mm.plugin.gif.u".equals(clazz.getName())) {
                        try {
                            Field sField = clazz.getField("s");
                            Bitmap bitmap = (Bitmap) sField.get(drawable);
                            Field rField = clazz.getField("r");
                            Bitmap[] bitmaps = (Bitmap[]) rField.get(drawable);
                            Log.e(TAG_FIND_VIEW,
                                    "adapterWx childAt---bitmap: " + bitmap + "," + bitmaps.length);
                            drawBitmap(view, bitmap);
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if ((checkViewName(view, "com.tencent.mm.pluginsdk.ui.emoji.RTChattingEmojiViewTo"))
                && checkIdName(view, "bkm") && view instanceof ViewGroup) {
            // 动图
            ViewGroup vg = (ViewGroup) view;
            if (vg.getChildCount() > 0) {
                View childAt = vg.getChildAt(2);
                if (checkViewName(childAt, "com.tencent.mm.pluginsdk.ui.emoji.ChattingEmojiView")
                        && childAt instanceof ImageView) {
                    Drawable drawable = ((ImageView) childAt).getDrawable();
                    Class<?> clazz = drawable.getClass();
                    if ("com.tencent.mm.plugin.gif.n".equals(clazz.getName())) {
                        try {
                            Field iField = clazz.getField("i");
                            Bitmap bitmap = (Bitmap) iField.get(drawable);
                            drawBitmap(view, bitmap);
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        iterateView(view);
    }

    private void adapterAlbum(View view) {
        Log.e(TAG_FIND_VIEW, "adapterAlbum：" + view);
        if (view instanceof ViewGroup) {
            ViewGroup vp = (ViewGroup) view;
            Log.e(TAG_FIND_VIEW, "adapterAlbum vp：" + vp);
            if (vp.getChildCount() > 0) {
                View child = vp.getChildAt(0);
                Log.e(TAG_FIND_VIEW, "adapterAlbum child：" + child);
                if (child instanceof ImageView) {
                    drawImage(child);
                    return;
                }
            }
        }
        iterateView(view);
    }

    private void adapterMeituan(View view) {
        Log.e(TAG_FIND_VIEW, "adapterMeituan start: " + view);
        if (checkViewName(view, "com.facebook.react.views.view.f") && view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            Log.e(TAG_FIND_VIEW, "adapterMeituan bkg:" + vg + "," + vg.getChildCount());
            if (vg.getChildCount() > 3) {
                View child = vg.getChildAt(3);
                if (child instanceof TextView) {
                    CharSequence text = ((TextView) child).getText();
                    drawText(child, text);
                    Log.e(TAG_FIND_VIEW, "adapterMeituan text: " + text);
                    return;
                }
            }
        }
        iterateView(view);
    }

    private static class ImageShadow extends View.DragShadowBuilder {
        private final View mShadow;
        private final int width;
        private final int height;
        public ImageShadow(View view, int width, int height) {
            super(view);
            mShadow = view;
            this.width = width;
            this.height = height;
        }

        @Override
        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            outShadowSize.set(width, height);
            outShadowTouchPoint.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            Path clipPath = new Path();
            clipPath.addRoundRect(0, 0, width, height, 13, 13, Path.Direction.CW);
            canvas.clipPath(clipPath);
            mShadow.measure(width, height);
            mShadow.layout(0, 0, width, height);
            mShadow.draw(canvas);
        }
    }

    private static class TextShadow extends View.DragShadowBuilder {
        private final View mShadow;
        private final int width;

        public static final int HEIGHT = 66;
        public TextShadow(View view, int width) {
            super(view);
            mShadow = view;
            this.width = width;
        }

        @Override
        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            outShadowSize.set(width, HEIGHT);
            outShadowTouchPoint.set(width / 2, HEIGHT / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            mShadow.measure(width, HEIGHT);
            mShadow.layout(0, 0, width, HEIGHT);
            mShadow.draw(canvas);
        }
    }

    public static void printLog(String className, String content) {
        Log.e(TAG, className + "--printLog: " + content);
    }
}
