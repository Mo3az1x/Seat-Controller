# Seat Controller ECU Project

A comprehensive automotive seat control system featuring STM32 microcontroller, Arduino bridge components, and PC-based monitoring tools.

## 🏗️ System Architecture

```
┌─────────────────┐    I2C     ┌─────────────────┐    UART    ┌──────────────────┐
│   STM32 Nucleo  │◄──────────►│ Arduino Nano    │◄──────────►│ PC Bus Sniffer   │
│   F411RE        │            │ (I2C-UART       │            │ (Java App)       │
│   (Master ECU)  │            │  Bridge)        │            │                  │
└─────────────────┘            └─────────────────┘            └──────────────────┘
         │                                                              
         │ SPI                                                          
         ▼                                                              
┌─────────────────┐                                                    
│ Arduino Uno     │                                                    
│ (EEPROM HIL)    │                                                    
└─────────────────┘                                                    
```

## 📋 Project Overview

This project implements a complete automotive seat controller ECU system with the following components:

- **STM32 Nucleo F411RE**: Master ECU running FreeRTOS with seat control logic
- **Arduino Nano**: I2C to UART bridge for PC communication
- **Arduino Uno**: SPI EEPROM Hardware-in-Loop (HIL) for calibration data
- **Java Bus Sniffer**: PC application for monitoring and testing

### Key Features

- ✅ **Dual Mode Operation**: Manual and Automatic seat control
- ✅ **Real-time Communication**: I2C, SPI, and UART protocols
- ✅ **Safety Compliance**: Seat position validation and fault detection
- ✅ **User Profiles**: EEPROM-based calibration and user settings
- ✅ **Professional Testing**: Comprehensive test suite and monitoring tools

## 🎯 Seat Control Specifications

| Parameter | Range | Units |
|-----------|-------|-------|
| **Height** | 2.0 - 5.3 | cm |
| **Slide** | 3.0 - 7.5 | cm |
| **Incline** | 67°5'30" - 105°5'30" | degrees |

### Communication Timing
- **Alive Messages**: 5ms periodic
- **Seat Position Updates**: 2ms periodic
- **Gearbox Status**: 10ms periodic
- **Fault Detection**: 1ms/2ms activation, 2ms/5ms deactivation

## 🚀 Quick Start

### Prerequisites

**Software Requirements:**
- STM32CubeIDE or STM32CubeProgrammer
- Arduino IDE or Arduino CLI
- Java JDK 8 or later
- Git for version control

