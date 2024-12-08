package com.cariad.m2.netty.im

import android.graphics.Bitmap
import com.cariad.m2.netty.decoder.CustomMessageDecoder
import com.cariad.m2.netty.encoder.CustomMessageEncoder
import com.cariad.m2.netty.pack.CustomMessage
import io.netty.channel.ChannelPipeline

class NettyServer {
    private var serverHandler: ServerHandler? = null

    fun stop() {
        serverHandler?.close()
        serverHandler = null
    }

    fun sendMessageByAll(message: CustomMessage) {
        serverHandler?.sendMessageByAll(message)
    }

    fun sendMessageByName(message: CustomMessage) {
        serverHandler?.sendMessageByName(message)
    }

    fun start(port: Int, callback: IHandlerCallback) {
        if (serverHandler != null) {
            stop()
        }
        serverHandler = ServerHandler(callback)
        serverHandler?.start(port) { ch ->
            val pipeline: ChannelPipeline = ch.pipeline()
            pipeline.addLast(CustomMessageDecoder())
            pipeline.addLast(CustomMessageEncoder())
            pipeline.addLast(serverHandler)
        }
    }
}