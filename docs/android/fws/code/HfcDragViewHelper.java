package android.view;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.text.View;

/**
 * @hide
 */
public class HfcDragViewHelper {
    private static final String TAG = "HfcDragViewHelper";

    private static final ArrayList<String> PM_WRITE_LIST = new ArrayList<>();

    private View mCurrentDragView;

    private final Handler mMainHandler;
    private final Handler mThreadHandler;

    private static final float MOVE_IGNORE_THRESHOLD_DP = 10.0f;
    private static final long LONG_PRESS_THRESHOLD_MS = ViewConfiguration.getLongPressTimeout() + 50L;
    private long mDownTime;
    private float mDownX;
    private float mDownY;

    private HfcDragViewHelper() {
        PM_WRITE_LIST.add("com.tencent.mm");
        PM_WRITE_LIST.add("com.example.testenvir");
        PM_WRITE_LIST.add("com.dianping.v1");
        HandlerThread imageThread = new HandlerThread("thread-image");
        imageThread.start();
        mThreadHandler = new Handler(imageThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    private static class Holder {
        private static final HfcDragViewHelper instance = new HfcDragViewHelper();
    }

    public static HfcDragViewHelper getInstance() {
        return Holder.instance;
    }

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
        Log.e(TAG, "onLongPress: " + view +"," + view.mContext.getPackageName()  + "," +
                view.getAccessibilityClassName());
        dragView(view);
    }

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

    private void drawOther(View view) {
        String packageName = view.mContext.getPackageName();
        try {
            if ("com.tencent.mm".equals(packageName)) {
                adapterWx(view);
            } else if ("com.sankuai.meituan".equals(packageName)) {
                adapterMeituan(view);
            } else {
                Log.e(TAG, "drawOther not support type: " + view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawBitmap(View view, Bitmap bitmap) {
        if (bitmap != null) {
            File dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File saveDir = new File(dir, "drag-drop");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }
            mThreadHandler.post(() -> {
                try {
                    File outputFile = new File(saveDir, "bitmap-" + view.mContext.getPackageName() + ".png");
                    if (outputFile.exists()) {
                        boolean file = outputFile.delete();
                        Log.e(TAG, "delete file: " + file);
                    }
                    OutputStream os = new FileOutputStream(outputFile, false);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                    os.close();
                    String[] mimeTypes = {"image/png"};
                    if (outputFile.exists()) {
                        MediaScannerConnection.scanFile(view.mContext,
                        new String[]{outputFile.getPath()},
                        mimeTypes,
                        (path, uri) -> {
                            mMainHandler.post(() -> {
                                ClipData.Item item = new ClipData.Item(uri);
                                ClipData data = new ClipData("image drag",
                                        mimeTypes, item);
                                view.startDragAndDrop(data,
                                        new View.DragShadowBuilder(view), null,
                                        View.DRAG_FLAG_GLOBAL);
                            });
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "performLongClickInternal save bitmap error: " + e.getMessage());
                }
            });
        }
    }

    private void drawImage(View view) {
        ImageView iv = (ImageView) view;
        Log.e(TAG, "ImageView: " + view);
        Drawable drawable = iv.getDrawable();
        Log.e(TAG, "drawable: " + drawable);
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

    private Bitmap tryExtract(ImageView imageView) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(),
                    Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            imageView.draw(canvas);
        } catch (Exception e) {
            Log.e(TAG, "createBitmap once error: " + e.getMessage());
        }
        if (bitmap == null) {
            try {
                imageView.setDrawingCacheEnabled(true);
                imageView.buildDrawingCache();
                if (imageView.getDrawingCache() != null) {
                    bitmap = Bitmap.createBitmap(imageView.getDrawingCache());
                }
                imageView.setDrawingCacheEnabled(false);
            } catch (Exception e) {
                Log.e(TAG, "createBitmap second error: " + e.getMessage());
            }
        }
        return bitmap;
    }


    private void drawText(View view, CharSequence content) {
        view.startDragAndDrop(ClipData.newPlainText("drag text", content),
                new View.DragShadowBuilder(view), null, View.DRAG_FLAG_GLOBAL);
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
        Log.e(TAG, "adapterWx start: " + view);
        if (checkIdName(view, "bkg") && view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            Log.e(TAG, "adapterWx bkg:" + vg + "," + vg.getChildCount());
            if (vg.getChildCount() > 0) {
                View childAt = vg.getChildAt(0);
                Log.e(TAG, "adapterWx count:" + childAt);
                if (childAt instanceof ViewGroup) {
                    // 微信聊天界面地址
                    Log.e(TAG, "adapterWx childAt group: " + childAt);
                    View vgf = ((ViewGroup) childAt).getChildAt(0);
                    Log.e(TAG, "adapterWx vgl: " + vgf);
                    if (vgf instanceof ViewGroup) {
                        View vgl = ((ViewGroup) vgf).getChildAt(0);
                        Log.e(TAG, "adapterWx vgf: " + vgf);
                        if (vgl instanceof ViewGroup) {
                            if (((ViewGroup) vgl).getChildCount() > 1) {
                                StringBuilder address = new StringBuilder();
                                View vglv1 = ((ViewGroup) vgl).getChildAt(0);
                                Log.e(TAG, "adapterWx vgflv1: " + vglv1);
                                if (checkIdName(vglv1, "bp8") && vglv1 instanceof TextView) {
                                    address.append(((TextView) vglv1).getText());
                                }
                                View vglv2 = ((ViewGroup) vgl).getChildAt(1);
                                Log.e(TAG, "adapterWx vgflv2: " + vglv2);
                                if (checkIdName(vglv2, "bp6") && vglv2 instanceof TextView) {
                                    address.append(((TextView) vglv2).getText());
                                }
                                Log.e(TAG, "adapterWx address: " + address);
                                drawText(view, address);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "adapterWx childAt: " + childAt);
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
                Log.e(TAG, "adapterWx clazz: " + clazz);
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
            } catch (Exception e) {
                printLog("MMNeat7extView", "error: " + e.getMessage());
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
                    Log.e(TAG, "adapterWx childAt---drawable: " + drawable);
                    if ("com.tencent.mm.plugin.gif.u".equals(clazz.getName())) {
                        try {
                            Field sField = clazz.getField("s");
                            Bitmap bitmap = (Bitmap) sField.get(drawable);
                            Field rField = clazz.getField("r");
                            Bitmap[] bitmaps = (Bitmap[]) rField.get(drawable);
                            Log.e(TAG,
                                    "adapterWx childAt---bitmap: " + bitmap + "," + bitmaps.length);
                            drawBitmap(view, bitmap);
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "adapterWx other: " + view);
        }
    }

    private void adapterMeituan(View view) {
        Log.e(TAG, "adapterMeituan start: " + view);
        if (checkViewName(view, "com.facebook.react.views.view.f") && view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            Log.e(TAG, "adapterMeituan bkg:" + vg + "," + vg.getChildCount());
            if (vg.getChildCount() > 3) {
                View child = vg.getChildAt(3);
                if (child instanceof TextView) {
                    CharSequence text = ((TextView) child).getText();
                    drawText(child, text);
                    Log.e(TAG, "adapterMeituan text: " + text);
                }
            }
        }
    }

    public void sendAllowDragApp(Context context, ClipData clipData, String pkg) {
        Intent shareBarIntent = getShareBarIntent();
        shareBarIntent.setClipData(clipData);
        shareBarIntent.setAction("action.hfc.custom.pkg_drag");
        shareBarIntent.putExtra("target_pkg", pkg);
        context.sendBroadcast(shareBarIntent);
    }

    public void dragStart(Context context, ClipData data) {
        showBar(context, true, data);
    }

    public boolean hasDrag(View view) {
        mCurrentDragView = view;
        if (mCurrentDragView != null) {
            Log.e(TAG, "hasDrag: " + mCurrentDragView);
            if (!mCurrentDragView.hasDrag) {
                mCurrentDragView.hasDrag = true;
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private void showBar(Context context, boolean show, ClipData data) {
        if (context == null) {
            Log.e(TAG, "showBar context is null");
            return;
        }
        Log.e(TAG, "showBar: " + show + "," + context.getPackageName());
        if (!PM_WRITE_LIST.contains(context.getPackageName())) {
            Log.e(TAG, "showBar pkg is not in write list");
            return;
        }
        Intent intent = getShareBarIntent();
        intent.setAction(show ?  "action.hfc.show": "action.hfc.hide");
        intent.setClipData(data);
        context.sendBroadcast(intent);
    }

    private Intent getShareBarIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.hfc.manager",
                "com.hfc.manager.HfcManagerReceiver"));
        return intent;
    }

    public void printLog(String tag, String msg) {
        Log.e(TAG, "printLog tag: " + tag + ",msg: " + msg);
    }
}
