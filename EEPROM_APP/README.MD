# EEPROM Programmer & Memory Management System

A comprehensive dual-interface EEPROM programming and memory management system featuring Arduino firmware with UART/SPI communication protocols and a Java-based GUI for advanced memory operations.

## 🎯 Overview

This project provides a complete solution for EEPROM memory management with:
- *Arduino-based EEPROM emulator* with dual communication protocols (UART + SPI)
- *Java GUI application* for memory operations and real-time monitoring
- *Memory service architecture* with structured data regions
- *Protocol sniffing and debugging* capabilities
- *Real-time trace logging* for development and diagnostics

## 🏗 System Architecture


┌─────────────────────┐    UART/SPI    ┌─────────────────────┐
│   Java GUI Client  │◄──────────────►│  Arduino EEPROM     │
│                     │                │  Emulator           │
│ • Memory Operations │                │ • UART Protocol     │
│ • Trace Logging     │                │ • SPI Slave Mode    │
│ • Memory Services   │                │ • Memory Regions    │
└─────────────────────┘                └─────────────────────┘


### Memory Layout

| Region | Address Range | Purpose | Description |
|--------|---------------|---------|-------------|
| *Startup* | 0x0000 - 0x0003 | Boot Configuration | System initialization parameters |
| *Calibration* | 0x0004 - 0x03FF | Calibration Data | Sensor calibration and configuration |
| *Diagnostic* | 0x0400 - 0x07FF | Diagnostic Info | Error logs and system diagnostics |

## 📁 Project Structure


EEPROM_APP/
├── eeprom/
│   ├── eeprom.ino              # Arduino firmware (UART + SPI)
│   └── NvmManager.java         # Memory manager utility class
├── src/eeprom/eeprom/
│   ├── EepromGUI.java          # Main GUI application
│   ├── PortUtil.java           # Serial port utilities
│   ├── SerialComm.java         # Serial communication handler
│   ├── SnifferManager.java     # Protocol sniffer and analyzer
│   └── TraceListener.java      # Trace logging interface
└── README.md                   # This file


## ⚡ Features

### Arduino Firmware (eeprom.ino)

#### Communication Protocols
- *UART Protocol*: Frame-based communication with header/tail markers
- *SPI Slave Mode*: Direct memory access for high-speed operations
- *Dual-mode operation*: Both protocols can be used simultaneously

#### Command Set
| Command | ID | Description |
|---------|----|-----------| 
| CMD_READ_BYTE | 0x01 | Read single byte from address |
| CMD_WRITE_BYTE | 0x02 | Write single byte to address |
| CMD_READ_ALL | 0x03 | Read entire memory or specified range |
| CMD_WRITE_ALL | 0x04 | Write data block to memory |
| CMD_CLEAR | 0x05 | Clear entire EEPROM (fill with 0xFF) |
| CMD_VERIFY | 0x06 | Verify written data integrity |
| CMD_STARTUP | 0x07 | Get system information |
| CMD_DIAGNOSIS | 0x08 | Get diagnostic information |
| CMD_CALIB_READ | 0x0A | Read calibration data |
| CMD_CALIB_WRITE | 0x0B | Write calibration data |

#### Frame Format (UART)


│ 0x7E │ LENGTH(4B) │ CMD(1B) │ DATA │ 0x7F │
│Header│   32-bit   │ Command │      │ Tail │


### Java GUI Application

#### Main Features
- *Tabbed Interface*: Organized by functionality (EEPROM, Utility, Memory Service)
- *Real-time Communication*: Live trace logging of all operations
- *Memory Services*: Structured access to memory regions
- *Protocol Sniffing*: Advanced debugging and analysis tools

#### GUI Tabs

##### 1. EEPROM Operations
- *Read Byte*: Read single memory location
- *Write Byte*: Write single byte to specific address
- *Read All*: Dump entire memory or specified range
- *Write All*: Bulk write operations

##### 2. Utility Functions
- *Clear*: Erase entire EEPROM
- *Verify*: Data integrity checking

##### 3. Memory Service
- *Startup Region*: Boot configuration management
- *Calibration Region*: Sensor calibration data
- *Diagnostic Region*: System diagnostic information

## 🚀 Getting Started

### Prerequisites

#### Hardware
- *Arduino Uno/Nano* (or compatible)
- *USB cable* for programming and communication
- *Optional*: SPI master device (e.g., STM32) for SPI testing

#### Software
- *Arduino IDE* (1.8.x or newer)
- *Java Runtime Environment* (JRE 8 or newer)
- *Java Development Kit* (JDK 8 or newer) for compilation
- *jSerialComm library* for Java serial communication

### Installation

#### 1. Arduino Setup
bash
# Upload the firmware to Arduino
# Open eeprom/eeprom.ino in Arduino IDE
# Select your board and port
# Click Upload


#### 2. Java Dependencies
Add jSerialComm to your project:
xml
<!-- Maven -->
<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>2.9.2</version>
</dependency>


#### 3. Compile Java Application
bash
# Navigate to src directory
cd EEPROM_APP/src

