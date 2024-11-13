package com.hfc.netty

import android.graphics.Bitmap
import com.hfc.netty.base.SimpleListener

interface ByteArrayClientListener: SimpleListener {
    fun messageReceived(bitmap: Bitmap)
    fun messageReceived(msg: String)
}