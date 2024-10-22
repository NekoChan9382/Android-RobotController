package com.bit.wificonn

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketException


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

    suspend fun sendLoop()  = withContext(Dispatchers.IO){

        var preTimeStick = System.currentTimeMillis()
        var preTimeExtractArm = preTimeStick
        var stickChanged = false
        var preTheta = 0
        var preExtractMove = extractArmOrientation.Stop.ordinal

        while(sendLoop) {
            val currentTime = System.currentTimeMillis()

            if (preTheta != thetas) {

                preTheta = thetas
                stickChanged = true
            }
            if (currentTime - preTimeStick > 5 && stickChanged) {
                preTimeStick = currentTime
                stickChanged = false
                val msg = "stick\n$thetas\n"

                if ((dos == null).not()) {
                    try {
                        dos?.writeBytes(msg)
                        dos?.flush()
                    } catch (e: SocketException) {
                        Log.e("wifi", "Send Failed")
                    }
                }
            }
            if (currentTime - preTimeExtractArm > 500 && isButtonClicked || preExtractMove != extractArmMove) {
                preTimeExtractArm = currentTime
                var msg: String
                if (isButtonClicked) {
                    isButtonClicked = false
                    msg = "arm\n$extractArmPos\n"
                } else {
                    preExtractMove = extractArmMove
                    msg="armmove\n$extractArmMove\n"
                }

                if ((dos == null).not()) {
                    try {
                        dos?.writeBytes(msg)
                        dos?.flush()
                    } catch(e: SocketException) {
                        Log.e("wifi", "Send Failed")
                    }
                }
            }
        }

    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            dos?.close()
            socket?.close()
        } catch(e: SocketException) {
            Log.e("wifi", "Disconnect Failed")
            dos = null
            socket = null
        }
    }
}

