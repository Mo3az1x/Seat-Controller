# Seat Controller ECU Bus Sniffer

A comprehensive Java Swing application for monitoring, testing, and interacting with automotive seat controller ECUs via UART communication.

## Features

- *Real-time Serial Communication*: Monitor UART traffic with configurable baud rates
- *Seat Position Control*: Send commands to adjust seat height, slide, and incline positions
- *EEPROM Operations*: Read/write individual bytes or entire EEPROM content
- *User Profiles*: Save and load seat position profiles to/from EEPROM
- *Fault Monitoring*: Trigger and monitor fault conditions
- *Visual Waveform Display*: Real-time visualization of communication frames
- *Message Tracing*: Comprehensive logging of all communication
- *Statistics Tracking*: Monitor communication performance and errors

## System Requirements

- *Java*: JDK 8 or higher
- *Operating System*: Windows, Linux, or macOS
- *Hardware*: Serial port (USB-to-Serial adapter supported)
- *Dependencies*: jSerialComm library (included)

## Project Structure


BusSnifferApp/
├── src/
│   └── application/
│       ├── SeatControllerBusSniffer.java      # Main GUI application
│       ├── SeatControllerSnifferManager.java  # Core communication manager
│       ├── SerialComm.java                     # Serial port communication
│       ├── SnifferManager.java                 # Wrapper/facade
│       ├── TraceListener.java                  # Event listener interface
│       └── ConsoleTraceListener.java           # Console output (placeholder)
├── lib/
│   └── jSerialComm-2.11.2.jar                # Serial communication library
├── out/                                        # Compiled classes (auto-generated)
├── build.bat                                   # Windows build script
├── run.bat                                     # Windows run script (console)
├── run_gui.bat                                # Windows GUI run script
└── seat_sniffer.properties                   # Configuration file


## Building and Running

### Windows

1. *Build the application:*
   batch
   build.bat
   

2. *Run the GUI application:*
   batch
   run_gui.bat
   

3. *Run console version:*
   batch
   run.bat
   

### Manual Compilation

