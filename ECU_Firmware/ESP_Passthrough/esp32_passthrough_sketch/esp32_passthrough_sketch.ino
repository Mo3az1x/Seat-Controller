#include <HardwareSerial.h>

HardwareSerial espSerial(2); // RX, TX

void setup() {
  Serial.begin(9600);       // USB serial
  espSerial.begin(9600, SERIAL_8N1, 16, 17);    // To ESP32
}

void loop() 
{
  while (Serial.available()) 
  {
    espSerial.write(Serial.read());
  }

  while (espSerial.available()) 
  {
    Serial.write(espSerial.read());
  }
}