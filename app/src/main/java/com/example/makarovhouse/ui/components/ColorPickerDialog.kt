package com.example.makarovhouse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.makarovhouse.ControlManager
import com.example.makarovhouse.ui.theme.BrandBlack
import com.example.makarovhouse.ui.theme.BrandWhite

// Глобальные переменные для диалога (в реальном приложении лучше вынести в State/ViewModel)
var pickerTarget by mutableStateOf<PickerTarget?>(null)

sealed class PickerTarget {
    data class Group(val groupId: Int) : PickerTarget()
    data class Led(val index: Int) : PickerTarget()
    object All : PickerTarget()
}

fun showColorPickerForGroup(groupId: Int) { pickerTarget = PickerTarget.Group(groupId) }
fun showColorPickerForLed(index: Int) { pickerTarget = PickerTarget.Led(index) }
fun showColorPickerForAll() { pickerTarget = PickerTarget.All }

@Composable
fun GlobalColorPickerDialog(controlManager: ControlManager) {
    val target = pickerTarget ?: return
    
    AlertDialog(
        onDismissRequest = { pickerTarget = null },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = { 
            Text(when(target) {
                is PickerTarget.Group -> "Group ${target.groupId} Color"
                is PickerTarget.Led -> "LED ${target.index + 1} Color"
                is PickerTarget.All -> "All LEDs Color"
            }, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
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
            }
        },
        confirmButton = {
            TextButton(onClick = { pickerTarget = null }) { Text("Close", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
        }
    )
}
