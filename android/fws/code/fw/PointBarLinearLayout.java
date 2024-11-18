package com.cariad.m2;

import static com.android.internal.policy.SystemBarUtils.WindowMode.SPLIT_PHONE_LEFT;
import static com.android.internal.policy.SystemBarUtils.WindowMode.SPLIT_PHONE_RIGHT;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import android.annotation.Nullable;
import android.widget.PopupWindow;

import com.android.internal.R;
import com.android.internal.policy.SystemBarUtils;

public class PointBarLinearLayout extends LinearLayout {
    private ViewGroup containerFull;
    private ViewGroup containerSplitLeft;
    private ViewGroup containerSplitRight;
    private ViewGroup containerFloating;
    private ViewGroup containerExit;
    private PointBarImageView btnFloating;
    private PointBarImageView btnSplitRight;
    private PointBarImageView btnSplitLeft;
    private PointBarImageView btnSplitExit;
    private PointBarImageView btnFull;

    public PointBarLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setColor(0xF2FFFFFF); // 设置背景颜色
        gradientDrawable.setCornerRadius(24); // 设置圆角半径
        setBackground(gradientDrawable);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, 103);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        containerFull = (ViewGroup) getChildAt(0);
        btnFull = (PointBarImageView) containerFull.getChildAt(0);
        setClickTag(btnFull, PointBarImageView.CLICK_FULL);

        containerSplitLeft = (ViewGroup) getChildAt(1);
        btnSplitLeft = (PointBarImageView) containerSplitLeft.getChildAt(0);
        setClickTag(btnSplitLeft, PointBarImageView.CLICK_SPLIT_LEFT);

        containerSplitRight = (ViewGroup) getChildAt(2);
        btnSplitRight = (PointBarImageView) containerSplitRight.getChildAt(0);
        setClickTag(btnSplitRight, PointBarImageView.CLICK_SPLIT_RIGHT);

        containerFloating = (ViewGroup) getChildAt(3);
        btnFloating = (PointBarImageView) containerFloating.getChildAt(0);
        setClickTag(btnFloating, PointBarImageView.CLICK_FLOATING);

        containerExit = (ViewGroup) getChildAt(4);
        btnSplitExit = (PointBarImageView) containerExit.getChildAt(0);
        setClickTag(btnSplitExit, PointBarImageView.CLICK_EXIT);
    }

    private void setClickTag(PointBarImageView view, int click) {
        view.setClickIndex(click);
    }

    private void visibleView(boolean full, boolean left, boolean right,
                             boolean floating) {
        containerFull.setVisibility(full ? VISIBLE : GONE);
        containerSplitLeft.setVisibility(left ? VISIBLE : GONE);
        containerSplitRight.setVisibility(right ? VISIBLE : GONE);
        containerFloating.setVisibility(floating ? VISIBLE : GONE);
    }

    public void updateView(SystemBarUtils.WindowMode windowMode) {
        switch (windowMode) {
            case FULL_CAR:
                visibleView(false, true, true, false);
                break;
            case FULL_PHONE:
                visibleView(false, true, true, true);
                break;
            case SPLIT_PHONE_LEFT:
                visibleView(true, false, true, true);
                break;
            case SPLIT_PHONE_RIGHT:
                visibleView(true, true, false, true);
                break;
            case SPLIT_CAR_LEFT:
                visibleView(true, false, true, false);
                break;
            case SPLIT_CAR_RIGHT:
                visibleView(true, true, false, false);
                break;
            default:
                break;
        }
        btnSplitLeft.setWindowMode(windowMode);
        btnSplitRight.setWindowMode(windowMode);
        btnSplitExit.setWindowMode(windowMode);
        if (windowMode == SPLIT_PHONE_LEFT) {
            btnSplitRight.setClickIndex(PointBarImageView.CLICK_FLOATING);
            btnFloating.setClickIndex(PointBarImageView.CLICK_SPLIT_RIGHT);

            btnSplitRight.setImageDrawable(getResources().getDrawable(R.drawable.icon_point_bar_float));
            btnFloating.setImageDrawable(getResources().getDrawable(R.drawable.icon_point_bar_right));
        } else if (windowMode == SPLIT_PHONE_RIGHT){
            btnSplitLeft.setClickIndex(PointBarImageView.CLICK_FLOATING);
            btnFloating.setClickIndex(PointBarImageView.CLICK_SPLIT_LEFT);

            btnSplitLeft.setImageDrawable(getResources().getDrawable(R.drawable.icon_point_bar_float));
            btnFloating.setImageDrawable(getResources().getDrawable(R.drawable.icon_point_bar_left));
        } else {
            btnSplitLeft.setClickIndex(PointBarImageView.CLICK_SPLIT_LEFT);
            btnSplitRight.setClickIndex(PointBarImageView.CLICK_SPLIT_RIGHT);
            btnFloating.setClickIndex(PointBarImageView.CLICK_FLOATING);

            btnSplitLeft.setImageDrawable(getResources().getDrawable(R.drawable.icon_point_bar_left));
            btnSplitRight.setImageDrawable(getResources().getDrawable(R.drawable.icon_point_bar_right));
            btnFloating.setImageDrawable(getResources().getDrawable(R.drawable.icon_point_bar_float));
        }
    }

    public void setWindow(PopupWindow popupWindow) {
        btnFull.setWindow(popupWindow);
        btnFloating.setWindow(popupWindow);
        btnSplitLeft.setWindow(popupWindow);
        btnSplitRight.setWindow(popupWindow);
        btnSplitExit.setWindow(popupWindow);
    }
}
