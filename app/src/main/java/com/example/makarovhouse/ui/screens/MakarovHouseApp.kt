package com.example.makarovhouse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.makarovhouse.ControlManager
import com.example.makarovhouse.ui.components.GlobalColorPickerDialog
import com.example.makarovhouse.ui.components.GroupButton
import com.example.makarovhouse.ui.components.LedItemView
import com.example.makarovhouse.ui.components.parseHexColor
import com.example.makarovhouse.ui.components.showColorPickerForAll
import com.example.makarovhouse.ui.components.showColorPickerForGroup
import com.example.makarovhouse.ui.components.showColorPickerForLed
import com.example.makarovhouse.ui.theme.BrandBlack
import com.example.makarovhouse.ui.theme.BrandGrayDark
import com.example.makarovhouse.ui.theme.BrandGrayLight
import com.example.makarovhouse.ui.theme.BrandWhite
import kotlinx.coroutines.delay

@Composable
fun MakarovHouseApp(controlManager: ControlManager = viewModel()) {
    val uiState by controlManager.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        while (true) {
            controlManager.syncStatus()
            delay(2000)
        }
    }

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

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Controls", "Scenarios", "Settings")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("makarov house", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (uiState.isConnected) {
                    Text(
                        text = "Connected",
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "Connect to Wi-Fi: MakarovHouse_ESP\nPassword: password123",
                        color = BrandGrayDark,
                        fontSize = 14.sp
                    )
                }
            }

            if (selectedTab == 0) {
                // IP убран в настройки

                // Дверь
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark)
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
                                    containerColor = if (uiState.isDoorOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (uiState.isDoorOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = if (uiState.isDoorOpen) ButtonDefaults.buttonElevation(4.dp) else ButtonDefaults.buttonElevation(0.dp),
                                border = if (!uiState.isDoorOpen) androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark) else null
                            ) {
                                Text("Open")
                            }
                            Button(
                                onClick = { controlManager.setDoorState(false) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!uiState.isDoorOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (!uiState.isDoorOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = if (!uiState.isDoorOpen) ButtonDefaults.buttonElevation(4.dp) else ButtonDefaults.buttonElevation(0.dp),
                                border = if (uiState.isDoorOpen) androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark) else null
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }

                // Фонарики (Реле)
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark)
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
                                    containerColor = if (uiState.isFlashlightOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (uiState.isFlashlightOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = if (uiState.isFlashlightOn) ButtonDefaults.buttonElevation(4.dp) else ButtonDefaults.buttonElevation(0.dp),
                                border = if (!uiState.isFlashlightOn) androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark) else null
                            ) {
                                Text("ON")
                            }
                            Button(
                                onClick = { controlManager.setFlashlightState(false) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!uiState.isFlashlightOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (!uiState.isFlashlightOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = if (!uiState.isFlashlightOn) ButtonDefaults.buttonElevation(4.dp) else ButtonDefaults.buttonElevation(0.dp),
                                border = if (uiState.isFlashlightOn) androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark) else null
                            ) {
                                Text("OFF")
                            }
                        }
                    }
                }

                // Группы
                Text("LED Groups", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupButton("G1 (1-6)", Modifier.weight(1f)) { showColorPickerForGroup(1) }
                    GroupButton("G2 (7-12)", Modifier.weight(1f)) { showColorPickerForGroup(2) }
                    GroupButton("G3 (13-17)", Modifier.weight(1f)) { showColorPickerForGroup(3) }
                }

                // Индивидуальные
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Individual LEDs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Button(
                        onClick = { showColorPickerForAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandWhite, contentColor = BrandBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp),
                        elevation = ButtonDefaults.buttonElevation(1.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGrayDark)
                    ) {
                        Text("All Color", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            showColorPickerForLed(index)
                        }
                    }
                }
            } else if (selectedTab == 1) {
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
                        containerColor = if (uiState.isDiscoMode) Color.Transparent else MaterialTheme.colorScheme.surface,
                        contentColor = if (uiState.isDiscoMode) BrandBlack else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
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
                        containerColor = if (uiState.isSecurityMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (uiState.isSecurityMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (uiState.isSecurityMode) "SECURITY: ARMED" else "ARM SECURITY", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Emergency Mode
                Button(
                    onClick = { controlManager.setEmergencyMode(!uiState.isEmergencyMode) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isEmergencyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (uiState.isEmergencyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (uiState.isEmergencyMode) "EMERGENCY: TRIGGERED" else "TRIGGER EMERGENCY", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else if (selectedTab == 2) {
                // Вкладка Настройки
                Text("Settings", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                
                OutlinedTextField(
                    value = uiState.esp32Host,
                    onValueChange = { controlManager.updateHost(it) },
                    label = { Text("ESP32 IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BrandGrayDark
                    )
                )

                Button(
                    onClick = { controlManager.syncStatus() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Force Sync Connection", fontWeight = FontWeight.Bold)
                }
            }

            GlobalColorPickerDialog(controlManager)
        }
    }
}
