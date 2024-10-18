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

    suspend fun sendLoop()  = withContext(Dispatchers.IO){

        var preTimeStick = System.currentTimeMillis()
        var preTimeSlider = preTimeStick
        var stickChanged = false
        var sliderChanged = false
        var preStickX = 0
        var preStickY = 0
        var preSlider = 0
        var preTheta = 0

        while(sendLoop) {
            val currentTime = System.currentTimeMillis()

            if (/*preStickX != stickX || preStickY != stickY*/preTheta != thetas) {
                preStickX = stickX
                preStickY = stickY
                preTheta = thetas
                stickChanged = true
            }
            if (preSlider != slider) {
                preSlider = slider
                sliderChanged = true
            }
            if (currentTime - preTimeStick > 10 && stickChanged) {
                preTimeStick = currentTime
                stickChanged = false
                val msg = "stick\n$thetas\n"
                Log.d("wifi", "send: $msg")
                if ((dos == null).not()) {
                    dos?.writeBytes(msg)
                    dos?.flush()
                }
            }
            if (currentTime - preTimeSlider > 500 && sliderChanged) {
                preTimeSlider = currentTime
                sliderChanged = false
                val msg = "slider\n$slider\n"
                Log.d("wifi", "send: $msg")
                if ((dos == null).not()) {
                    dos?.writeBytes(msg)
                    dos?.flush()
                }
            }
        }

    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        dos?.close()
        socket?.close()
    }
}

