#include <EEPROM.h>
#include <SPI.h>


// ==== Frame Constants ====
const byte HEADER = 0x7E;
const byte TAIL   = 0x7F;

// ==== Command IDs ====
const byte CMD_READ_BYTE     = 0x01;
const byte CMD_WRITE_BYTE    = 0x02;
const byte CMD_READ_ALL      = 0x03;
const byte CMD_WRITE_ALL     = 0x04;
const byte CMD_CLEAR         = 0x05;
const byte CMD_VERIFY        = 0x06;
const byte CMD_STARTUP       = 0x07;
const byte CMD_DIAGNOSIS     = 0x08;
const byte CMD_COLLAB        = 0x09;
const byte CMD_CALIB_READ    = 0x0A;
const byte CMD_CALIB_WRITE   = 0x0B;

// ==== Response IDs ====
const byte RES_ACK           = 0x10;
const byte RES_NACK          = 0x11;
const byte RES_READ_BYTE     = 0x81;
const byte RES_WRITE_BYTE    = 0x82;
const byte RES_READ_ALL      = 0x83;
const byte RES_WRITE_ALL     = 0x84;
const byte RES_INFO          = 0x90;
const byte RES_DIAGNOSIS     = 0x91;
const byte RES_COLLAB        = 0x92;
const byte RES_CALIB         = 0x93;
// ==== EEPROM SPI Basic Commands ====
#define CMD_READ   0x03   // SPI Read
#define CMD_WRITE  0x02   // SPI Write
// ==== Frame Buffer ====
const int MAX_FRAME_SIZE = 256;
byte frame[MAX_FRAME_SIZE];
int frameIndex = 0;

unsigned long lastByteTime = 0;
const unsigned long FRAME_TIMEOUT = 100; // ms

// ==== Diagnostics ====
byte lastError = 0x00; // 0 = no error


// ==== SPI variables ====
volatile int spiIndex = 0;
volatile int eepromAddr = 0;
volatile bool writeMode = false;


// ================= Setup =================
void setup() {
  Serial.begin(9600);
  delay(200);
  sendStartupInfo(); // Send EEPROM info at startup
  Serial.println("=== EEPROM Emulator (UART + SPI Slave) Started ===");
  // إعداد Arduino كـ SPI Slave
  pinMode(MISO, OUTPUT);
  SPI.begin();
  SPI.setBitOrder(MSBFIRST);
  SPI.setDataMode(SPI_MODE0);
  SPCR |= _BV(SPE) | _BV(SPIE); // Enable SPI + interrupt

}

// ================= Loop =================
void loop() {
  while (Serial.available()) {
    byte b = Serial.read();
    frame[frameIndex++] = b;
    lastByteTime = millis();

    if (b == TAIL && frameIndex > 0) {
      processFrame(frame, frameIndex);
      frameIndex = 0;
    }

    if (frameIndex >= MAX_FRAME_SIZE) frameIndex = 0;
  }

  if (frameIndex > 0 && (millis() - lastByteTime > FRAME_TIMEOUT)) {
    frameIndex = 0; // reset if timeout
  }
}
// ==== SPI ISR (Slave mode) ====
ISR (SPI_STC_vect) {
  byte c = SPDR;  // byte from STM32

  // First byte = Command
  if (spiIndex == 0) {
    if (c == CMD_WRITE) {
      writeMode = true;
      spiIndex = 1;
    } else if (c == CMD_READ) {
      writeMode = false;
      spiIndex = 1;
    } else {
      spiIndex = 0;
    }
    return;
  }

  // Address High
  if (spiIndex == 1) {
    eepromAddr = (c << 8);
    spiIndex++;
    return;
  }

  // Address Low
  if (spiIndex == 2) {
    eepromAddr |= c;
    spiIndex++;
    return;
  }

  // Write Mode
  if (writeMode) {
    EEPROM.write(eepromAddr, c);
    Serial.print("[SPI WRITE] addr=");
    Serial.print(eepromAddr);
    Serial.print(" val=0x");
    Serial.println(c, HEX);
    eepromAddr++;
    SPDR = 0xFF; // Dummy response
  } 
  // Read Mode
  else {
    byte val = EEPROM.read(eepromAddr);
    Serial.print("[SPI READ] addr=");
    Serial.print(eepromAddr);
    Serial.print(" val=0x");
    Serial.println(val, HEX);
    SPDR = val;  // return value
    eepromAddr++;
  }
}

// ================= Process Frame =================
// ==== UART Protocol  ====

