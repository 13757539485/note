package com.hfc.netty.base

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

abstract class SimpleChannelHandler<T: Any, S: SimpleListener>(
    private val listener: SimpleListener?,
    private var baseHandler: BaseHandler<S>
) : SimpleChannelInboundHandler<T>() {

    override fun channelActive(ctx: ChannelHandlerContext?) {
        super.channelActive(ctx)
        baseHandler.channelHandlerContext = ctx
        listener?.channelActive()
        println("channelActive")
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        listener?.channelInActive("")
        println("channelInactive")
    }
}