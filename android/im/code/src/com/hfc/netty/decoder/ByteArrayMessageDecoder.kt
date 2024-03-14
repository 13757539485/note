package com.hfc.netty.decoder

import com.hfc.netty.pack.ByteArrayMessage
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

/**
 * 解码器，用来将字节数组转换为消息(ByteArrayMessage对象)
 */
class ByteArrayMessageDecoder: ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext?, byteBuf: ByteBuf, out: MutableList<Any>) {
        println("start decode")
        if (byteBuf.readableBytes() < 4) {
            return
        }
        byteBuf.markReaderIndex()
        val length = byteBuf.readInt()
        if (byteBuf.readableBytes() < length) {
            byteBuf.resetReaderIndex()
            return
        }
        val data = ByteArray(length)
        byteBuf.readBytes(data);
        val messageType = byteBuf.readByte().toInt()
        out.add(ByteArrayMessage(data, length, messageType))
        println("end decode")
    }
}