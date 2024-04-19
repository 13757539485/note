package com.hfc.netty

import android.graphics.Bitmap
import com.hfc.netty.base.SimpleListener

interface ByteArrayServerListener: SimpleListener {
    fun messageReceived(bitmap: Bitmap)

    fun messageReceived(msg: String)
}