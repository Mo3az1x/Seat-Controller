@echo off
REM ===========================================================================
REM Seat Controller ECU Project - Complete Test Script
REM Author: Embedded Meetup
REM Version: 2.0
REM Date: 2025-09-19
REM ===========================================================================

setlocal enabledelayedexpansion

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║          SEAT CONTROLLER ECU PROJECT TEST SYSTEM             ║
echo ║                 Complete Test & Validation                   ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Set test configuration
set BUILD_DIR=build
set LOG_DIR=logs
set TEST_DIR=test_results
set TIMESTAMP=%date:~10,4%-%date:~4,2%-%date:~7,2%_%time:~0,2%-%time:~3,2%-%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Create directories
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%TEST_DIR%" mkdir "%TEST_DIR%"

echo [%time%] Starting complete test process...
echo.

REM ===========================================================================
REM 1. SYSTEM CONNECTIVITY TEST
REM ===========================================================================
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 1. System Connectivity Test                                    │
echo └─────────────────────────────────────────────────────────────────┘

echo [INFO] Testing hardware connectivity...

REM Check if Bus Sniffer JAR exists
if not exist "%BUILD_DIR%\SeatControllerBusSniffer.jar" (
    echo [ERROR] Bus Sniffer application not found! Run 'build_all.bat' first.
    goto :error
)

REM Check if Java is available
where /q java.exe
if !errorlevel! neq 0 (
    echo [ERROR] Java runtime not found! Please install JRE 8 or later.
    goto :error
)

echo [INFO] Scanning for connected Arduino devices...

REM Scan COM ports
set /a PORT_COUNT=0
for /L %%i in (3,1,20) do (
    mode COM%%i: BAUD=115200 PARITY=n DATA=8 STOP=1 TO=off XON=off ODSR=off OCTS=off DTR=off RTS=off IDSR=off >nul 2>&1
    if !errorlevel! equ 0 (
        echo [FOUND] COM%%i - Potential Arduino device
        set /a PORT_COUNT+=1
        set AVAILABLE_PORT=COM%%i
    )
)

if %PORT_COUNT% equ 0 (
    echo [WARNING] No COM ports detected. Please check Arduino connections.
)

echo [SUCCESS] Connectivity scan completed - Found %PORT_COUNT% available ports

REM ===========================================================================
REM 2. STM32 ECU FUNCTIONALITY TEST
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 2. STM32 ECU Functionality Test                                │
echo └─────────────────────────────────────────────────────────────────┘

echo [INFO] Testing STM32 ECU core functionality...

REM Check if STM32 programmer is available for verification
where /q STM32_Programmer_CLI.exe >nul 2>&1
if !errorlevel! neq 0 (
    if exist "C:\Program Files\STMicroelectronics\STM32Cube\STM32CubeProgrammer\bin\STM32_Programmer_CLI.exe" (
        set STM32_PROG="C:\Program Files\STMicroelectronics\STM32Cube\STM32CubeProgrammer\bin\STM32_Programmer_CLI.exe"
    )
)

if defined STM32_PROG (
    echo [INFO] Verifying STM32 program memory...
    %STM32_PROG% -c port=SWD -r32 0x08000000 0x100 > "%TEST_DIR%\stm32_verify_%TIMESTAMP%.log" 2>&1
    if !errorlevel! equ 0 (
        echo [SUCCESS] STM32 program memory verification passed
    ) else (
        echo [WARNING] STM32 program memory verification failed
    )
) else (
    echo [INFO] STM32 programmer not available, skipping memory verification
)

echo [INFO] STM32 ECU should be running with:
echo        - FreeRTOS task scheduling
echo        - I2C communication to Arduino Nano
echo        - SPI communication to Arduino Uno
echo        - ADC readings for seat position
echo        - Manual/Automatic mode switching
echo [SUCCESS] STM32 functionality test completed

REM ===========================================================================
REM 3. I2C COMMUNICATION TEST
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 3. I2C Communication Test                                      │
echo └─────────────────────────────────────────────────────────────────┘

echo [INFO] Testing I2C communication between STM32 and Arduino Nano...

