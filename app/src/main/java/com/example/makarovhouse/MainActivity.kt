package com.example.makarovhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.atan2

// --- Стилизация Learn Up (ЧБ) ---
val BrandBlack = Color(0xFF111111)
val BrandWhite = Color(0xFFFFFFFF)
val BrandGrayLight = Color(0xFFF5F5F5)
val BrandGrayDark = Color(0xFF555555)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = BrandBlack,
                    onPrimary = BrandWhite,
                    background = BrandGrayLight,
                    surface = BrandWhite,
                    onSurface = BrandBlack
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MakarovHouseApp()
                }
            }
        }
    }
}

@Composable
fun MakarovHouseApp(controlManager: ControlManager = viewModel()) {
    val uiState by controlManager.uiState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val pastelGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFB3BA), Color(0xFFBAFFC9), Color(0xFFBAE1FF)),
        start = Offset(0f, animatedOffset),
        end = Offset(1000f, animatedOffset + 500f)
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Controls", "Scenarios")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab, containerColor = BrandWhite, contentColor = BrandBlack) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    selectedContentColor = BrandBlack,
                    unselectedContentColor = BrandGrayDark
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Заголовок и статус (Learn Up style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("makarov house", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (uiState.isConnected) "Connected" else "Disconnected",
                        color = if (uiState.isConnected) Color(0xFF4CAF50) else BrandGrayDark,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandWhite)
                        .clickable { controlManager.syncStatus() }
                        .padding(12.dp)
                ) {
                    Text("Sync", fontWeight = FontWeight.Bold)
                }
            }

            if (selectedTab == 0) {
                // IP Адрес
                OutlinedTextField(
                    value = uiState.esp32Host,
                    onValueChange = { controlManager.updateHost(it) },
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlack,
                        unfocusedBorderColor = BrandGrayDark
                    )
                )

                // Дверь
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                        Text("Piston Door", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { controlManager.setDoorState(true) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isDoorOpen) BrandBlack else BrandGrayLight,
                                    contentColor = if (uiState.isDoorOpen) BrandWhite else BrandBlack
                                )
                            ) {
                                Text("Open")
                            }
                            Button(
                                onClick = { controlManager.setDoorState(false) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!uiState.isDoorOpen) BrandBlack else BrandGrayLight,
                                    contentColor = if (!uiState.isDoorOpen) BrandWhite else BrandBlack
                                )
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }

                // Фонарики (Реле)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                        Text("Flashlights", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { controlManager.setFlashlightState(true) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isFlashlightOn) BrandBlack else BrandGrayLight,
                                    contentColor = if (uiState.isFlashlightOn) BrandWhite else BrandBlack
                                )
                            ) {
                                Text("ON")
                            }
                            Button(
                                onClick = { controlManager.setFlashlightState(false) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!uiState.isFlashlightOn) BrandBlack else BrandGrayLight,
                                    contentColor = if (!uiState.isFlashlightOn) BrandWhite else BrandBlack
                                )
                            ) {
                                Text("OFF")
                            }
                        }
                    }
                }

                // Группы
                Text("LED Groups", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupButton("G1 (1-6)", Modifier.weight(1f)) { showColorPickerForGroup(1, controlManager) }
                    GroupButton("G2 (7-12)", Modifier.weight(1f)) { showColorPickerForGroup(2, controlManager) }
                    GroupButton("G3 (13-17)", Modifier.weight(1f)) { showColorPickerForGroup(3, controlManager) }
                }

                // Индивидуальные
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Individual LEDs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Turn OFF", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            controlManager.setAllLedsColor("#000000")
                        }.padding(8.dp))
                        Text("All Color", color = BrandGrayDark, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            showColorPickerForAll(controlManager)
                        }.padding(8.dp))
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp),
                    userScrollEnabled = false
                ) {
                    items(17) { index ->
                        val hex = uiState.ledColors.getOrElse(index) { "#000000" }
                        val color = parseHexColor(hex)
                        LedItemView(index, color) {
                            showColorPickerForLed(index, controlManager)
                        }
                    }
                }
            } else {
                // Вкладка Scenarios
                Text("Scenarios", fontWeight = FontWeight.Bold, fontSize = 22.sp)

                // Режим Дискотеки
                val discoModifier = if (uiState.isDiscoMode) {
                    Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(20.dp)).background(pastelGradient)
                } else {
                    Modifier.fillMaxWidth().height(80.dp)
                }
                Button(
                    onClick = { controlManager.setDiscoMode(!uiState.isDiscoMode) },
                    modifier = discoModifier,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isDiscoMode) Color.Transparent else BrandBlack,
                        contentColor = if (uiState.isDiscoMode) BrandBlack else BrandWhite
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (uiState.isDiscoMode) "DISCO: ON" else "START DISCO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Security Mode
                Button(
                    onClick = { controlManager.setSecurityMode(!uiState.isSecurityMode) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isSecurityMode) BrandBlack else BrandWhite,
                        contentColor = if (uiState.isSecurityMode) BrandWhite else BrandBlack
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BrandBlack)
                ) {
                    Text(if (uiState.isSecurityMode) "SECURITY: ARMED" else "ARM SECURITY", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Emergency Mode
                Button(
                    onClick = { controlManager.setEmergencyMode(!uiState.isEmergencyMode) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isEmergencyMode) Color.Red else BrandWhite,
                        contentColor = if (uiState.isEmergencyMode) Color.White else Color.Red
                    ),
                    border = if (!uiState.isEmergencyMode) androidx.compose.foundation.BorderStroke(2.dp, Color.Red) else null
                ) {
                    Text(if (uiState.isEmergencyMode) "EMERGENCY: TRIGGERED" else "TRIGGER EMERGENCY", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            GlobalColorPickerDialog(controlManager)
        }
    }
}
@Composable
fun GroupButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(45.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandWhite, contentColor = BrandBlack),
        elevation = ButtonDefaults.buttonElevation(1.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun LedItemView(index: Int, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, BrandGrayLight, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val isLight = (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114) > 0.5
        Text(
            text = "${index + 1}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLight) BrandBlack else BrandWhite
        )
    }
}

