package com.bit.wificonn

import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
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
import com.bit.wificonn.ui.theme.WificonnTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AppScreen {
    Connect,
    Control
}

enum class WifiState {
    Null,
    Connect,
    Disconnect
}

enum class ClickedButton {
    Top,
    Middle,
    Bottom,
    Floor
}

var slider = 0
var thetas = 0
var sendLoop = false
var extractArmPos = 0
var isButtonClicked = false

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
) {
    var isWifiEnabled = true

    if (!wifiManager.isWifiEnabled) {
        isWifiEnabled = false
    }

    var state by remember { mutableIntStateOf(WifiState.Null.ordinal) }
    var ipAddress by remember { mutableStateOf("") }
    var portNum by remember { mutableIntStateOf(25655) }

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
                    ipAddress = ip
                    portNum = port.toInt()

                },
                isDialogVisible = isDialogVisible,
                isDialogVisibleAction = { a: Boolean -> isDialogVisible = a }
            )
        }
        composable(route = AppScreen.Control.name) {
            MotorControlUI(
                extractArmButtonAction = {},
                joystickMovedAction = { x: Float, y: Float, theta: Float ->
                    if (abs(x) > 0.2 || abs(y) > 0.2) {
                        Log.d("stick", "$theta")

                        thetas = (theta * 0.022).toInt() + 1
                        Log.d("stick", "$thetas")
                    }
                },
                joystickStopAction = {
                    thetas = 0
                },
                disconnectButtonAction = { state = WifiState.Disconnect.ordinal },
                joystickOffsetX = 50.dp,
                joystickOffsetY = 100.dp,
            )
        }
    }
    when (state) {
        WifiState.Connect.ordinal -> {

            connections = connectToEsp(ipAddress, portNum, navController){ isDialogVisible = true }
            sendLoop = true
            SendLoop(connections)
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
    isDialogVisibleAction: (Boolean) -> Unit = { _ -> },
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
                    connectionErrorDialogue(visible = isDialogVisibleAction)
                }
            }
        }
    }
}

suspend fun connectionErrorDialogue(
    visible: (Boolean) -> Unit
) {
    visible(true)
    delay(2000L)
    visible(false)

}


fun connectToEsp(
    ipAddress: String,
    portNumber: Int,
    nav: NavHostController,
    connFailed: () -> Unit
): ConnectionActivity {
    val conn = ConnectionActivity(ipAddress, portNumber)
    runBlocking {


            val result = kotlin.runCatching { conn.connect() }
            when (result.exceptionOrNull()) {
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
    LaunchedEffect(Unit) {
        coroutineScope {
            conn?.sendLoop()
        }
    }
}

@Composable
fun Disconnect(conn: ConnectionActivity?, nav: NavHostController) {
    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                conn?.disconnect()
                nav.popBackStack(AppScreen.Connect.name, inclusive = false)
            }
        }
    }
}
