#include <SoftwareSerial.h>
SoftwareSerial espSerial(10, 11); // RX, TX

void setup() {
  Serial.begin(115200);       // USB serial
  espSerial.begin(115200);    // To ESP32
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