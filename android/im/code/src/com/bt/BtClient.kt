package com.cariad.m2.bluetooth.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.cariad.m2.bluetooth.base.BLUE_UUID
import com.cariad.m2.bluetooth.base.BluetoothListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
class BtClient(private val bluetoothDevice: BluetoothDevice, private val listener: BluetoothListener?) {
    private val TAG = "BtClient"
    private val bluetooth by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val socket: BluetoothSocket? by lazy {
        bluetoothDevice.createRfcommSocketToServiceRecord(BLUE_UUID)
    }

    init {
        listener?.onStart()
    }

    private var handleSocket: HandleSocket? = null

    suspend fun startClient() = withContext(Dispatchers.IO){
        bluetooth.cancelDiscovery()
        try {
            socket?.run {
                connect()
                remoteDevice?.let { listener?.onConnected(it.name) }
                handleSocket = HandleSocket(socket, listener)
                handleSocket?.start()
            }
        } catch (e: Exception) {
            Log.d(TAG, "start client error: ${e.message}")
            listener?.onError("start client error: ${e.message.toString()}")
        }
    }

    fun sendMsg(msg: String) {
        handleSocket?.sendMsg(msg)
    }

    fun close() {
        handleSocket?.cancel()
    }
}