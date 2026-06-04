#include <WiFi.h>
#include <WebServer.h>
#include <Adafruit_NeoPixel.h>

// --- Настройки сети ---
const char* AP_SSID = "MakarovHouse_ESP";
const char* AP_PASSWORD = "password123";

WebServer server(80);

// --- Настройки светодиодов ---
#define LED_PIN 18
#define LED_COUNT 17
Adafruit_NeoPixel strip(LED_COUNT, LED_PIN, NEO_GRB + NEO_KHZ800);

bool isSecurityMode = false;   // Флаг режима охраны

unsigned long lastDiscoTime = 0; // Таймер для дискотеки

uint32_t ledColors[LED_COUNT]; // Базовые цвета светодиодов

// --- Настройки поршня (L298N) ---
#define L298N_IN1 25
#define L298N_IN2 27

// --- Настройки реле (Фонарики) ---
#define RELAY_PIN 26
int flashlightState = 0; // 0 - Выкл, 1 - Вкл

// --- ИК Датчик ---
#define IR_SENSOR_PIN 19

// 0 - Закрыта, 1 - Открыта
int doorState = 0; 
bool isDiscoMode = false;
bool isEmergencyMode = false;
unsigned long lastEmergencyTime = 0;
bool emergencyBlinkState = false;

// Безопасность мотора двери
bool isDoorMoving = false;
unsigned long doorMoveStartTime = 0;
const unsigned long DOOR_MOVE_DURATION = 2500; // 2.5 секунды на движение поршня

void applyLeds() {
  if (isDiscoMode || isEmergencyMode || isSecurityMode) return; // Не переопределяем цвета извне
  for (int i = 0; i < LED_COUNT; i++) {
    strip.setPixelColor(i, ledColors[i]);
  }
  strip.show();
}

void applyDoorState() {
  if (doorState == 1) {
    digitalWrite(L298N_IN1, HIGH);
    digitalWrite(L298N_IN2, LOW);
  } else if (doorState == 0) {
    digitalWrite(L298N_IN1, LOW);
    digitalWrite(L298N_IN2, HIGH);
  } else {
    digitalWrite(L298N_IN1, LOW);
    digitalWrite(L298N_IN2, LOW);
  }
  
  // Включаем защитный таймер (чтобы мотор не сгорел)
  isDoorMoving = true;
  doorMoveStartTime = millis();
}

// Парсинг HEX, например: #FF0000 -> 16711680 (uint32_t)
uint32_t parseHex(String hexStr) {
  if (hexStr.startsWith("#")) {
    hexStr = hexStr.substring(1);
  }
  return (uint32_t) strtol(hexStr.c_str(), NULL, 16);
}

void applyCommand(const String& cmd) {
  if (cmd == "MODE:DISCO") {
    isDiscoMode = true;
    isEmergencyMode = false;
    isSecurityMode = false;
    return;
  } else if (cmd == "MODE:EMERGENCY") {
    isEmergencyMode = true;
    isDiscoMode = false;
    isSecurityMode = false;
    doorState = 0; // Закрыть дверь
    applyDoorState();
    return;
  } else if (cmd == "MODE:SECURITY") {
    isSecurityMode = true;
    isEmergencyMode = false;
    isDiscoMode = false;
    doorState = 1; // Открыть дверь
    applyDoorState();
    // Включаем все светодиоды зеленым цветом
    for (int i = 0; i < LED_COUNT; i++) {
      strip.setPixelColor(i, strip.Color(0, 255, 0));
    }
    strip.show();
    return;
  } else if (cmd == "MODE:MANUAL") {
    isDiscoMode = false;
    isEmergencyMode = false;
    isSecurityMode = false;
    applyLeds();
    return;
  }

  if (cmd.startsWith("DOOR:")) {
    doorState = cmd.substring(5).toInt();
    applyDoorState();
    // Отключаем охрану и тревогу при ручном управлении дверью
    if (isSecurityMode || isEmergencyMode) {
      isSecurityMode = false;
      isEmergencyMode = false;
      applyLeds();
    }
  } 
  else if (cmd.startsWith("FLASHLIGHT:")) {
    flashlightState = cmd.substring(11).toInt();
    digitalWrite(RELAY_PIN, flashlightState == 1 ? LOW : HIGH); // Инвертированная логика
  }
  else if (cmd.startsWith("LED:ALL:HEX:")) {
    uint32_t color = parseHex(cmd.substring(12));
    for (int i = 0; i < LED_COUNT; i++) {
      ledColors[i] = color;
    }
    isDiscoMode = false;
    applyLeds();
  }
  else if (cmd.startsWith("LED:G")) {
    // LED:G1:HEX:FF0000
    int firstColon = cmd.indexOf(':');
    int secondColon = cmd.indexOf(':', firstColon + 1);
    
    if (firstColon > 0 && secondColon > 0) {
      String groupStr = cmd.substring(firstColon + 1, secondColon); // G1, G2, G3
      String hexStr = cmd.substring(cmd.lastIndexOf(':') + 1);
      uint32_t color = parseHex(hexStr);

      int startIdx = 0;
      int endIdx = 0;
      if (groupStr == "G1") { startIdx = 0; endIdx = 6; } // 0..5
      else if (groupStr == "G2") { startIdx = 6; endIdx = 12; } // 6..11
      else if (groupStr == "G3") { startIdx = 12; endIdx = 17; } // 12..16

      for (int i = startIdx; i < endIdx; i++) {
        ledColors[i] = color;
      }
      isDiscoMode = false;
      applyLeds();
    }
  }
  else if (cmd.startsWith("LED:")) {
    // LED:5:HEX:FF0000
    int firstColon = cmd.indexOf(':');
    int secondColon = cmd.indexOf(':', firstColon + 1);
    
    if (firstColon > 0 && secondColon > 0) {
      int index = cmd.substring(firstColon + 1, secondColon).toInt();
      String hexStr = cmd.substring(cmd.lastIndexOf(':') + 1);
      
      uint32_t color = parseHex(hexStr);
      if (index >= 0 && index < LED_COUNT) {
        ledColors[index] = color;
        isDiscoMode = false;
        applyLeds();
      }
    }
  }
}

