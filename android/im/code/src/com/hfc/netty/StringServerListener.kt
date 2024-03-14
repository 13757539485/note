package com.hfc.netty

import com.hfc.netty.base.SimpleListener

interface StringServerListener: SimpleListener {
    fun messageReceived(msg: String)
}