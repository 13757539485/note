package com.hfc.netty.base

interface SimpleListener {
    fun channelActive()
    fun channelInActive(msg: String)
}