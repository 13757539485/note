package com.hfc.netty

//import android.graphics.BitmapFactory
import com.hfc.netty.base.BaseHandler
import com.hfc.netty.base.SimpleChannelHandler
import com.hfc.netty.decoder.ByteArrayMessageDecoder
import com.hfc.netty.encoder.ByteArrayMessageEncoder
import com.hfc.netty.pack.ByteArrayMessage
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ByteArrayServerHandler : BaseHandler<ByteArrayServerListener>() {
    private val TAG = "ByteArrayServerHandler"

    fun startServer(port: Int = 1234) {
        buildTcpServer(port) { socketChannel ->
            socketChannel.pipeline().addLast(ByteArrayMessageEncoder())
            socketChannel.pipeline().addLast(ByteArrayMessageDecoder())
            socketChannel.pipeline()
                .addLast(object :
                    SimpleChannelHandler<ByteArrayMessage, ByteArrayServerListener>(listener, this) {
                    override fun messageReceived(
                        ctx: ChannelHandlerContext,
                        msg: ByteArrayMessage
                    ) {
                        when (msg.type) {
                            0 -> {
                                /*listener?.messageReceived(
                                    BitmapFactory.decodeByteArray(
                                        msg.data,
                                        0,
                                        msg.length
                                    )
                                )*/
                            }

                            1 -> {
                                listener?.messageReceived(String(msg.data))
                            }
                        }
                    }
                })
        }
    }

    fun sendMsg(msg: ByteArrayMessage) {
        checkHandlerContext{
            it.writeAndFlush(msg)
        }
    }

    fun sendStr(msg: String) {
        checkHandlerContext {
            val bytes = msg.toByteArray()
            it.writeAndFlush(ByteArrayMessage(bytes, bytes.size, 1))
        }
    }
}