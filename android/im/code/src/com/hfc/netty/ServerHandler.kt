package com.cariad.m2.netty.im

import android.graphics.Bitmap
import android.util.Log
import com.cariad.m2.netty.decoder.CustomMessageDecoder
import com.cariad.m2.netty.encoder.CustomMessageEncoder
import com.cariad.m2.netty.pack.CustomMessage
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.AttributeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Sharable
class ServerHandler(private val callback: IHandlerCallback) :
    SimpleChannelInboundHandler<CustomMessage>() {
    private val bossGroup by lazy { NioEventLoopGroup() }
    private val workerGroup by lazy { NioEventLoopGroup() }
    private var channel: Channel? = null
    private var future: ChannelFuture? = null
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }
    private val TAG = "ServerHandler"
    private val NAME_KEY: AttributeKey<String> = AttributeKey.valueOf("name")
    private val allClients by lazy {  ConcurrentHashMap<String, ChannelHandlerContext>()}
    private val heartbeatInterval = 30L // 心跳间隔，单位：秒

    override fun messageReceived(ctx: ChannelHandlerContext, msg: CustomMessage) {
        Log.d(TAG, "messageReceived: $msg")
        if (ctx.channel().attr(NAME_KEY).get() == null) {
            ctx.channel().attr(NAME_KEY).set(msg.name)
            if (!allClients.containsKey(msg.name)) {
                allClients[msg.name] = ctx
                callback.channelActive(msg.name)
                Log.d(TAG, "New client connected: $ctx ${msg.name}")
            }
        } else {
            //心跳包暂不处理
            Log.d(TAG, "onReceive: $msg")
            callback.onReceive(msg)
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        val name = ctx.channel().attr(NAME_KEY).get()
        if (name != null) {
            if (allClients.containsKey(name)) {
                allClients.remove(name)
                Log.d(TAG, "Client disconnected: $name")
            }
        }
        callback.channelInActive(name, "")
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

    fun sendMessageByAll(message: CustomMessage) {
        for (ctx in allClients.values) {
            ctx.writeAndFlush(message)
        }
    }

    fun sendMessageByName(message: CustomMessage) {
        allClients[message.name]?.writeAndFlush(message)
    }

    fun start(port: Int, block: (ch: SocketChannel) -> Unit) {
        coroutineScope.launch {
            try {
                val bootstrap = ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            block(ch)
                        }
                    })
                future = bootstrap.bind(port).sync()
                channel = future?.channel()
                println("server running: $port")
                channel!!.closeFuture().sync()
            } catch (e: Exception) {
                e.printStackTrace()
                callback.channelInActive("", "")
            }
        }
    }

    fun close() {
        coroutineScope.launch {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
            future?.channel()?.close()?.sync()
            channel?.closeFuture()?.syncUninterruptibly()
            future = null
            channel = null
            for (ctx in allClients.values) {
                ctx.close().sync()
            }
            allClients.clear()
            Log.d(TAG, "server close")
        }
    }

    private fun scheduleHeartbeat(ctx: ChannelHandlerContext) {
        ctx.executor().schedule({
            if (ctx.channel().isActive) {
                ctx.writeAndFlush(CustomMessage("Server", "heartbeat", "",null))
                scheduleHeartbeat(ctx)
            }
        }, heartbeatInterval, TimeUnit.SECONDS)
    }
}

