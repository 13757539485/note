package com.xxx.xx;

import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Rounded animation class, only to use in system window animation
 * @hide
 * */
public class RoundCornerAnimation extends Animation {

    private float startRadius;
    private float endRadius;

    public RoundCornerAnimation(float startRadius, float endRadius) {
        this.startRadius = startRadius;
        this.endRadius = endRadius;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float radius = startRadius + ((endRadius - startRadius) * interpolatedTime);
        t.setRadius(radius);
    }

    @Override
    public boolean willChangeBounds() {
        return false;
    }

    @Override
    public boolean hasRoundedCorners() {
        return true;
    }
}
