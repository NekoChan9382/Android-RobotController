package com.bit.wificonn

import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bit.wificonn.ui.theme.WificonnTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bit.wificonn.ConnectionActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AppScreen() {
    Connect,
    Control
}

enum class WifiState() {
    Null,
    Connect,
    Up,
    Down,
    Stop,
    JoystickMoved,
    Disconnect
}

var stickX = 0
var stickY = 0
var slider = 0
var button = 0
var thetas = 0
var sendLoop = false

var connections: ConnectionActivity? = null


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wifiManager: WifiManager =
            applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        enableEdgeToEdge()
        setContent {
            WificonnTheme {

                RobotController(wifiManager)
            }
        }
    }
}

@Composable
fun RobotController(
    wifiManager: WifiManager,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    var isWifiEnabled = true

    if (!wifiManager.isWifiEnabled) {
        isWifiEnabled = false
    }

    var state by remember { mutableIntStateOf(WifiState.Null.ordinal) }
    var ip_ by remember { mutableStateOf("") }
    var port_ by remember { mutableIntStateOf(25655) }
    var preTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var joystickX by remember { mutableFloatStateOf(0f) }
    var joystickY by remember { mutableFloatStateOf(0f) }

    var sliderPos by remember { mutableIntStateOf(0) }

    var isDialogVisible by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = AppScreen.Connect.name,
        modifier = Modifier
    ) {
        composable(route = AppScreen.Connect.name) {
            Buttons(
                isWifiEnabled = isWifiEnabled,
                onConnect = { ip: String, port: String ->
                    state = WifiState.Connect.ordinal
                    ip_ = ip
                    port_ = port.toInt()

                },
                isDialogVisible = isDialogVisible,
                isDialogVisibleAc = { a: Boolean -> isDialogVisible = a }
            )
        }
        composable(route = AppScreen.Control.name) {
            MotorControlUI(
                upButtonAction = { state = WifiState.Up.ordinal },
                downButtonAction = { state = WifiState.Down.ordinal },
                stopButtonAction = { state = WifiState.Stop.ordinal },
                joystickMovedAction = { x: Float, y: Float, theta: Float ->
                    if (abs(x) > 0.05 && abs(y) > 0.05) {

                        stickX = (x * 3).roundToInt()
                        stickY = (y * 3).roundToInt()
                        thetas = theta.roundToInt()
                        Log.d("stick", "$thetas")

                    }
                },
                joystickStopAction = {
                    stickX = 0
                    stickY = 0
                    thetas = 5
                },
                disconnectButtonAction = { state = WifiState.Disconnect.ordinal },
                joystickOffsetX = 50.dp,
                joystickOffsetY = 100.dp,
                onSliderChanged = { pos: Int ->
                    slider = pos
                },
                modifier = Modifier
            )
        }
    }
    when (state) {
        WifiState.Connect.ordinal -> {
            Log.d("conn", "$connections")
            connections = connectToEsp(ip_, port_, navController, { isDialogVisible = true })

//            while(connections == null) {}
            Log.d("conn2", "$connections")
            sendLoop = true
            Log.d("conn", "did")
            SendLoop(connections)

        }

        WifiState.Up.ordinal, WifiState.Down.ordinal, WifiState.JoystickMoved.ordinal, WifiState.Stop.ordinal -> {
            val nowTime = System.currentTimeMillis()
            Log.d("sock", "W")
            if (nowTime - preTime > 50L) {
                preTime = nowTime
                SendToEsp(
                    connections,
                    state,
                    joystickX,
                    joystickY,
                    sliderPos
                )

            }

        }

        WifiState.Disconnect.ordinal -> {
            sendLoop = false
            Disconnect(connections, navController)
        }
    }
    state = WifiState.Null.ordinal

}