void handlePing() {
  server.send(200, "text/plain", "OK");
}

void handleCommand() {
  if (!server.hasArg("value")) {
    server.send(400, "text/plain", "Missing value");
    return;
  }
  String command = server.arg("value");
  applyCommand(command);
  server.send(200, "text/plain", command);
}

void handleStatus() {
  String json = "{";
  json += "\"door\":" + String(doorState) + ",";
  json += "\"flashlight\":" + String(flashlightState) + ",";
  json += "\"disco\":" + String(isDiscoMode ? "true" : "false") + ",";
  json += "\"emergency\":" + String(isEmergencyMode ? "true" : "false") + ",";
  json += "\"security\":" + String(isSecurityMode ? "true" : "false") + ",";
  json += "\"colors\":[";
  for (int i = 0; i < LED_COUNT; i++) {
    char hex[16];
    sprintf(hex, "\"#%06X\"", ledColors[i] & 0xFFFFFF);
    json += String(hex);
    if (i < LED_COUNT - 1) json += ",";
  }
  json += "]}";
  server.send(200, "application/json", json);
}

void setup() {
  Serial.begin(115200);
  
  pinMode(L298N_IN1, OUTPUT);
  pinMode(L298N_IN2, OUTPUT);
  pinMode(RELAY_PIN, OUTPUT);
  pinMode(IR_SENSOR_PIN, INPUT_PULLUP);
  
  digitalWrite(L298N_IN1, LOW);
  digitalWrite(L298N_IN2, LOW);
  digitalWrite(RELAY_PIN, HIGH); // Выключаем реле по умолчанию (обратная логика) // Выключаем реле при старте

  strip.begin();
  strip.setBrightness(60); // Ограничение яркости (около 25%) для предотвращения перезагрузок ESP32 по питанию (Brownout)
  strip.show();
  
  for (int i = 0; i < LED_COUNT; i++) {
    ledColors[i] = 0;
  }

  WiFi.softAP(AP_SSID, AP_PASSWORD);
  IPAddress IP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(IP);

  server.on("/ping", HTTP_GET, handlePing);
  server.on("/command", HTTP_GET, handleCommand);
  server.on("/status", HTTP_GET, handleStatus);
  
  server.begin();
}

void loop() {
  server.handleClient();
  
  // Защитное отключение мотора двери через 2.5 секунды
  if (isDoorMoving && (millis() - doorMoveStartTime > DOOR_MOVE_DURATION)) {
    digitalWrite(L298N_IN1, LOW);
    digitalWrite(L298N_IN2, LOW);
    isDoorMoving = false;
  }

  // Проверка ИК-датчика
  // Если датчик срабатывает (LOW) И включен режим охраны:
  if (isSecurityMode && digitalRead(IR_SENSOR_PIN) == LOW) {
    // Включаем аварийный режим
    isSecurityMode = false;
    isEmergencyMode = true;
    doorState = 0;
    applyDoorState();
  }

  if (isDiscoMode) {
    if (millis() - lastDiscoTime > 150) { // Мигание каждые 150мс
      lastDiscoTime = millis();
      for (int i = 0; i < LED_COUNT; i++) {
        uint8_t r = random(0, 256);
        uint8_t g = random(0, 256);
        uint8_t b = random(0, 256);
        
        // Делаем цвета яркими и насыщенными
        int c = random(0, 3);
        if (c == 0) r = 255;
        else if (c == 1) g = 255;
        else b = 255;

        strip.setPixelColor(i, strip.Color(r, g, b));
      }
      strip.show();
    }
  } else if (isEmergencyMode) {
    if (millis() - lastEmergencyTime > 150) { // Мигание каждые 200мс
      lastEmergencyTime = millis();
      emergencyBlinkState = !emergencyBlinkState;
      uint32_t color = emergencyBlinkState ? strip.Color(255, 0, 0) : strip.Color(0, 0, 0);
      for (int i = 0; i < LED_COUNT; i++) {
        strip.setPixelColor(i, color);
      }
      strip.show();
    }
  }
}
