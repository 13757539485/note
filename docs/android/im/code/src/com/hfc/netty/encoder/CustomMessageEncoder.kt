package com.hfc.netty.encoder

import com.cariad.m2.netty.pack.CustomMessage
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class CustomMessageEncoder : MessageToByteEncoder<CustomMessage>() {
    override fun encode(ctx: ChannelHandlerContext, msg: CustomMessage, out: ByteBuf) {
        println("start encode")
        out.writeInt(msg.name.length)
        out.writeBytes(msg.name.toByteArray())
        out.writeInt(msg.type.length)
        out.writeBytes(msg.type.toByteArray())
        out.writeInt(msg.content.length)
        out.writeBytes(msg.content.toByteArray())

        msg.getBitmapBytes()?.let {
            out.writeInt(it.size)
            out.writeBytes(it)
        } ?: out.writeInt(0)
        println("end encode")
    }
}