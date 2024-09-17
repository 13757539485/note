package com.cariad.m2;

import android.annotation.Nullable;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.android.internal.policy.SystemBarUtils;

public class PointBarImageView extends ImageView implements View.OnClickListener {
    public static final int CLICK_FULL = 0;
    public static final int CLICK_SPLIT_LEFT = 1;
    public static final int CLICK_SPLIT_RIGHT = 2;
    public static final int CLICK_FLOATING = 3;
    public static final int CLICK_EXIT = 4;
    private static final String TAG = "PointBarImageView";

    private PopupWindow popupWindow;
    private int clickIndex;
    private SystemBarUtils.WindowMode windowMode;

    public PointBarImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEnablePressedAnim(true);
        setOnClickListener(this);
        setPadding(10, 10, 10, 10);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(54, 54);
    }

    public void setClickIndex(int index) {
        this.clickIndex = index;
    }

    @Override
    public void onClick(View v) {
        postDelayed(this::click, 300);
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
    }

    private void click() {
        switch (clickIndex) {
            case CLICK_FULL:
                SplitScreenHelper.startFullScreen(mContext, mContext.getPackageName());
                break;
            case CLICK_SPLIT_LEFT:
                if (isFullMode())
                    SplitScreenHelper.startSplitScreen(mContext, mContext.getPackageName(), true);
                else
                    SplitScreenHelper.changeScreen(mContext, mContext.getPackageName());
                break;
            case CLICK_SPLIT_RIGHT:
                if (isFullMode())
                    SplitScreenHelper.startSplitScreen(mContext, mContext.getPackageName(), false);
                else
                    SplitScreenHelper.changeScreen(mContext, mContext.getPackageName());
                break;
            case CLICK_FLOATING:
                Toast.makeText(mContext, "click floating", Toast.LENGTH_SHORT).show();
                break;
            case CLICK_EXIT:
                if (windowMode == SystemBarUtils.WindowMode.SPLIT_CAR_LEFT
                        || windowMode == SystemBarUtils.WindowMode.SPLIT_PHONE_LEFT)
                    SplitScreenHelper.pushOut(mContext, mContext.getPackageName(), true);
                if (windowMode == SystemBarUtils.WindowMode.SPLIT_CAR_RIGHT
                        || windowMode == SystemBarUtils.WindowMode.SPLIT_PHONE_RIGHT)
                    SplitScreenHelper.pushOut(mContext, mContext.getPackageName(), false);
                break;
            default:
                Log.e(TAG, "unknown click");
                break;
        }
    }

    private boolean isFullMode() {
        return windowMode == SystemBarUtils.WindowMode.FULL_CAR ||
                windowMode == SystemBarUtils.WindowMode.FULL_PHONE;
    }

    public void setWindow(PopupWindow popupWindow) {
        this.popupWindow = popupWindow;
    }

    public void setWindowMode(SystemBarUtils.WindowMode windowMode) {
        this.windowMode = windowMode;
    }

}