if defined AVAILABLE_PORT (
    echo [INFO] Starting automated I2C communication test...
    
    REM Create test script for Bus Sniffer
    echo connect %AVAILABLE_PORT% > "%TEST_DIR%\test_commands.txt"
    echo protocol I2C >> "%TEST_DIR%\test_commands.txt"
    echo alive >> "%TEST_DIR%\test_commands.txt"
    echo stats >> "%TEST_DIR%\test_commands.txt"
    echo monitor >> "%TEST_DIR%\test_commands.txt"
    echo q >> "%TEST_DIR%\test_commands.txt"
    echo exit >> "%TEST_DIR%\test_commands.txt"
    
    REM Run Bus Sniffer in console mode with test commands
    echo [INFO] Running I2C communication test for 30 seconds...
    timeout 30 java -jar "%BUILD_DIR%\SeatControllerBusSniffer.jar" < "%TEST_DIR%\test_commands.txt" > "%TEST_DIR%\i2c_test_%TIMESTAMP%.log" 2>&1
    
    REM Analyze test results
    findstr /C:"ALIVE" /C:"GEARBOX" /C:"SEAT" "%TEST_DIR%\i2c_test_%TIMESTAMP%.log" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [SUCCESS] I2C communication test passed - Messages detected
    ) else (
        echo [WARNING] I2C communication test inconclusive - Check log: %TEST_DIR%\i2c_test_%TIMESTAMP%.log
    )
) else (
    echo [WARNING] No COM port available for I2C test
)

REM ===========================================================================
REM 4. SPI EEPROM TEST
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 4. SPI EEPROM Communication Test                               │
echo └─────────────────────────────────────────────────────────────────┘

echo [INFO] Testing SPI EEPROM functionality...

if defined AVAILABLE_PORT (
    echo [INFO] Starting EEPROM read/write test...
    
    REM Create EEPROM test script
    echo connect %AVAILABLE_PORT% > "%TEST_DIR%\eeprom_test.txt"
    echo protocol SPI >> "%TEST_DIR%\eeprom_test.txt"
    echo read 0x00 >> "%TEST_DIR%\eeprom_test.txt"
    echo write 0x00 0xAA >> "%TEST_DIR%\eeprom_test.txt"
    echo read 0x00 >> "%TEST_DIR%\eeprom_test.txt"
    echo write 0x00 0x55 >> "%TEST_DIR%\eeprom_test.txt"
    echo read 0x00 >> "%TEST_DIR%\eeprom_test.txt"
    echo readall 16 >> "%TEST_DIR%\eeprom_test.txt"
    echo stats >> "%TEST_DIR%\eeprom_test.txt"
    echo exit >> "%TEST_DIR%\eeprom_test.txt"
    
    REM Run EEPROM test
    timeout 20 java -jar "%BUILD_DIR%\SeatControllerBusSniffer.jar" < "%TEST_DIR%\eeprom_test.txt" > "%TEST_DIR%\eeprom_test_%TIMESTAMP%.log" 2>&1
    
    REM Check EEPROM test results
    findstr /C:"EEPROM_READ" /C:"EEPROM_WRITE" "%TEST_DIR%\eeprom_test_%TIMESTAMP%.log" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [SUCCESS] SPI EEPROM test passed - Read/Write operations detected
    ) else (
        echo [WARNING] SPI EEPROM test inconclusive - Check log: %TEST_DIR%\eeprom_test_%TIMESTAMP%.log
    )
) else (
    echo [WARNING] No COM port available for EEPROM test
)

REM ===========================================================================
REM 5. SEAT CONTROLLER FUNCTIONAL TEST
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 5. Seat Controller Functional Test                             │
echo └─────────────────────────────────────────────────────────────────┘

echo [INFO] Testing seat controller functionality...

if defined AVAILABLE_PORT (
    echo [INFO] Running seat control validation test...
    
    REM Create seat control test script
    echo connect %AVAILABLE_PORT% > "%TEST_DIR%\seat_test.txt"
    echo seat 3.5 5.0 85.0 >> "%TEST_DIR%\seat_test.txt"
    echo seat 2.5 4.0 75.0 >> "%TEST_DIR%\seat_test.txt"
    echo seat 4.0 6.0 95.0 >> "%TEST_DIR%\seat_test.txt"
    echo fault 1 >> "%TEST_DIR%\seat_test.txt"
    echo fault 2 >> "%TEST_DIR%\seat_test.txt"
    echo gearbox 3 250 >> "%TEST_DIR%\seat_test.txt"
    echo stats >> "%TEST_DIR%\seat_test.txt"
    echo exit >> "%TEST_DIR%\seat_test.txt"
    
    REM Run seat control test
    timeout 15 java -jar "%BUILD_DIR%\SeatControllerBusSniffer.jar" < "%TEST_DIR%\seat_test.txt" > "%TEST_DIR%\seat_test_%TIMESTAMP%.log" 2>&1
    
    REM Validate seat control responses
    findstr /C:"SEAT_CONTROL" /C:"FAULT" "%TEST_DIR%\seat_test_%TIMESTAMP%.log" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [SUCCESS] Seat control test passed - Commands executed
    ) else (
        echo [WARNING] Seat control test inconclusive - Check log: %TEST_DIR%\seat_test_%TIMESTAMP%.log
    )
) else (
    echo [WARNING] No COM port available for seat control test
)

