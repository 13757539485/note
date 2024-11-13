package com.cariad.m2.bluetooth.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.cariad.m2.bluetooth.base.BLUE_UUID
import com.cariad.m2.bluetooth.base.BluetoothListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
class BtServer(private val listener: BluetoothListener?) {
    private val TAG = "BtServer"
    private val bluetooth by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var serverSocket: BluetoothServerSocket? = null
    private var handleSocket: HandleSocket? = null

    init {
        listener?.onStart()
        serverSocket = bluetooth.listenUsingInsecureRfcommWithServiceRecord(TAG, BLUE_UUID)
    }

    @Volatile
    private var isStart: Boolean = false

    suspend fun startServer() = withContext(Dispatchers.IO) {
        isStart = true
        while (isStart) {
            try {
                val socket = serverSocket?.accept()
                socket?.also {
                    listener?.onConnected(it.remoteDevice.name)
                }
                handleSocket = HandleSocket(socket, listener)
                handleSocket?.start()
            } catch (e: Exception) {
                Log.d(TAG, "blue socket accept fail: ${e.message}")
                listener?.onError("blue socket accept fail: ${e.message}")
            }
        }
    }

    fun sendMsg(msg: String) {
        if (!isStart) {
            Log.d(TAG, "server is not start")
            return
        }
        handleSocket?.sendMsg(msg)
    }

    fun close() {
        isStart = false
        handleSocket?.cancel()

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            listener?.onError("serverSocket close error: $e")
        }
    }
}