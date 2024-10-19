package com.bit.wificonn

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MotorControlUI(
    upButtonAction: () ->Unit,
    downButtonAction: () -> Unit,
    stopButtonAction: () ->Unit,
    joystickMovedAction: (x: Float, y: Float, theta: Float) -> Unit = { _, _, _ -> },
    joystickStopAction: () -> Unit,
    disconnectButtonAction: () -> Unit,
    joystickOffsetX: Dp = 0.dp,
    joystickOffsetY: Dp = 0.dp,
    onSliderChanged: (Int) -> Unit,
    modifier: Modifier) {
    var sliderPos by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(0.dp, 50.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Button(
                onClick = upButtonAction
            ) {
                Text(
                    text = "Up"
                )
            }
            Button(
                onClick = downButtonAction
            ) {
                Text(
                    text = "Down"
                )
            }
            Button(
                onClick = stopButtonAction
            ) {
                Text(
                    text = "Stop"
                )
            }
            Button(
                onClick = disconnectButtonAction
            ) {
                Text(
                    text = "Disconnect"
                )
            }

        }
        Joystick(stickOffsetX = joystickOffsetX,
            stickOffsetY = joystickOffsetY,
            size = 120.dp,
            dotSize = 30.dp,
            movedAction = joystickMovedAction,
            stopAction = joystickStopAction)

        Slider(
            value = sliderPos,
            onValueChange = { sliderPos = it
                onSliderChanged(sliderPos.toInt()) },
            valueRange = 0f..100f,
            steps = 99,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .width(200.dp)
                .height(50.dp)
                .offset(x = -50.dp, y = 500.dp)

        )

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
        modifier: Modifier = Modifier
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

                            thetaDeg = atan2(y, x) * ( 180 / PI.toFloat() ) - 45f

                            Log.d("stick", "$x , $y , $thetaDeg")

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
