package com.cariad.m2.sharebar.util

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View

fun dp2px(context: Context, value: Float) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, value,
    context.resources.displayMetrics
).toInt()

fun dp2pxReal( value: Float): Float = DisplayMetrics.DENSITY_DEVICE_STABLE.toFloat() / DisplayMetrics.DENSITY_DEFAULT * value

fun px2dp(context: Context, value: Float) =
    (value / context.resources.displayMetrics.density).toInt()

fun View.dp2px(value: Float) = dp2px(context, value)

fun View.px2dp(value: Float) = px2dp(context, value)

fun getStatusBarHeight(context: Context): Int {
    /*var result = 0
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = context.resources.getDimensionPixelSize(resourceId)
    }*/
    return 86
}