package com.cariad.m2.netty.im

import android.util.Log
import com.cariad.m2.netty.decoder.CustomMessageDecoder
import com.cariad.m2.netty.encoder.CustomMessageEncoder
import com.cariad.m2.netty.pack.CustomMessage
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.NoRouteToHostException

class ClientHandler(private val name: String, private val callback: IHandlerCallback) :
    SimpleChannelInboundHandler<CustomMessage>() {
    private val group: EventLoopGroup by lazy { NioEventLoopGroup() }
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }
    private val TAG = "ClientHandler"
    private var channel: Channel? = null
    private var future: ChannelFuture? = null
    private var channelHandlerContext: ChannelHandlerContext? = null

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

    override fun messageReceived(ctx: ChannelHandlerContext?, msg: CustomMessage) {
        Log.d(TAG, "messageReceived() called with: ctx = $name, msg = $msg")
        callback.onReceive(msg)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        Log.d(TAG, "channelInactive() called with: ctx = $name")
        channelHandlerContext = null
        callback.channelInActive(name, "")
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        super.channelActive(ctx)
        channelHandlerContext = ctx
        Log.d(TAG, "channelActive() called with: ctx = $name")
        callback.channelActive(name)
    }

    fun start(host: String, port: Int, block: (ch: SocketChannel) -> Unit) {
        coroutineScope.launch {
            try {
                val bootstrap = Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel::class.java)
                    .handler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            block(ch)
                        }
                    })
                future = bootstrap.connect(host, port).sync()
                channel = future?.channel()

                // 发送初始化消息
                channel?.writeAndFlush(CustomMessage(name, "init-info", "", null))
                channel?.closeFuture()?.sync()
            } catch (e: Exception) {
                if (e is ConnectException || e is NoRouteToHostException) {
                    Log.e(TAG, "server is not running: ${e.message}")
                    callback.channelInActive(name, "ConnectException")
                } else {
                    e.printStackTrace()
                }
            }
        }
    }

    fun checkHandlerContext(block: (channelHandlerContext: ChannelHandlerContext) -> Unit) {
        if (channelHandlerContext == null || channelHandlerContext?.channel() == null
            || channelHandlerContext?.channel()?.isActive != true
        ) {
            Log.e(TAG, "connect is not active, please check")
            return
        }
        val eventLoop = channelHandlerContext!!.channel().eventLoop()
        if (eventLoop.inEventLoop()) {
            block(channelHandlerContext!!)
        } else {
            eventLoop.execute {
                block(channelHandlerContext!!)
            }
        }
    }

    fun close() {
        coroutineScope.launch {
            group.shutdownGracefully()
            future?.channel()?.close()?.sync()
            channel?.closeFuture()?.syncUninterruptibly()
            future = null
            channel = null
            channelHandlerContext?.close()?.sync()
            channelHandlerContext = null
            Log.d(TAG, "client close")
        }
    }
}