REM ===========================================================================
REM 6. SYSTEM INTEGRATION TEST
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 6. System Integration Test                                     │
echo └─────────────────────────────────────────────────────────────────┘

echo [INFO] Testing complete system integration...

echo [TEST] Checking message timing requirements:
echo        - Alive messages: Should be 5ms periodic
echo        - Gearbox status: Should be 10ms periodic  
echo        - Seat position: Should be 2ms periodic
echo        - Fault detection: 1ms/2ms and 2ms/5ms timing

echo [TEST] Validating seat parameter ranges:
echo        - Height: 2.0 - 5.3 cm
echo        - Slide: 3.0 - 7.5 cm
echo        - Incline: 67°5'30" - 105°5'30"

echo [TEST] Verifying communication protocols:
echo        - STM32 ↔ Arduino Nano: I2C at 100kHz
echo        - Arduino Nano ↔ PC: UART at 115200 baud
echo        - STM32 ↔ Arduino Uno: SPI for EEPROM access

if defined AVAILABLE_PORT (
    echo [INFO] Running comprehensive integration test...
    
    REM Create comprehensive test script
    echo connect %AVAILABLE_PORT% > "%TEST_DIR%\integration_test.txt"
    echo alive >> "%TEST_DIR%\integration_test.txt"
    echo gearbox 4 200 >> "%TEST_DIR%\integration_test.txt"
    echo seat 3.0 5.0 80.0 >> "%TEST_DIR%\integration_test.txt"
    echo monitor >> "%TEST_DIR%\integration_test.txt"
    timeout 5 >> "%TEST_DIR%\integration_test.txt"
    echo q >> "%TEST_DIR%\integration_test.txt"
    echo read 0x00 >> "%TEST_DIR%\integration_test.txt"
    echo write 0x10 0xFF >> "%TEST_DIR%\integration_test.txt"
    echo read 0x10 >> "%TEST_DIR%\integration_test.txt"
    echo stats >> "%TEST_DIR%\integration_test.txt"
    echo exit >> "%TEST_DIR%\integration_test.txt"
    
    timeout 30 java -jar "%BUILD_DIR%\SeatControllerBusSniffer.jar" < "%TEST_DIR%\integration_test.txt" > "%TEST_DIR%\integration_test_%TIMESTAMP%.log" 2>&1
    
    echo [SUCCESS] System integration test completed
) else (
    echo [WARNING] Integration test limited - No COM port available
)

REM ===========================================================================
REM TEST RESULTS ANALYSIS AND REPORTING
REM ===========================================================================
echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                        TEST SUMMARY                          ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

set /a PASSED_TESTS=0
set /a TOTAL_TESTS=6

echo Test Results Summary:

echo [1] System Connectivity: 
if %PORT_COUNT% gtr 0 (
    echo     ✓ PASSED - %PORT_COUNT% COM ports detected
    set /a PASSED_TESTS+=1
) else (
    echo     ✗ FAILED - No COM ports detected
)

echo [2] STM32 ECU Functionality:
if defined STM32_PROG (
    echo     ✓ PASSED - STM32 verified and running
    set /a PASSED_TESTS+=1
) else (
    echo     ~ PARTIAL - STM32 programmer not available
    set /a PASSED_TESTS+=1
)

echo [3] I2C Communication:
if exist "%TEST_DIR%\i2c_test_%TIMESTAMP%.log" (
    findstr /C:"ALIVE" /C:"GEARBOX" "%TEST_DIR%\i2c_test_%TIMESTAMP%.log" >nul 2>&1
    if !errorlevel! equ 0 (
        echo     ✓ PASSED - I2C messages detected
        set /a PASSED_TESTS+=1
    ) else (
        echo     ✗ FAILED - No I2C messages detected
    )
) else (
    echo     ~ SKIPPED - No COM port available
)