**Hardware Requirements:**
- STM32 Nucleo F411RE development board
- Arduino Nano (ATmega328P)
- Arduino Uno (ATmega328P)
- USB cables and jumper wires

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://gitlab.com/your-project/seat-controller-ecu.git
   cd seat-controller-ecu
   ```

2. **Build All Components**
   ```batch
   build_all.bat
   ```

3. **Flash Hardware**
   ```batch
   flash_all.bat
   ```

4. **Run System Tests**
   ```batch
   test_all.bat
   ```

## 📁 Project Structure

```
SeatController-ECU/
├── STM32_SeatController/          # STM32 main ECU application
│   ├── Core/
│   │   ├── Src/
│   │   │   └── main.c            # Main application with FreeRTOS tasks
│   │   └── Inc/
│   │       └── main.h            # Header definitions
│   ├── Drivers/                  # HAL drivers
│   ├── Middlewares/              # FreeRTOS configuration
│   └── *.ioc                     # STM32CubeMX configuration
│
├── Arduino_Nano_Bridge/          # I2C to UART bridge
│   └── I2C_UART_Bridge.ino      # Arduino Nano firmware
│
├── Arduino_Uno_EEPROM/           # SPI EEPROM HIL
│   └── SPI_EEPROM_HIL.ino       # Arduino Uno firmware
│
├── BusSnifferApp/               # Java monitoring application
│   ├── src/application/
│   │   ├── SeatControllerBusSniffer.java    # Main GUI application
│   │   ├── SnifferManager.java              # Communication manager
│   │   ├── TraceListener.java               # Message interface
│   │   ├── SerialComm.java                  # Serial communication
│   │   ├── PortUtil.java                    # Port utilities
│   │   └── Main.java                        # Console interface
│   └── lib/
│       └── jSerialComm-2.10.4.jar          # Serial communication library
│
├── build/                       # Generated build outputs
├── logs/                        # Build and test logs
├── test_results/               # Test execution results
├── docs/                       # Additional documentation
│
├── build_all.bat              # Complete build script
├── flash_all.bat              # Hardware flash script
├── test_all.bat               # System test script
└── README.md                  # This file
```

## 🔧 Hardware Connections

### STM32 Nucleo F411RE Pinout

| Function | Pin | Connection |
|----------|-----|------------|
| **I2C1_SCL** | PB8 | Arduino Nano A5 |
| **I2C1_SDA** | PB9 | Arduino Nano A4 |
| **SPI1_SCK** | PA5 | Arduino Uno D13 |
| **SPI1_MOSI** | PA7 | Arduino Uno D11 |
| **SPI1_MISO** | PA6 | Arduino Uno D12 |
| **SPI1_CS** | PA4 | Arduino Uno D10 |
| **UART2_TX** | PA2 | Debug output (115200 baud) |

### Arduino Connections

**Arduino Nano (I2C Bridge):**
- A4 (SDA) → STM32 PB9
- A5 (SCL) → STM32 PB8  
- D0/D1 → PC USB Serial (115200 baud)

**Arduino Uno (EEPROM HIL):**
- D10 (SS) → STM32 PA4
- D11 (MOSI) → STM32 PA7
- D12 (MISO) → STM32 PA6
- D13 (SCK) → STM32 PA5

## 🖥️ Usage

### GUI Application

Launch the graphical Bus Sniffer:
```batch
java -jar build/SeatControllerBusSniffer.jar
```

**Features:**
- Real-time seat position control
- Communication protocol monitoring
- EEPROM calibration management
- Fault injection and testing
- Message waveform visualization

### Console Application

For automated testing and scripting:
```batch
java -jar build/SeatControllerBusSniffer.jar -port COM4 -protocol I2C
```

**Available Commands:**
```
connect <port>          - Connect to ECU
seat <h> <s> <i>       - Set seat position (height, slide, incline)
alive                  - Send alive message
fault <1|2>           - Trigger fault condition
read <addr>           - Read EEPROM byte
write <addr> <val>    - Write EEPROM byte
monitor               - Start continuous monitoring
stats                 - Show communication statistics
```

## 🧪 Testing

The project includes comprehensive testing capabilities:

### Automated Tests
```batch
test_all.bat
```

**Test Coverage:**
1. **System Connectivity** - Hardware detection and COM port scanning
2. **STM32 ECU Functionality** - Core application verification
3. **I2C Communication** - Message exchange testing
4. **SPI EEPROM** - Calibration data read/write validation
5. **Seat Controller** - Functional command execution
6. **System Integration** - End-to-end workflow testing

### Manual Testing

1. **Hardware Verification**
   - Check all connections according to pinout
   - Verify power supply (5V/3.3V as required)
   - Test individual components

2. **Software Testing**
   - Monitor STM32 debug output via UART2
   - Check Arduino serial monitor outputs
   - Verify Bus Sniffer message reception

### Expected Test Results

**Success Indicators:**
- ✅ All COM ports detected
- ✅ I2C messages flowing (ALIVE, GEARBOX, SEAT)
- ✅ EEPROM read/write operations working
- ✅ Seat control commands executing
- ✅ Fault detection and reporting active

## 📊 Performance Metrics

### Communication Performance
- **I2C Bus Speed**: 100 kHz
- **SPI Bus Speed**: 1 MHz  
- **UART Baud Rate**: 115200
- **Message Throughput**: ~500 msg/sec

### Real-time Requirements
- **Task Scheduling**: FreeRTOS with priority-based scheduling
- **Response Time**: <10ms for seat control commands
- **Fault Detection**: <5ms activation time
- **Memory Usage**: <80% of available RAM/Flash

## 🛠️ Development

### Building from Source

**Prerequisites:**
- STM32CubeIDE 1.8.0+
- Arduino IDE 1.8.15+ or Arduino CLI
- JDK 8+ with Maven (optional)

**Build Steps:**
1. Import STM32 project into STM32CubeIDE
2. Configure Arduino IDE with required libraries
3. Compile Java sources with dependencies

### Adding New Features

1. **STM32 Development**
   - Modify `main.c` for new functionality
   - Update FreeRTOS task priorities as needed
   - Rebuild and flash via ST-Link

2. **Arduino Modifications**
   - Update bridge or EEPROM firmware
   - Test with Arduino IDE serial monitor
   - Flash via USB bootloader

3. **Java Application**
   - Extend `SeatControllerBusSniffer.java` for new UI features
   - Add protocol support in `SnifferManager.java`
   - Rebuild JAR with updated dependencies

## 🔍 Troubleshooting

### Common Issues

**Connection Problems:**
- Check COM port assignments
- Verify cable connections
- Test with device manager
- Try different USB ports

**Communication Errors:**
- Verify baud rate settings (115200)
- Check I2C pull-up resistors (if needed)
- Test SPI clock polarity/phase
- Monitor with oscilloscope if available

**Build Failures:**
- Update development tools to latest versions
- Check library dependencies
- Review compilation error logs
- Verify target hardware selection

### Debug Tools

**STM32 Debugging:**
- Use ST-Link GDB debugger
- Monitor UART2 output (115200 baud)
- Check GPIO pin states with multimeter
- Use STM32CubeProgrammer for memory inspection

**Arduino Debugging:**
- Serial monitor at appropriate baud rates
- LED indicators for status checking
- Logic analyzer for I2C/SPI signals
- Multimeter for power supply verification

## 📈 Future Enhancements

### Planned Features
- [ ] CAN bus communication support
- [ ] Wireless seat position monitoring
- [ ] Advanced diagnostics with OBD-II integration
- [ ] Mobile app for remote seat control
- [ ] Machine learning for user preference prediction
- [ ] Integration with vehicle climate control
- [ ] Voice control interface
- [ ] Seat wear monitoring and maintenance alerts

### Roadmap
- **Phase 1**: Core functionality (Complete)
- **Phase 2**: Enhanced testing and validation (Current)
- **Phase 3**: CAN bus integration (Q2 2025)
- **Phase 4**: Wireless features (Q3 2025)
- **Phase 5**: AI-based enhancements (Q4 2025)

## 🤝 Contributing

### Development Guidelines

1. **Code Standards**
   - Follow MISRA-C guidelines for STM32 code
   - Use Arduino coding standards for Arduino projects
   - Apply Oracle Java conventions for Java development
   - Document all public APIs and complex functions

2. **Testing Requirements**
   - All new features must include unit tests
   - Integration tests for hardware interfaces
   - Performance regression testing
   - Documentation updates

3. **Version Control**
   - Use feature branches for development
   - Meaningful commit messages
   - Code review before merging to main
   - Tag releases with semantic versioning

### Submission Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-capability`)
3. Make changes with appropriate tests
4. Run full test suite (`test_all.bat`)
5. Submit pull request with detailed description
6. Address review feedback
7. Merge after approval

