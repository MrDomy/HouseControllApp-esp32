# Makarov House Control System 🏠💡

A complete Smart Home system consisting of an ESP32-based hardware controller and a beautiful, modern Android companion app built with Jetpack Compose (Learn Up aesthetics).

## Features ✨

### 📱 Android App (Jetpack Compose)
- **Modern UI**: Clean, minimalist black-and-white design (Learn Up style).
- **Tabbed Navigation**: Separate tabs for direct controls and global scenarios.
- **Real-time Color Picker**: Drag-and-drop color wheel for precise RGB LED control.
- **Piston Door Control**: Open and close doors remotely via L298N driver.
- **Relay/Flashlight Control**: Toggle external lighting on and off.
- **LED Grouping**: Control 17 LEDs individually, in groups, or all at once.

### 🔌 ESP32 Firmware (C++/Arduino)
- **Wi-Fi Access Point**: ESP32 acts as a standalone Wi-Fi AP (`MakarovHouse_ESP`).
- **RESTful API**: Receives commands via lightweight HTTP GET requests.
- **Global Scenarios**:
  - 🪩 **Disco Mode**: Random flashing vibrant colors across all LEDs.
  - 🚨 **Emergency Mode**: Fast-blinking red lights across the house.
  - 🛡️ **Security Mode**: House turns green. If the IR motion sensor (Pin 19) is triggered, it instantly shuts the door and activates Emergency Mode!

## Hardware Requirements 🛠️
- ESP32 Development Board
- WS2812B RGB LEDs (17 LEDs total)
- L298N Motor Driver (for Piston Door)
- 5V Relay Module (for Flashlights)
- IR Obstacle Avoidance Sensor (Motion detection for Security Mode)

## Wiring Guide 🔌
| Component | ESP32 Pin |
|-----------|-----------|
| **WS2812B LEDs** | `GPIO 18` |
| **L298N IN1** | `GPIO 25` |
| **L298N IN2** | `GPIO 27` |
| **Relay Module** | `GPIO 26` |
| **IR Sensor (OUT)**| `GPIO 19` |

## CI/CD Pipeline 🚀
This project is equipped with **GitHub Actions**. Upon every `push` or `pull_request` to the `main` branch, the pipeline will automatically:
1. Build and test the Android APK.
2. Ensure no build regressions are introduced.
