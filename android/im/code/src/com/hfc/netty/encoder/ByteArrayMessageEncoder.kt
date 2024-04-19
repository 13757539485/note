package com.hfc.netty.encoder

import com.hfc.netty.pack.ByteArrayMessage
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

/**
 * 编码器，用来将消息(ByteArrayMessage对象)转换为字节数组
 */
class ByteArrayMessageEncoder: MessageToByteEncoder<ByteArrayMessage>() {
    override fun encode(ctx: ChannelHandlerContext?, msg: ByteArrayMessage, out: ByteBuf) {
        println("start encode")
        out.writeInt(msg.length)
        out.writeBytes(msg.data)
        out.writeByte(msg.type)
        println("end encode")
    }
}