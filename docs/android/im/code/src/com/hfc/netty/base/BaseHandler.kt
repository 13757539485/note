package com.hfc.netty.base

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.NoRouteToHostException

abstract class BaseHandler<T : SimpleListener> {
    protected val mainScope by lazy { MainScope() }
    private var channel: Channel? = null
    private var workerGroup: EventLoopGroup? = null
    protected var listener: T? = null
    var channelHandlerContext: ChannelHandlerContext? = null

    open fun registerListener(listener: T) {
        this.listener = listener
    }

    fun buildTcpServer(
        port: Int,
        exce: ((e: Exception) -> Unit)? = null,
        block: (socketChannel: SocketChannel) -> Unit
    ) {
        if (channel != null && channel!!.isActive) {
            println("server already start: $port")
            return
        }
        mainScope.launch {
            withContext(Dispatchers.IO) {
                workerGroup = NioEventLoopGroup()
                val bootstrap = defaultTcpClientOption()
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        @Throws(Exception::class)
                        override fun initChannel(socketChannel: SocketChannel) {
                            block(socketChannel)
                        }
                    })
                try {
                    val future = bootstrap.bind(port).sync()
                    channel = future.channel()
                    println("server running: $port")
                    channel!!.closeFuture().sync()
                } catch (e: Exception) {
                    e.printStackTrace()
                    exce?.invoke(e)
                }
            }
        }
    }

    private fun defaultTcpClientOption(): ServerBootstrap {
        workerGroup = NioEventLoopGroup()
        val bootstrap = ServerBootstrap()
        return bootstrap.group(workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
            .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
    }

    fun buildTcpClient(ip: String, port: Int, block: (socketChannel: SocketChannel) -> Unit) {
        if (channel != null && channel!!.isActive) {
            println("client already start")
            return
        }
        mainScope.launch {
            withContext(Dispatchers.IO) {
                workerGroup = NioEventLoopGroup()
                val bootstrap = Bootstrap()
                bootstrap.group(workerGroup)
                    .channel(NioSocketChannel::class.java)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(object : ChannelInitializer<SocketChannel>() {
                        @Throws(Exception::class)
                        override fun initChannel(socketChannel: SocketChannel) {
                            block(socketChannel)
                        }
                    })
                try {
                    val future = bootstrap.connect(ip, port).sync()
                    channel = future.channel()
                    println("client running $ip:$port")
                    future.channel().closeFuture().sync()
                } catch (e: Exception) {
                    if (e is ConnectException || e is NoRouteToHostException) {
                        println("server is not running: ${e.message}")
                        listener?.channelInActive("ConnectException")
                    } else {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun checkHandlerContext(block: (channelHandlerContext: ChannelHandlerContext) -> Unit) {
        if (channelHandlerContext == null || channelHandlerContext?.channel() == null
            || channelHandlerContext?.channel()?.isActive != true
        ) {
            println("connect is not active, please check")
            return
        }
        block(channelHandlerContext!!)
    }

    fun isActive(): Boolean {
        return !(channelHandlerContext == null || channelHandlerContext?.channel() == null
                || channelHandlerContext?.channel()?.isActive != true)
    }

    open fun close() {
        mainScope.launch {
            withContext(Dispatchers.IO) {
                workerGroup?.shutdownGracefully()
                channel?.closeFuture()?.syncUninterruptibly()
                channel = null
                channelHandlerContext?.close()?.sync()
                channelHandlerContext = null
            }
            println("server close")
        }
    }
}