## 📝 Documentation

### Additional Resources

- **Hardware Design Guide**: `/docs/hardware/`
- **Software Architecture**: `/docs/software/`
- **Communication Protocols**: `/docs/protocols/`
- **Test Procedures**: `/docs/testing/`
- **Calibration Manual**: `/docs/calibration/`

### API Documentation

**STM32 APIs:**
- Seat control functions
- Communication interfaces
- Fault management
- User profile handling

**Java APIs:**
- Bus monitoring interfaces
- Protocol implementations
- Message parsing utilities
- GUI components

## 🔒 Safety and Compliance

### Automotive Standards
- **ISO 26262**: Functional safety compliance
- **ISO 11898**: CAN bus specifications (future)
- **ISO 14229**: Diagnostic communication
- **AUTOSAR 4.4.0**: Software architecture compliance

### Safety Features
- Position limit validation
- Fault detection and reporting
- Emergency stop functionality
- Diagnostic trouble codes (DTCs)
- Graceful degradation modes

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Third-Party Components
- **FreeRTOS**: MIT License
- **STM32 HAL**: BSD License
- **jSerialComm**: LGPL License
- **Arduino Libraries**: Various open-source licenses

## 🏆 Acknowledgments

### Contributors
- **Embedded Meetup**: Core development team
- **STMicroelectronics**: STM32 platform support
- **Arduino Community**: Hardware abstraction libraries
- **FreeRTOS Team**: Real-time operating system