// Глобальные переменные для диалога (в реальном приложении лучше вынести в State/ViewModel)
var pickerTarget by mutableStateOf<PickerTarget?>(null)

sealed class PickerTarget {
    data class Group(val groupId: Int) : PickerTarget()
    data class Led(val index: Int) : PickerTarget()
    object All : PickerTarget()
}

fun showColorPickerForGroup(groupId: Int, cm: ControlManager) { pickerTarget = PickerTarget.Group(groupId) }
fun showColorPickerForLed(index: Int, cm: ControlManager) { pickerTarget = PickerTarget.Led(index) }
fun showColorPickerForAll(cm: ControlManager) { pickerTarget = PickerTarget.All }

@Composable
fun GlobalColorPickerDialog(controlManager: ControlManager) {
    val target = pickerTarget ?: return
    
    AlertDialog(
        onDismissRequest = { pickerTarget = null },
        containerColor = BrandWhite,
        title = { 
            Text(when(target) {
                is PickerTarget.Group -> "Group ${target.groupId} Color"
                is PickerTarget.Led -> "LED ${target.index + 1} Color"
                is PickerTarget.All -> "All LEDs Color"
            }, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ColorWheel { selectedHex ->
                    when (target) {
                        is PickerTarget.Group -> controlManager.setGroupColor(target.groupId, selectedHex)
                        is PickerTarget.Led -> controlManager.setLedColor(target.index, selectedHex)
                        is PickerTarget.All -> controlManager.setAllLedsColor(selectedHex)
                    }
                }
                
                // Быстрый выкл
                Button(
                    onClick = {
                        when (target) {
                            is PickerTarget.Group -> controlManager.setGroupColor(target.groupId, "#000000")
                            is PickerTarget.Led -> controlManager.setLedColor(target.index, "#000000")
                            is PickerTarget.All -> controlManager.setAllLedsColor("#000000")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlack)
                ) {
                    Text("Turn Off")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { pickerTarget = null }) { Text("Close", color = BrandBlack, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun ColorWheel(onColorSelected: (String) -> Unit) {
    val rainbowColors = listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
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
                            touchPosition = change.position
                            val now = System.currentTimeMillis()
                            // Отправляем не чаще 10 раз в секунду, чтобы не повесить ESP32
                            if (now - lastSendTime > 100) {
                                val colorHex = calculateColorFromOffset(change.position, size.width.toFloat())
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
            // Белый центр
            drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color.Transparent)), radius = size.width / 3f)
            
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