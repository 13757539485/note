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
import java.net.BindException

object StringServerHandler : BaseHandler<StringServerListener>() {
    private val TAG = "StringServerHandler"

    fun startServer(port: Int = 1235) {
        println("$TAG -------------------------$port")
        if (port < 0 || port > 65535) {
            println("$TAG $port is invalid")
            return
        }
        buildTcpServer(port, exce = {
            if (it is BindException) {
                startServer(port + 1)
            }
        }) { socketChannel ->
            socketChannel.pipeline().addLast(LineBasedFrameDecoder(1024))
            socketChannel.pipeline().addLast(StringDecoder(Charsets.UTF_8))
            socketChannel.pipeline()
                .addLast(object :
                    SimpleChannelHandler<String, StringServerListener>(listener, this) {
                    override fun messageReceived(
                        ctx: ChannelHandlerContext,
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