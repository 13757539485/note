package com.cariad.m2.bluetooth.bt

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.cariad.m2.bluetooth.base.BluetoothListener
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

/**
 *
 */
class HandleSocket(private val socket: BluetoothSocket?, private val listener: BluetoothListener?) {
    private val reader = BufferedReader(InputStreamReader(socket?.inputStream))
    private val writer =
        PrintWriter(BufferedWriter(OutputStreamWriter(socket?.outputStream)), true)
    companion object {
        private val TAG = HandleSocket::class.java.simpleName
    }
    private val job = Job()
    private val scope = CoroutineScope(job)
    @Volatile
    private var startRead = false

    suspend fun start() = withContext(Dispatchers.IO) {
        startRead = true
        var content: String?
        while (startRead) {
            try {
                content = reader.readLine()
            } catch (e: Exception) {
                startRead = false
                e.message?.let { listener?.onError(it) }
                return@withContext
            }

            if (!content.isNullOrEmpty()) {
                listener?.onMsgRecv(socket, content)
            } else {
                listener?.onError("断开连接")
                startRead = false
            }
        }
        this@HandleSocket.cancel()
    }

    fun sendMsg(msg: String) {
        if (!startRead) {
            Log.e(TAG, "sendMsg not call start()")
            return
        }
        scope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                sendScope(msg)
            }

            if (result != null) {
                listener?.onError(result)
            }
        }
    }

    private fun sendScope(msg: String): String? {
        return try {
            writer.println(msg)
            null
        } catch (e: Exception) {
            e.toString()
        }
    }

    fun cancel() {
        startRead = false
        socket?.close()
        close(reader)
        close(writer)
        job.cancel()
    }
}

fun close(vararg closeable: Closeable?) {
    closeable.forEach { obj ->
        obj?.close()
    }
}