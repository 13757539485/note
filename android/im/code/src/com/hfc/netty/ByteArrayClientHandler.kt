package com.hfc.netty

import android.graphics.BitmapFactory
import com.hfc.netty.base.BaseHandler
import com.hfc.netty.base.SimpleChannelHandler
import com.hfc.netty.decoder.ByteArrayMessageDecoder
import com.hfc.netty.encoder.ByteArrayMessageEncoder
import com.hfc.netty.pack.ByteArrayMessage
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object ByteArrayClientHandler: BaseHandler<ByteArrayClientListener>() {

    fun startByteArrayClient(ip: String, port: Int = 1234) {
        buildTcpClient(ip, port) {socketChannel->
            socketChannel.pipeline().addLast(ByteArrayMessageEncoder())
            socketChannel.pipeline().addLast(ByteArrayMessageDecoder())
            socketChannel.pipeline()
                .addLast(object : SimpleChannelHandler<ByteArrayMessage, ByteArrayClientListener>(listener, this) {
                    override fun messageReceived(
                        ctx: ChannelHandlerContext?,
                        msg: ByteArrayMessage
                    ) {
                        when (msg.type) {
                            0 -> {
                                listener?.messageReceived(
                                    BitmapFactory.decodeByteArray(
                                        msg.data,
                                        0,
                                        msg.length
                                    )
                                )
                            }

                            1 -> {
                                listener?.messageReceived(String(msg.data))
                            }
                        }
                    }
                })
        }
    }

    fun sendMsg(msg: List<ByteArrayMessage>) {
        checkHandlerContext {chc->
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    msg.forEach {
                        chc.writeAndFlush(it)
                    }
                }
            }
        }
    }

    fun sendStr(msg: String) {
        checkHandlerContext { chc ->
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    val bytes = msg.toByteArray()
                    chc.writeAndFlush(ByteArrayMessage(bytes, bytes.size, 1))
                }
            }
        }
    }
}