# Compile (ensure jSerialComm.jar is in classpath)
javac -cp ".:jSerialComm.jar" eeprom/*.java

# Run the GUI
java -cp ".:jSerialComm.jar" eeprom.EepromGUI


### Basic Usage

#### 1. Connect Hardware
1. Upload Arduino firmware
2. Connect Arduino to PC via USB
3. Note the COM port (e.g., COM3, /dev/ttyUSB0)

#### 2. Launch GUI Application
1. Run the Java GUI application
2. Select the correct COM port from dropdown
3. Set baud rate to *9600*
4. Click *Connect*

#### 3. Perform Operations

Example Workflow:
1. Click "Write Byte" → Enter address: 100 → Enter value: 42
2. Click "Read Byte" → Enter address: 100 → Verify value is 42
3. Check trace log for communication details


## 🔧 Protocol Details

### UART Communication

#### Request Frame Structure
c
byte frame[] = {
    0x7E,           // Header
    0x00, 0x00, 0x00, 0x05,  // Length (5 bytes)
    CMD_READ_BYTE,  // Command
    0x00, 0x00, 0x00, 0x64,  // Address (100)
    0x00,           // Value (for write operations)
    0x7F            // Tail
};


#### Response Frame Structure
c
byte response[] = {
    0x7E,           // Header
    0x00, 0x00, 0x00, 0x01,  // Length (1 byte)
    RES_READ_BYTE,  // Response type
    0x2A,           // Data (value 42)
    0x7F            // Tail
};


### SPI Communication

#### SPI Command Sequence

1. Master sends: CMD_READ (0x03)
2. Master sends: Address High Byte
3. Master sends: Address Low Byte
4. Master receives: Data byte


## 📊 Memory Services

### NvmManager Class Usage
java
NvmManager nvm = new NvmManager();

// Read operations
String startup = nvm.readStartup();
String calib = nvm.readCalibration();
String diag = nvm.readDiagnostic();

// Write operations
byte[] startupData = {0x01, 0x02, 0x03, 0x04};
String result = nvm.writeStartup(startupData);


## 🐛 Debugging & Troubleshooting

### Common Issues

#### Connection Problems

Symptoms: "Failed to connect" message
Solutions:
• Check COM port selection
• Verify Arduino is plugged in
• Ensure correct baud rate (9600)
• Close other serial applications


#### Communication Errors

Symptoms: No response from Arduino
Solutions:
• Check Arduino serial monitor for error messages
• Verify frame format in trace log
• Reset Arduino and reconnect
• Check USB cable integrity


#### Memory Operation Failures

Symptoms: NACK responses or verification failures
Solutions:
• Check address range limits
• Verify data format
• Monitor for error flags in diagnostic
• Check EEPROM wear level


### Trace Log Analysis

Example trace output:
Operation: READ_BYTE | Addr=100
Frame: 7E 00 00 00 05 01 00 00 00 64 00 7F
Received: 7E 00 00 00 01 81 2A 7F

Analysis:
• 7E/7F = Header/Tail markers ✓
• 05 = Command payload length ✓  
• 01 = READ_BYTE command ✓
• 64 = Address 100 (0x64) ✓
• 81 = READ_BYTE response ✓
• 2A = Data value 42 (0x2A) ✓


## 🔒 Safety Features

### Error Handling
- *Address validation*: Prevents out-of-bounds access
- *Frame integrity*: Header/tail validation
- *Timeout protection*: Prevents hanging operations
- *Error reporting*: Comprehensive diagnostic feedback

### Data Protection
- *Verification commands*: Ensure write operations succeeded
- *Memory region isolation*: Separate startup, calibration, and diagnostic areas
- *Backup mechanisms*: Easy data export/import via GUI

## 📈 Performance Characteristics

| Operation | UART Speed | SPI Speed | Notes |
|-----------|------------|-----------|-------|
| Single Read | ~10ms | ~1ms | Including protocol overhead |
| Single Write | ~15ms | ~5ms | Including EEPROM write time |
| Bulk Read (1KB) | ~2s | ~500ms | Large transfers |
| Bulk Write (1KB) | ~15s | ~5s | EEPROM write limitations |

## 🤝 Contributing

### Adding New Commands
1. *Arduino Side*: Add command ID to constants and implement in processFrame()
2. *Java Side*: Add GUI button and frame builder method
3. *Documentation*: Update command table and protocol specs

### Extending Memory Services
java
// Example: Add new memory region
public static final int CONFIG_START = 0x0800;
public static final int CONFIG_END   = 0x0BFF;

public String readConfig() {
    return "[NVM] READ_CONFIG | Addr=0x0800–0x0BFF";
}


## 📄 License

This project is provided as an educational and development tool. Feel free to modify and extend for your specific applications.

## 📞 Support

For technical questions:
- Check the trace logs for communication details
- Verify hardware connections
- Review Arduino serial monitor output
- Test with simple read/write operations first

---

This EEPROM management system demonstrates professional embedded development practices with robust communication protocols and comprehensive debugging tools.