### Special Thanks
- Automotive industry experts for requirements validation
- Open-source community for development tools
- Testing volunteers for system validation
- Documentation reviewers

## 📞 Support

### Getting Help

**Technical Support:**
- Create an issue on GitLab for bugs or feature requests
- Check existing documentation in `/docs/`
- Review troubleshooting guide above
- Join community discussions

**Commercial Support:**
- Contact: info@embeddedmeetup.net
- Professional development services
- Custom feature implementation
- Training and consulting

### Community

- **GitLab Issues**: Bug reports and feature requests
- **Discussions**: Technical questions and ideas
- **Wiki**: Community-maintained documentation
- **Releases**: Stable versions and changelogs

## 📊 Project Status

### Build Status
![Build Status](https://gitlab.com/seat-controller-ecu/badges/main/pipeline.svg)

### Current Version
**v2.0** - Complete system implementation with GUI monitoring

### Supported Platforms
- **STM32**: Nucleo F411RE (primary), F401RE, F446RE
- **Arduino**: Nano (ATmega328P), Uno (ATmega328P)
- **PC**: Windows 10/11, Linux (Ubuntu 18.04+), macOS 10.14+
- **Java**: JRE 8, 11, 17 (LTS versions)

### System Requirements

**Development Environment:**
- Windows 10+ or Ubuntu 18.04+
- 4 GB RAM minimum, 8 GB recommended
- 500 MB free disk space
- USB ports for hardware programming

**Runtime Environment:**
- Java 8+ for Bus Sniffer application
- Available COM ports for Arduino communication
- ST-Link compatible debugger for STM32

## 🔄 Changelog

### Version 2.0 (Current)
- Complete system integration
- Enhanced Bus Sniffer with GUI
- Comprehensive test automation
- Real-time waveform visualization
- Improved fault detection
- User profile management

### Version 1.0 (Previous)
- Basic seat control functionality
- I2C communication implementation
- Manual testing procedures
- STM32 FreeRTOS integration
- Arduino bridge communication

### Future Versions
- **v2.1**: Performance optimizations and bug fixes
- **v2.2**: Additional protocol support (CAN, LIN)
- **v3.0**: Wireless connectivity and mobile app

---

## Quick Command Reference

```bash
# Build everything
build_all.bat

# Flash all components  
flash_all.bat

# Run comprehensive tests
test_all.bat

# Launch GUI application
java -jar build/SeatControllerBusSniffer.jar

# Console mode with specific port
java -jar build/SeatControllerBusSniffer.jar -port COM4 -protocol I2C

# View help
java -jar build/SeatControllerBusSniffer.jar -help
```

---

**Last Updated**: September 19, 2025  
**Project Status**: Active Development  
**Maintainer**: Embedded Meetup Team
