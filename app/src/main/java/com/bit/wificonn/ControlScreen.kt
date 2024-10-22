package com.bit.wificonn

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bit.wificonn.ui.theme.WificonnTheme
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MotorControlUI(
    extractArmButtonAction: (c: Int) -> Unit = {},
    joystickMovedAction: (x: Float, y: Float, theta: Float) -> Unit = { _, _, _ -> },
    joystickStopAction: () -> Unit = {},
    disconnectButtonAction: () -> Unit = {},
    joystickOffsetX: Dp = 0.dp,
    joystickOffsetY: Dp = 0.dp,
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(0.dp, 50.dp)
    ) {
        Button(
            onClick = disconnectButtonAction,
        ) {
            Text(
                text = "Disconnect"
            )
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(250.dp)
                .offset(x = 500.dp)
        ) {
            Button(
                onClick = { extractArmButtonAction(ClickedButton.Top.ordinal) }
            ) {
                Text(
                    text = "Top"
                )
            }
            Button(
                onClick = { extractArmButtonAction(ClickedButton.Middle.ordinal) }
            ) {
                Text(
                    text = "Middle"
                )
            }
            Button(
                onClick = { extractArmButtonAction(ClickedButton.Bottom.ordinal) }
            ) {
                Text(
                    text = "Bottom"
                )
            }
            Button(
                onClick = { extractArmButtonAction(ClickedButton.Floor.ordinal) }
            ) {
                Text(
                    text = "Floor"
                )
            }
        }
        Joystick(stickOffsetX = joystickOffsetX,
            stickOffsetY = joystickOffsetY,
            size = 150.dp,
            dotSize = 50.dp,
            movedAction = joystickMovedAction,
            stopAction = joystickStopAction)
    }
}
    @Composable
   fun Joystick(
        size: Dp = 200.dp,
        dotSize: Dp = 50.dp,
        movedAction: (x: Float, y: Float, theta: Float) -> Unit = { _, _, _ -> },
        stopAction: () -> Unit = {},
        stickOffsetX: Dp = 0.dp,
        stickOffsetY: Dp = 0.dp,
    ) {
        val radius = with(LocalDensity.current) { (size / 2).toPx() }

        val centerX = with(LocalDensity.current) { ((size - dotSize) / 2).toPx() }
        val centerY = with(LocalDensity.current) { ((size - dotSize) / 2).toPx() }

        var offsetX by remember { mutableFloatStateOf(centerX) }
        var offsetY by remember { mutableFloatStateOf(centerY) }

        var currentRadius by remember { mutableFloatStateOf(0f) }
        var theta by remember { mutableFloatStateOf(0f) }
        var posX by remember { mutableFloatStateOf(0f) }
        var posY by remember { mutableFloatStateOf(0f) }

        var thetaDeg = 0f

        Box(
            modifier = Modifier
                .size(size)
                .offset(x = stickOffsetX, y = stickOffsetY),
        ) {
            Image(
                painterResource(R.drawable.joystick_background_1),
                contentDescription = "joystick_background",
                modifier = Modifier.size(size)
            )
            Image(
                painterResource(R.drawable.joystick_dot_1),
                contentDescription = "joystick_dot",
                modifier = Modifier
                    .size(dotSize)
                    .offset {
                        IntOffset(
                            x = (posX + centerX).roundToInt(),
                            y = (posY + centerY).roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = {
                            offsetX = centerX
                            offsetY = centerY
                            currentRadius = 0f
                            theta = 0f
                            posX = 0f
                            posY = 0f
                            stopAction()
                        }) { change: PointerInputChange, dragAmount: Offset ->
                            val x = offsetX + dragAmount.x - centerX
                            val y = offsetY + dragAmount.y - centerY

                            change.consume()

                            theta = if (x >= 0 && y >= 0) {
                                atan(y / x)
                            } else if (x < 0 && y >= 0) {
                                (Math.PI).toFloat() + atan(y / x)
                            } else if (x < 0 && y < 0) {
                                -(Math.PI).toFloat() + atan(y / x)
                            } else {
                                atan(y / x)
                            }

                            thetaDeg = theta * (180f / PI.toFloat()) -22.5f
                            if (thetaDeg < 0) thetaDeg += 360f


                            currentRadius = sqrt((x.pow(2)) + (y.pow(2)))

                            offsetX += dragAmount.x
                            offsetY += dragAmount.y

                            if (currentRadius > radius) {
                                polarToCartesian(radius, theta)
                            } else {
                                polarToCartesian(currentRadius, theta)
                            }
                                .apply {
                                    posX = first
                                    posY = second
                                }

                        }
                    }
                    .onGloballyPositioned { coordinates ->
                        movedAction(
                            (coordinates.positionInParent().x - centerX) / radius,
                            -(coordinates.positionInParent().y - centerY) / radius,
                            thetaDeg
                        )
                    }
            )
        }
    }


private fun polarToCartesian(radius: Float, theta: Float): Pair<Float, Float> =
    Pair(radius * cos(theta), radius * sin(theta))

@Preview
@Composable
fun PreviewControl() {
    WificonnTheme {
        MotorControlUI()
    }
}