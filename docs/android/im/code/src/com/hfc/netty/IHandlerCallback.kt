package com.cariad.m2.netty.im

import com.cariad.m2.netty.pack.CustomMessage

interface IHandlerCallback {
    fun channelActive(deviceName: String)
    fun channelInActive(deviceName: String, msg: String)
    fun onReceive(msg: CustomMessage)
}