void processFrame(byte *f, int len) {
  if (len < 7) return;
  if (f[0] != HEADER || f[len - 1] != TAIL) return;

  byte cmd = f[5];
  lastError = 0x00; // reset error

  switch (cmd) {
    case CMD_WRITE_BYTE: {
      if (len >= 12) {
        int addr = (f[6]<<24) | (f[7]<<16) | (f[8]<<8) | f[9];
        byte val = f[10];
        if (addr >= 0 && addr < EEPROM.length()) {
          EEPROM.write(addr, val);
          sendResponse(RES_WRITE_BYTE, &val, 1);
          Serial.print("[UART WRITE] addr=");
          Serial.print(addr);
          Serial.print(" val=0x");
          Serial.println(val, HEX);

        } else {
          lastError = 0x01; // Invalid address
         bool sendAck(false);
        }
      }
      break;
    }

    case CMD_READ_BYTE: {
      if (len >= 11) {
        int addr = (f[6]<<24) | (f[7]<<16) | (f[8]<<8) | f[9];
        byte val = 0;
        if (addr >= 0 && addr < EEPROM.length()) {
          byte val = EEPROM.read(addr);
          sendResponse(RES_READ_BYTE, &val, 1);
        } else {
          lastError = 0x01; // Invalid address
         bool sendAck(false);
        }
      }
      break;
    }

    case CMD_WRITE_ALL: {
      int dataLen = len - 7;
      for (int i = 0; i < dataLen; i++) {
        if (i < EEPROM.length()) EEPROM.write(i, f[6+i]);
      }
     bool sendAck(true);
      break;
    }

    case CMD_READ_ALL: {
      int size = (f[6] << 24) | (f[7] << 16) | (f[8] << 8) | f[9];
      if (size > EEPROM.length()) size = EEPROM.length();
      sendReadAll(size);
      break;
    }

    case CMD_CLEAR: {
      for (int i = 0; i < EEPROM.length(); i++) EEPROM.write(i, 0xFF);
      bool sendAck(true);
      break;
    }

    case CMD_VERIFY: {
      int size = (f[6] << 24) | (f[7] << 16) | (f[8] << 8) | f[9];
      bool ok = true;
      for (int i = 0; i < size; i++) {
        if (i < EEPROM.length() && EEPROM.read(i) != f[10+i]) {
          ok = false; break;
        }
      }
      if (!ok) lastError = 0x02; // Verification failed
      bool sendAck(ok);
      break;
    }

    case CMD_STARTUP: {
      sendStartupInfo();
      break;
    }

    case CMD_DIAGNOSIS: {
      sendDiagnosis();
      break;
    }

    case CMD_COLLAB: {
      byte msg[2] = {0xCA, 0xFE}; // Example response
      sendResponse(RES_COLLAB, msg, 2);
      break;
    }

    case CMD_CALIB_READ: {
      int start = (f[6]<<8) | f[7];
      int len   = (f[8]<<8) | f[9];
      sendCalibration(start, len);
      break;
    }

    case CMD_CALIB_WRITE: {
      int start = (f[6]<<8) | f[7];
      int len   = (f[8]<<8) | f[9];
      for (int i=0; i<len; i++) {
        if (start+i < EEPROM.length()) EEPROM.write(start+i, f[10+i]);
      }
      bool sendAck(true);
      break;
    }
  }
}

// ================= Responses =================
void sendAck(bool success = true) {
  byte resp[7];
  resp[0] = HEADER;
  resp[1] = 0x00; resp[2] = 0x00; resp[3] = 0x00; resp[4] = 0x01;
  resp[5] = success ? RES_ACK : RES_NACK;
  resp[6] = TAIL;
  Serial.write(resp, 7);
}

void sendResponse(byte type, byte *data, int size) {
  byte resp[7 + size];
  resp[0] = HEADER;
  resp[1] = (byte)((size >> 24) & 0xFF);
  resp[2] = (byte)((size >> 16) & 0xFF);
  resp[3] = (byte)((size >> 8) & 0xFF);
  resp[4] = (byte)(size & 0xFF);
  resp[5] = type;
  for (int i = 0; i < size; i++) resp[6+i] = data[i];
  resp[6+size] = TAIL;
  Serial.write(resp, 7+size);
}

void sendReadAll(int size) {
  byte resp[7 + size];
  resp[0] = HEADER;
  resp[1] = (byte)((size >> 24) & 0xFF);
  resp[2] = (byte)((size >> 16) & 0xFF);
  resp[3] = (byte)((size >> 8) & 0xFF);
  resp[4] = (byte)(size & 0xFF);
  resp[5] = RES_READ_ALL;
  for (int i = 0; i < size; i++) resp[6+i] = EEPROM.read(i);
  resp[6+size] = TAIL;
  Serial.write(resp, 7+size);
}

void sendCalibration(int start, int len) {
  if (start+len > EEPROM.length()) len = EEPROM.length()-start;
  byte resp[7+len];
  resp[0] = HEADER;
  resp[1] = (byte)((len >> 24) & 0xFF);
  resp[2] = (byte)((len >> 16) & 0xFF);
  resp[3] = (byte)((len >> 8) & 0xFF);
  resp[4] = (byte)(len & 0xFF);
  resp[5] = RES_CALIB;
  for (int i=0; i<len; i++) resp[6+i] = EEPROM.read(start+i);
  resp[6+len] = TAIL;
  Serial.write(resp, 7+len);
}

void sendStartupInfo() {
  byte info[6];
  info[0] = (byte)((EEPROM.length() >> 8) & 0xFF);
  info[1] = (byte)(EEPROM.length() & 0xFF);
  info[2] = 0x01; // Firmware Version Major
  info[3] = 0x00; // Minor
  info[4] = 0x55; // Device ID Example
  info[5] = lastError;
  sendResponse(RES_INFO, info, 6);
}

void sendDiagnosis() {
  byte diag[2];
  diag[0] = lastError;
  diag[1] = 0x00; // reserved
  sendResponse(RES_DIAGNOSIS, diag, 2);
}
