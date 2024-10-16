package com.bit.wificonn

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.net.Socket


class ConnectionActivity(
    private val ip: String,
    private val port: Int
) {
    private var socket: Socket? = null
    private var dos: DataOutputStream? = null

    suspend fun connect() = withContext(Dispatchers.IO) {
        socket = Socket(ip, port)
        dos = DataOutputStream(BufferedOutputStream(socket?.outputStream))
    }

    suspend fun sendToEsp(msg: String) = withContext(Dispatchers.IO) {
        Log.d("wifi", "recv")
        dos?.writeBytes(msg)
        dos?.flush()
        Log.d("wifi", "sent")
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        dos?.close()
        socket?.close()
    }
}

