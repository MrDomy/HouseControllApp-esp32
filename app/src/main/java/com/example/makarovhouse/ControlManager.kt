package com.example.makarovhouse

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ControlUiState(
    val esp32Host: String = "192.168.4.1",
    val isConnected: Boolean = false,
    val isDoorOpen: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val isDiscoMode: Boolean = false,
    val isEmergencyMode: Boolean = false,
    val isSecurityMode: Boolean = false,
    val ledColors: List<String> = List(17) { "#000000" },
    val statusMessage: String? = null
)

class ControlManager : ViewModel() {
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    init {
        syncStatus() // Синхронизируем UI с ESP32 сразу при запуске
    }

    fun updateHost(host: String) {
        _uiState.update { it.copy(esp32Host = host) }
    }

    fun syncStatus() {
        val host = _uiState.value.esp32Host
        if (host.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { Esp32HttpClient.fetchStatus(host) }
                .onSuccess { response ->
                    try {
                        val json = JSONObject(response)
                        val doorState = json.optInt("door", 0) == 1
                        val flashlightState = json.optInt("flashlight", 0) == 1
                        val disco = json.optBoolean("disco", false)
                        val emergency = json.optBoolean("emergency", false)
                        val security = json.optBoolean("security", false)
                        val colorsArray = json.optJSONArray("colors")
                        val colorsList = MutableList(17) { "#000000" }
                        if (colorsArray != null) {
                            for (i in 0 until colorsArray.length()) {
                                if (i < 17) colorsList[i] = colorsArray.getString(i)
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isConnected = true,
                                isDoorOpen = doorState,
                                isFlashlightOn = flashlightState,
                                isDiscoMode = disco,
                                isEmergencyMode = emergency,
                                isSecurityMode = security,
                                ledColors = colorsList,
                                statusMessage = "Синхронизировано"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(statusMessage = "Ошибка JSON: ${e.message}") }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            statusMessage = error.message ?: "Ошибка сети"
                        )
                    }
                }
        }
    }

    fun setDoorState(open: Boolean) {
        val cmd = if (open) "DOOR:1" else "DOOR:0"
        sendCommand(cmd) {
            _uiState.update { state -> 
                state.copy(
                    isDoorOpen = open,
                    isSecurityMode = false,
                    isEmergencyMode = false
                ) 
            }
        }
    }

    fun setFlashlightState(on: Boolean) {
        val stateStr = if (on) "1" else "0"
        sendCommand("FLASHLIGHT:$stateStr") {
            _uiState.update { it.copy(isFlashlightOn = on) }
        }
    }

    fun setDiscoMode(enabled: Boolean) {
        val cmd = if (enabled) "MODE:DISCO" else "MODE:MANUAL"
        sendCommand(cmd) {
            _uiState.update { it.copy(isDiscoMode = enabled, isEmergencyMode = false, isSecurityMode = false) }
        }
    }

    fun setEmergencyMode(enabled: Boolean) {
        val cmd = if (enabled) "MODE:EMERGENCY" else "MODE:MANUAL"
        sendCommand(cmd) {
            _uiState.update { state -> 
                state.copy(
                    isEmergencyMode = enabled, 
                    isDiscoMode = false,
                    isSecurityMode = false,
                    isDoorOpen = if (enabled) false else state.isDoorOpen
                ) 
            }
        }
    }

    fun setSecurityMode(enabled: Boolean) {
        val cmd = if (enabled) "MODE:SECURITY" else "MODE:MANUAL"
        sendCommand(cmd) {
            _uiState.update { state -> 
                state.copy(
                    isSecurityMode = enabled, 
                    isDiscoMode = false,
                    isEmergencyMode = false,
                    isDoorOpen = if (enabled) true else state.isDoorOpen
                ) 
            }
        }
    }

    fun setAllLedsColor(colorHex: String) {
        sendCommand("LED:ALL:HEX:${colorHex.removePrefix("#")}") {
            _uiState.update {
                it.copy(
                    ledColors = List(17) { colorHex },
                    isDiscoMode = false
                )
            }
        }
    }

    fun setGroupColor(groupId: Int, colorHex: String) {
        sendCommand("LED:G$groupId:HEX:${colorHex.removePrefix("#")}") {
            _uiState.update { state ->
                val newColors = state.ledColors.toMutableList()
                val range = when (groupId) {
                    1 -> 0..5
                    2 -> 6..11
                    3 -> 12..16
                    else -> return@update state
                }
                for (i in range) {
                    newColors[i] = colorHex
                }
                state.copy(ledColors = newColors, isDiscoMode = false)
            }
        }
    }

    fun setLedColor(index: Int, colorHex: String) {
        sendCommand("LED:$index:HEX:${colorHex.removePrefix("#")}") {
            _uiState.update { state ->
                val newColors = state.ledColors.toMutableList()
                if (index in newColors.indices) {
                    newColors[index] = colorHex
                }
                state.copy(ledColors = newColors, isDiscoMode = false)
            }
        }
    }

    private fun sendCommand(command: String, onSuccessLocalUpdate: () -> Unit) {
        val host = _uiState.value.esp32Host
        if (host.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Укажите IP-адрес ESP32") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { Esp32HttpClient.sendCommand(host, command) }
                .onSuccess {
                    _uiState.update { it.copy(isConnected = true, statusMessage = "Команда отправлена: $command") }
                    onSuccessLocalUpdate()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            statusMessage = error.message ?: "Ошибка при отправке: $command"
                        )
                    }
                }
        }
    }
}
