package com.hfc.netty.decoder

import android.graphics.Bitmap
import com.cariad.m2.netty.pack.CustomMessage
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class CustomMessageDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, out: MutableList<Any>) {
        println("start decode")
        if (byteBuf.readableBytes() < 12) {
            println("readableBytes < 12")
            return
        }

        val nameLength = byteBuf.readInt()
        if (byteBuf.readableBytes() < nameLength) {
            byteBuf.resetReaderIndex()
            println("readableBytes < nameLength")
            return
        }

        val nameBytes = ByteArray(nameLength)
        byteBuf.readBytes(nameBytes)
        val name = String(nameBytes)

        val typeLength = byteBuf.readInt()
        if (byteBuf.readableBytes() < typeLength) {
            byteBuf.resetReaderIndex()
            println("readableBytes < typeLength")
            return
        }

        val typeBytes = ByteArray(typeLength)
        byteBuf.readBytes(typeBytes)
        val type = String(typeBytes)

        val contentLength = byteBuf.readInt()
        if (byteBuf.readableBytes() < contentLength) {
            byteBuf.resetReaderIndex()
            println("readableBytes < contentLength")
            return
        }

        val contentBytes = ByteArray(contentLength)
        byteBuf.readBytes(contentBytes)
        val content = String(contentBytes)

        val bitmapLength = byteBuf.readInt()
        val bitmap: Bitmap? = if (bitmapLength > 0) {
            if (byteBuf.readableBytes() < bitmapLength) {
                byteBuf.resetReaderIndex()
                println("readableBytes < bitmapLength")
                return
            }

            val bitmapBytes = ByteArray(bitmapLength)
            byteBuf.readBytes(bitmapBytes)
            CustomMessage.bytesToBitmap(bitmapBytes)
        } else {
            null
        }
        out.add(CustomMessage(name, type, content, bitmap))
        println("end decode")
    }
}