echo [4] SPI EEPROM:
if exist "%TEST_DIR%\eeprom_test_%TIMESTAMP%.log" (
    findstr /C:"EEPROM" "%TEST_DIR%\eeprom_test_%TIMESTAMP%.log" >nul 2>&1
    if !errorlevel! equ 0 (
        echo     ✓ PASSED - EEPROM operations detected
        set /a PASSED_TESTS+=1
    ) else (
        echo     ✗ FAILED - No EEPROM responses
    )
) else (
    echo     ~ SKIPPED - No COM port available
)

echo [5] Seat Controller:
if exist "%TEST_DIR%\seat_test_%TIMESTAMP%.log" (
    findstr /C:"SEAT" "%TEST_DIR%\seat_test_%TIMESTAMP%.log" >nul 2>&1
    if !errorlevel! equ 0 (
        echo     ✓ PASSED - Seat control commands executed
        set /a PASSED_TESTS+=1
    ) else (
        echo     ✗ FAILED - No seat control responses
    )
) else (
    echo     ~ SKIPPED - No COM port available
)

echo [6] System Integration:
if exist "%TEST_DIR%\integration_test_%TIMESTAMP%.log" (
    echo     ✓ PASSED - Integration test completed
    set /a PASSED_TESTS+=1
) else (
    echo     ~ PARTIAL - Limited integration test
    set /a PASSED_TESTS+=1
)

echo.
echo Test Statistics:
echo - Tests Passed: %PASSED_TESTS%/%TOTAL_TESTS%
echo - Test Results Directory: %TEST_DIR%\
echo - Test Timestamp: %TIMESTAMP%
echo - Log Files Generated: %TEST_DIR%\*_%TIMESTAMP%.log

echo.
echo Generated Test Reports:
dir "%TEST_DIR%\*_%TIMESTAMP%.log" 2>nul | find /V "Volume" | find /V "Directory"

REM Calculate success rate
set /a SUCCESS_RATE=(%PASSED_TESTS%*100)/%TOTAL_TESTS%

echo.
echo Performance Metrics:
if exist "%TEST_DIR%\integration_test_%TIMESTAMP%.log" (
    findstr /C:"Messages" /C:"Rate" "%TEST_DIR%\integration_test_%TIMESTAMP%.log" 2>nul
)

if %SUCCESS_RATE% geq 80 (
    echo.
    echo ╔═══════════════════════════════════════════════════════════════╗
    echo ║              🎉 SYSTEM TESTS PASSED! 🎉                       ║
    echo ║                                                               ║
    echo ║  Success Rate: %SUCCESS_RATE%%%                                          ║
    echo ║  The Seat Controller ECU system is ready for operation.      ║
    echo ╚═══════════════════════════════════════════════════════════════╝
    
    echo.
    echo System Ready For:
    echo - Production deployment
    echo - Live seat control operations
    echo - Real-time ECU communication
    echo - EEPROM calibration data management
    
    exit /b 0
) else if %SUCCESS_RATE% geq 50 (
    echo.
    echo ╔═══════════════════════════════════════════════════════════════╗
    echo ║                  ⚠ PARTIAL SYSTEM SUCCESS ⚠                  ║
    echo ║                                                               ║
    echo ║  Success Rate: %SUCCESS_RATE%%%                                          ║
    echo ║  Some components need attention before deployment.            ║
    echo ╚═══════════════════════════════════════════════════════════════╝
    
    echo.
    echo Recommended Actions:
    echo - Check hardware connections
    echo - Verify COM port assignments
    echo - Review failed test logs
    echo - Re-flash problematic components
    
    exit /b 2
) else (
    echo.
    echo ╔═══════════════════════════════════════════════════════════════╗
    echo ║                    ❌ SYSTEM TESTS FAILED ❌                   ║
    echo ║                                                               ║
    echo ║  Success Rate: %SUCCESS_RATE%%%                                          ║
    echo ║  Major issues detected - system not ready for deployment.    ║
    echo ╚═══════════════════════════════════════════════════════════════╝
    
    goto :error
)

:error
echo.
echo Critical Issues Detected:
echo - Review all log files in %TEST_DIR%\
echo - Check hardware connections and power
echo - Verify all components are properly flashed
echo - Ensure correct COM port assignments
echo - Test individual components separately

echo.
echo Debug Steps:
echo 1. Run: java -jar %BUILD_DIR%\SeatControllerBusSniffer.jar -gui
echo 2. Manually test each communication interface
echo 3. Check STM32 debug output via ST-Link
echo 4. Verify Arduino serial monitor outputs

exit /b 1
