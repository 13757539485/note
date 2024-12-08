package com.cariad.m2.netty.im

import android.os.Build
import com.cariad.m2.netty.decoder.CustomMessageDecoder
import com.cariad.m2.netty.encoder.CustomMessageEncoder
import com.cariad.m2.netty.pack.CustomMessage
import io.netty.channel.ChannelPipeline

class NettyClient {
    private val clientName by lazy { Build.BRAND }
    private var clientHandler: ClientHandler? = null

    fun start(host: String, port: Int, callback: IHandlerCallback) {
        if (clientHandler != null) {
            stop()
        }
        clientHandler = ClientHandler(clientName, callback)
        clientHandler?.start(host, port) { ch ->
            val pipeline: ChannelPipeline = ch.pipeline()
            pipeline.addLast(CustomMessageDecoder())
            pipeline.addLast(CustomMessageEncoder())
            pipeline.addLast(clientHandler)
        }
    }

    fun stop() {
        clientHandler?.close()
        clientHandler = null
    }

    fun sendMessage(message: CustomMessage) {
        clientHandler?.checkHandlerContext { ctx ->
            ctx.writeAndFlush(message)
        }
    }
}