package com.hfc.netty

import com.hfc.netty.base.BaseHandler
import com.hfc.netty.base.SimpleChannelHandler
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object StingrClientHandler : BaseHandler<StringClientListener>() {
    private val TAG = "StingrClientHandler"

    fun startClient(ip: String, port: Int = 1235) {
        buildTcpClient(ip, port) { socketChannel ->
            socketChannel.pipeline().addLast(LineBasedFrameDecoder(1024))
            socketChannel.pipeline().addLast(StringDecoder(Charsets.UTF_8))
            socketChannel.pipeline()
                .addLast(object : SimpleChannelHandler<String, StringClientListener>(listener, this) {
                    override fun messageReceived(
                        ctx: ChannelHandlerContext?,
                        msg: String
                    ) {
                        listener?.messageReceived(msg)
                    }
                })
        }
    }

    fun sendMsg(msg: String) {
        checkHandlerContext {
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    it.writeAndFlush(
                        Unpooled.copiedBuffer(
                            (msg + System.getProperty("line.separator"))
                                .toByteArray()
                        )
                    )
                }
            }
        }
    }
}