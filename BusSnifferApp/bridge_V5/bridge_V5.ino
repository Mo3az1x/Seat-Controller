#include <Wire.h>

#define I2C_ADDR 0x50   // عنوان الـ STM32 (Slave address)

// Buffer
#define BUF_SIZE 64
char uartBuffer[BUF_SIZE];

void setup() {
  // UART with BusSniffer
  Serial.begin(115200);

  // I2C with STM32
  Wire.begin();  // Arduino Nano as I2C Master
}

void loop() {
  // ===== 1) Check data from BusSniffer (UART) and send to STM32 =====
  if (Serial.available()) {
    int len = Serial.readBytesUntil('\n', uartBuffer, BUF_SIZE - 1);
    uartBuffer[len] = '\0';

    // Send over I2C
    Wire.beginTransmission(I2C_ADDR);
    Wire.write((uint8_t*)uartBuffer, len);
    Wire.endTransmission();

    // Debug
    Serial.println(String(">> Sent to STM: ") + uartBuffer);
  }

  // ===== 2) Request data from STM32 and forward to BusSniffer =====
  Wire.requestFrom(I2C_ADDR, BUF_SIZE);
  int idx = 0;
  while (Wire.available()) {
    uartBuffer[idx++] = Wire.read();
  }
  if (idx > 0) {
    uartBuffer[idx] = '\0';
    Serial.println(String("<< From STM: ") + uartBuffer);
  }

  delay(50); // تخفيف الحمل على I2C
}
