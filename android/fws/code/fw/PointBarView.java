package com.cariad.m2;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.android.internal.R;
import com.android.internal.policy.SystemBarUtils;

/**
 * @author yuli
 * <p>
 * point bar view
 * @hide
 */
public class PointBarView extends FrameLayout implements View.OnClickListener {
    public static final String TAG = "PointBarView";

    private static final int FULL_POPUP_WIDTH = 686;
    private static final int SPLIT_POPUP_WIDTH = 549;
    private PopupWindow popupWindow;
    private PointBarLinearLayout popupView;
    private int popupWindowWidth;
    public float scaleDensity = 1f;

    private SystemBarUtils.WindowMode windowMode;

    public PointBarView(Context context) {
        super(context);
        init();
    }

    private void init() {
        float originDensity = DisplayMetrics.DENSITY_DEVICE_STABLE * 1.0f / DisplayMetrics.DENSITY_MEDIUM;
        float currentDensity = getResources().getDisplayMetrics().density;
        scaleDensity = originDensity / currentDensity;

        // 创建弹出视图
        popupView = (PointBarLinearLayout) View.inflate(getContext(), R.layout.point_bar_popup, null);
        popupView.setLayoutParams(new ViewGroup.LayoutParams(FULL_POPUP_WIDTH, 103));
        popupWindow = new PopupWindow(popupView, FULL_POPUP_WIDTH, 103);
        popupView.setWindow(popupWindow);
        popupWindow.setElevation(SystemBarUtils.isEqual(scaleDensity, 1f) ? 10f : 0);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setAnimationStyle(R.style.Animation_PopupWindow_CariadBar);
        setClickable(true);
        setOnClickListener(this);
        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(R.drawable.dots);
        addView(imageView, new FrameLayout.LayoutParams(38, 15, Gravity.CENTER));
    }

    @Override
    public void onClick(View v) {
        uiUpdate();
        int offsetX = (this.getWidth() - popupWindowWidth) / 2;
        popupWindow.setWidth(popupWindowWidth);
        popupWindow.showAsDropDown(this, offsetX, 0);
    }

    private void updatePopupWindowWidth(SystemBarUtils.WindowMode windowMode) {
        if (windowMode == SystemBarUtils.WindowMode.FULL_CAR || windowMode == SystemBarUtils.WindowMode.FULL_PHONE) {
            popupWindowWidth = FULL_POPUP_WIDTH;
        } else {
            popupWindowWidth = SPLIT_POPUP_WIDTH;
        }
    }

    public void hidePopupWindowIfNeeded() {
        if (popupWindow.isShowing())
            popupWindow.dismiss();
    }

    private void uiUpdate() {
        windowMode = SystemBarUtils.getWindowMode(getContext());
        updatePopupWindowWidth(windowMode);
        popupView.updateView(windowMode);
    }
}