@Composable
fun Buttons(
    isWifiEnabled: Boolean,
    onConnect: (String, String) -> Unit,
    isDialogVisible: Boolean = false,
    isDialogVisibleAc: (Boolean) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    var connectButtonText = "Connect"
    val focus = LocalFocusManager.current

    if (!isWifiEnabled) {
        connectButtonText = "Wifi Disabled"
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            var enteredIp by remember { mutableStateOf("") }
            var enteredPort by remember { mutableStateOf("25655") }

            OutlinedTextField(
                value = enteredIp,
                onValueChange = { enteredIp = it },
                modifier = Modifier.padding(10.dp),
                placeholder = { Text(text = "Enter IP Address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focus.moveFocus(FocusDirection.Down) }
                )
            )
            OutlinedTextField(
                value = enteredPort,
                onValueChange = { enteredPort = it },
                modifier = Modifier.padding(10.dp),
                placeholder = { Text(text = "Enter Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focus.clearFocus()
                        if (isWifiEnabled) onConnect(enteredIp, enteredPort)
                    }
                )
            )
            Button(
                onClick = { onConnect(enteredIp, enteredPort) },
                enabled = isWifiEnabled,
            ) {
                Text(
                    text = connectButtonText

                )
            }
        }
        if (isDialogVisible) {
            Image(
                painterResource(R.drawable.connfail),
                contentDescription = null,
                modifier = Modifier.offset(300.dp, 350.dp)

            )
            LaunchedEffect(Unit) {
                coroutineScope {
                    connectionErrorDialogue(visible = isDialogVisibleAc)
                }
            }
        }
    }
}

suspend fun connectionErrorDialogue(
    modifier: Modifier = Modifier,
    visible: (Boolean) -> Unit
) {
    visible(true)
    delay(2000L)
    Log.d("error", "del")
    visible(false)

}


fun connectToEsp(
    ipAddress: String,
    portNumber: Int,
    nav: NavHostController,
    connFailed: () -> Unit,
    modifier: Modifier = Modifier
): ConnectionActivity {
    val conn = ConnectionActivity(ipAddress, portNumber)
    runBlocking {


            val result = kotlin.runCatching { conn.connect() }
            when (val exception = result.exceptionOrNull()) {
                is java.net.ConnectException -> {
                    Log.e("socket", "Connection Failed")
                    connFailed()
                }

                is java.net.UnknownHostException -> {
                    Log.e("socket", "Connection Failed")
                    connFailed()
                }


                else -> {
                    nav.navigate(AppScreen.Control.name)
                    Log.d("socket", "Succeeded")
                    /*if (result.isSuccess) {
                        nav.navigate(AppScreen.Control.name)
                        Log.d("socket", "Succeeded")

                    } else {
                        Log.e("socket", "Unknown Error: $exception")
                    }*/


            }
        }
    }
    return conn
}

@Composable
fun SendLoop(
    conn: ConnectionActivity?
) {
    Log.d("main", "launch")
    LaunchedEffect(Unit) {
        coroutineScope {
            conn?.sendLoop()
        }
    }
}

@Composable
fun SendToEsp(
    conn: ConnectionActivity?,
    sendNum: Int,
    joystickX: Float = 0f,
    joystickY: Float = 0f,
    sliderPos: Int = 0,
    modifier: Modifier = Modifier
) {
    var msg = ""
    val sendX = (joystickX * 100).roundToInt()
    val sendY = (joystickY * 100).roundToInt()
    when (sendNum) {
        WifiState.Up.ordinal -> msg = "up\n"
        WifiState.Down.ordinal -> msg = "down\n"
        WifiState.JoystickMoved.ordinal -> msg = "stick\n$sendX\n$sendY\nslider\n$sliderPos\n"
        WifiState.Stop.ordinal -> msg = "stop\n"
    }

    Log.d("socket", "Send: $msg")

    LaunchedEffect(Unit) {
        coroutineScope {
            try {
                conn?.sendToEsp(msg)
            } catch (e: java.net.SocketException) {
                Log.e("socket", "Send failed")
            }
        }
    }
}

@Composable
fun Disconnect(conn: ConnectionActivity?, nav: NavHostController, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                conn?.disconnect()
                nav.popBackStack(AppScreen.Connect.name, inclusive = false)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WificonnTheme {

    }
}

