package com.cariad.m2.sharebar.core

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * 用来跨应用接收控制浮窗的广播类
 */
class ShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            context?.let { ctx ->
                it.component = ComponentName(ctx, ShareBarService::class.java)
                ctx.startService(it)
            }
        }
    }
}