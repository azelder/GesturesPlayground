package com.azelder.gestureplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.azelder.gestureplayground.ui.theme.GesturePlaygroundTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GesturePlaygroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GesturesPractice(innerPaddingValues = innerPadding)
                }
            }
        }
    }
}

@Composable
fun GesturesPractice(
    modifier: Modifier = Modifier,
    innerPaddingValues: PaddingValues
) {
    // the backdrop for the screen
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(innerPaddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        item {
            TransformablePractice()
            Spacer(modifier = Modifier.padding(16.dp))
        }
        item {
            Tilt2Dto3D()
            Spacer(modifier = Modifier.padding(16.dp))
        }
        item {
            GestureTransform3D()
            Spacer(modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
fun GesturesPracticePreview() {
    GesturesPractice(innerPaddingValues = PaddingValues(0.dp))
}

@Composable
private fun TransformablePractice() {
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += offsetChange
    }
    // the object to manipulate
    Box(
        modifier = Modifier
            .padding(16.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotation,
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = state)
            .background(Color.Green)
            .size(200.dp),
        contentAlignment = Alignment.Center

    ) {
        Text(
            text = "Watch me transform!",
            color = Color.Black
        )
    }
}

@Composable
fun Tilt2Dto3D() {
    var is3D by remember { mutableStateOf(false) }

    // Animate tilt transition (0° for 2D, 60° for 3D)
    val rotationX by animateFloatAsState(targetValue = if (is3D) 60f else 0f, label = "rotationX")

    // Camera distance (higher value for realistic 3D effect)
    val cameraDistance = 8f * LocalDensity.current.density

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 3D Transformable Box
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer(
                    rotationX = rotationX, // Tilt effect
                    cameraDistance = cameraDistance
                )
                .background(Color.Blue)
                .clickable { is3D = !is3D }, // Toggle 2D/3D
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (is3D) "3D Click Me" else "2D Click Me",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun Tilt2Dto3DPreview() {
    Tilt2Dto3D()
}

/**
 * Using a couple different references and sticking them together to animate a view that rotates
 * over itself with animation based decay (similar physics to swipe to delete or "Fling"
 *
 * https://github.com/android/snippets/blob/d2ccac0e57f635b49aea57804c3ff6ab3ddafd15/compose/snippets/src/main/java/com/example/compose/snippets/animations/AdvancedAnimationSnippets.kt#L94-L152
 * https://developer.android.com/reference/kotlin/androidx/compose/animation/core/Animatable
 */
@Composable
fun GestureTransform3D() {
    val rotationDegree = remember { Animatable(0f) }
    val decay = splineBasedDecay<Float>(LocalDensity.current)
    val cameraDistance = 8f * LocalDensity.current.density
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeight.toPx() }

    Box(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp)
            .pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        val velocityTracker = VelocityTracker()
                        // Stop any ongoing animation.
                        rotationDegree.stop()
                        awaitPointerEventScope {
                            // Detect a touch down event.
                            val pointerId = awaitFirstDown().id

                            verticalDrag(pointerId) { change ->
                                // Update the animation value with touch events
                                launch {
                                    rotationDegree.snapTo(
                                        rotationDegree.value +
                                                ((change.previousPosition.y - change.position.y) / screenHeightPx) * 360
                                    )
                                }
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                )
                            }
                        }
                        // No longer receiving touch events. Prepare the animation.
                        val velocity = velocityTracker.calculateVelocity().y
                        launch {
                            rotationDegree.animateDecay(velocity, decay)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer(
                    rotationX = rotationDegree.value,  // Tilt effect
                    cameraDistance = cameraDistance
                )
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Spin me!",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview
@Composable
fun GestureTransform3DPreview() {
    GestureTransform3D()
}