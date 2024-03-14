package com.hfc.netty

import com.hfc.netty.base.SimpleListener

interface StringClientListener: SimpleListener {
    fun messageReceived(msg: String)
}