bash
# Compile
javac -cp "lib/jSerialComm-2.11.2.jar" -d out src/application/*.java

# Run GUI
java -cp "out;lib/jSerialComm-2.11.2.jar" application.SeatControllerBusSniffer

# Run console (if implemented)
java -cp "out;lib/jSerialComm-2.11.2.jar" application.Main


## Configuration

The application uses seat_sniffer.properties for storing connection settings:

properties
# Seat Controller Sniffer Config
baud=115200
protocol=UART
port=COM6


Settings are automatically saved when you connect successfully.

## Usage Guide

### 1. Connection Setup

1. *Select COM Port*: Choose your serial port from the dropdown
2. *Set Baud Rate*: Enter the communication speed (default: 115200)
3. *Protocol*: Currently supports UART only
4. *Click Connect*: Establish connection to the ECU

### 2. Seat Control Operations

#### Position Control
- *Height Range*: 2.0 - 5.3 cm
- *Slide Range*: 3.0 - 7.5 cm  
- *Incline Range*: 67.1° - 105.1°

Enter target values and click "Send Seat Control" to move the seat.

#### Current Position Monitoring
The application displays real-time seat position feedback in the read-only current position fields.

### 3. EEPROM Operations

#### Single Byte Operations
- *Read Byte*: Enter address (hex or decimal) and click "Read Byte"
- *Write Byte*: Enter address and value, then click "Write Byte"

#### Bulk Operations
- *Read All*: Downloads entire EEPROM content
- *Write All*: Uploads test pattern to EEPROM

*Address Formats Supported:*
- Hexadecimal: 0x00FF, 0x1234
- Decimal: 255, 4660

### 4. Profile Management

#### Saving Profiles
1. Select profile number (0-9)
2. Enter height, slide, and incline values
3. Optionally specify EEPROM address
4. Click "Save Profile"

#### Loading Profiles
1. Select profile number
2. Optionally specify EEPROM address
3. Click "Load Profile"
4. Values will populate automatically when received

### 5. Fault Testing

- *Trigger Fault 1*: Activates fault condition 1 (auto-clears after 2 seconds)
- *Trigger Fault 2*: Activates fault condition 2 (auto-clears after 5 seconds)
- Status indicators show current fault states with color coding:
  - 🟢 *Green*: OK/Normal
  - 🔴 *Red*: Fault Active

### 6. Waveform Visualization

The waveform panel shows real-time communication activity:

- *Color Coding:*
  - 🟢 Green: Alive messages
  - 🔵 Blue: Gearbox status
  - 🟠 Orange: Seat control
  - 🔵 Cyan: EEPROM operations
  - 🔴 Red: Fault messages
  - 🟣 Magenta: Error conditions

- *Timeline*: Displays message sequence chronologically
- *Legend*: Shows message type color mapping

## Message Protocol

### Frame Structure (UART)

[HEADER][LENGTH][CMD_ID][PAYLOAD][CHECKSUM][TAIL]
  0x7E     4B      1B      N B       1B      0x7F


### Command IDs

| Command | ID | Description |
|---------|----:|-------------|
| READ_BYTE | 0x01 | Read single EEPROM byte |
| WRITE_BYTE | 0x02 | Write single EEPROM byte |
| READ_ALL | 0x03 | Read entire EEPROM |
| WRITE_ALL | 0x04 | Write EEPROM bulk data |
| ALIVE_MSG | 0x10 | Periodic alive message |
| GEARBOX_STATUS | 0x11 | Gearbox status update |
| SEAT_HEIGHT_TARGET | 0x20 | Height target position |
| SEAT_SLIDE_TARGET | 0x21 | Slide target position |
| SEAT_INCLINE_TARGET | 0x22 | Incline target position |
| SEAT_HEIGHT_CURRENT | 0x30 | Height current position |
| SEAT_SLIDE_CURRENT | 0x31 | Slide current position |
| SEAT_INCLINE_CURRENT | 0x32 | Incline current position |
| SEAT_CONTROL_REQ | 0x40 | Combined seat control |
| FAULT_1 | 0x50 | Fault condition 1 |
| FAULT_2 | 0x51 | Fault condition 2 |
| SAVE_PROFILE | 0x60 | Save user profile |
| LOAD_PROFILE | 0x61 | Load user profile |
| SAVE_PROFILE_AT | 0x62 | Save profile at address |
| LOAD_PROFILE_AT | 0x63 | Load profile from address |

### Response Codes
Response IDs are typically command ID + 0x80 (e.g., READ_BYTE response = 0x81).

## Troubleshooting

### Connection Issues

*"Connection Failed" Error:*
- Verify COM port exists and is not in use
- Check if ECU is powered and connected
- Try different baud rates (9600, 115200, 230400)
- Ensure serial cable wiring is correct

*"Port not found" Error:*
- Refresh available ports by restarting application
- Install proper USB-to-Serial drivers
- Check Windows Device Manager for port status

### Communication Issues

*No Response from ECU:*
- Verify protocol settings match ECU configuration
- Check if ECU firmware supports the message protocol
- Use oscilloscope to verify signal levels
- Try loopback test with TX-RX connected

*Garbled Data:*
- Incorrect baud rate setting
- Wrong data bits/parity configuration
- Electrical noise interference
- Cable length too long

### Performance Issues

*High Memory Usage:*
- Clear trace window regularly
- Reduce waveform history buffer
- Disable unnecessary message types

*Slow Response:*
- Check system CPU usage
- Reduce message frequency
- Close other serial applications

## Development

### Architecture

The application follows a layered architecture:

1. *Presentation Layer*: SeatControllerBusSniffer (Swing GUI)
2. *Business Logic*: SeatControllerSnifferManager (Protocol handling)
3. *Communication Layer*: SerialComm (Hardware abstraction)
4. *Event System*: TraceListener (Observer pattern)

### Key Classes

- **SeatControllerBusSniffer**: Main GUI application with tabbed interface
- **SeatControllerSnifferManager**: Core protocol implementation and message parsing
- **SerialComm**: Robust serial communication with threading and error handling
- **TraceListener**: Event-driven interface for message handling
- **SnifferManager**: Simplified facade for basic operations

### Extending the Application

#### Adding New Message Types

1. Define command ID in SeatControllerSnifferManager
2. Implement frame building method
3. Add parsing logic in handleFrameResponse()
4. Update GUI if needed

#### Adding New Protocols

1. Extend buildFrame() method for new protocol
2. Implement protocol-specific frame parsing
3. Update connection configuration

#### Custom Event Handling

Implement TraceListener interface for custom message processing:

java
public class CustomTraceListener implements TraceListener {
    @Override
    public void onTrace(String message) {
        // Custom logging logic
    }
    
    @Override
    public void onSeatControllerMessage(SeatControllerMessageType type, Object data) {
        // Custom message processing
    }
}


## Safety Considerations

⚠ *Important Safety Notes:*

- *Always ensure ECU is in safe state before testing*
- *Verify physical seat constraints before sending position commands*
- *Use appropriate fault testing procedures*
- *Do not perform EEPROM operations on production units without backup*
- *Monitor system behavior during testing*

## License

This software is provided as-is for development and testing purposes. Use at your own risk in automotive applications.

## Support

For technical support or feature requests, please refer to your internal development team or project documentation.

---

*Version*: 1.0  
*Last Updated*: September 2025  
*Compatible ECU Firmware*: Check with your ECU specifications
