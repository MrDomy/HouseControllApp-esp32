package com.example.makarovhouse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.makarovhouse.ui.theme.BrandBlack

@Composable
fun ColorWheel(onColorSelected: (String) -> Unit) {
    val rainbowColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                var lastSendTime = 0L
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            val maxDist = size.width / 2f
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            
                            val clampedPos = if (dist <= maxDist) {
                                change.position
                            } else {
                                val nx = dx / dist
                                val ny = dy / dist
                                Offset(cx + nx * maxDist, cy + ny * maxDist)
                            }
                            
                            touchPosition = clampedPos
                            val now = System.currentTimeMillis()
                            if (now - lastSendTime > 100) {
                                val colorHex = calculateColorFromOffset(clampedPos, size.width.toFloat())
                                onColorSelected(colorHex)
                                lastSendTime = now
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(brush = Brush.sweepGradient(rainbowColors))
            drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color.Transparent)), radius = size.width / 3f)
            
            // Четко выделенная зона выключения в самом центре
            drawCircle(color = BrandBlack, radius = size.width / 12f)
            
            touchPosition?.let { pos ->
                drawCircle(
                    color = Color.Black,
                    radius = 20f,
                    center = pos,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 20f,
                    center = pos,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }
        }
    }
}

fun calculateColorFromOffset(offset: Offset, size: Float): String {
    val cx = size / 2f
    val cy = size / 2f
    val dx = offset.x - cx
    val dy = offset.y - cy
    
    val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
    var degree = Math.toDegrees(angle).toFloat()
    if (degree < 0) degree += 360f
    
    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    val maxDist = size / 2f
    var saturation = dist / maxDist
    if (saturation > 1f) saturation = 1f
    
    // Если нажали в самый центр - выключаем (черный цвет)
    if (dist < size / 6f) {
        return "#000000"
    }
    
    val hsvColor = android.graphics.Color.HSVToColor(floatArrayOf(degree, saturation, 1f))
    return String.format("#%06X", 0xFFFFFF and hsvColor)
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Black
    }
}
