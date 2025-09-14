#include <Wire.h>
#define I2C_SLAVE_ADDR 0x08

void receiveEvent(int bytesReceived)
{
  Serial.print("Received: ");
  while (Wire.available())
  {
      byte b = Wire.read();
      Serial.print(b, HEX);
      Serial.print(" ");
  }
  Serial.println();
}

void requestEvent()
{
  while(Serial.available())
  {
    Wire.write(Serial.read());
  }
}

void setup()
{
  Serial.begin(9600);
  Wire.begin(I2C_SLAVE_ADDR);
  Wire.onReceive(receiveEvent);
  Wire.onRequest(requestEvent);
  Serial.println("ESP32 ready as I2C Slave...");
}

void loop()
{

}