@echo off
REM ===========================================================================
REM Seat Controller ECU Project - Complete Build Script
REM Author: Embedded Meetup
REM Version: 2.0
REM Date: 2025-09-19
REM ===========================================================================

setlocal enabledelayedexpansion

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║          SEAT CONTROLLER ECU PROJECT BUILD SYSTEM            ║
echo ║                    Complete Build Process                     ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Set build configuration
set BUILD_CONFIG=Release
set BUILD_DIR=build
set LOG_DIR=logs
set TIMESTAMP=%date:~10,4%-%date:~4,2%-%date:~7,2%_%time:~0,2%-%time:~3,2%-%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Create directories
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [%time%] Starting complete build process...
echo.

REM ===========================================================================
REM 1. STM32 NUCLEO F411RE - MAIN ECU APPLICATION
REM ===========================================================================
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 1. Building STM32 Nucleo F411RE - Main ECU Application         │
echo └─────────────────────────────────────────────────────────────────┘

cd STM32_SeatController

REM Check if STM32CubeIDE is available
where /q STM32CubeIDE.exe
if !errorlevel! neq 0 (
    echo [ERROR] STM32CubeIDE not found in PATH
    echo Please install STM32CubeIDE or add it to system PATH
    echo Expected location: C:\ST\STM32CubeIDE_*\STM32CubeIDE\stm32cubeide.exe
    goto :error
)

echo [INFO] Building STM32 ECU application...
STM32CubeIDE.exe --launcher.suppressErrors -nosplash -application org.eclipse.cdt.managedbuilder.core.headlessbuild -import . -cleanBuild "SeatController/%BUILD_CONFIG%" > "..\%LOG_DIR%\stm32_build_%TIMESTAMP%.log" 2>&1

if !errorlevel! neq 0 (
    echo [ERROR] STM32 build failed! Check log: %LOG_DIR%\stm32_build_%TIMESTAMP%.log
    goto :error
)

REM Check if output files exist
if exist "%BUILD_CONFIG%\SeatController.elf" (
    echo [SUCCESS] STM32 ELF file generated: %BUILD_CONFIG%\SeatController.elf
    copy "%BUILD_CONFIG%\SeatController.elf" "..\%BUILD_DIR%\SeatController_STM32.elf" > nul
    copy "%BUILD_CONFIG%\SeatController.bin" "..\%BUILD_DIR%\SeatController_STM32.bin" > nul 2>nul
    copy "%BUILD_CONFIG%\SeatController.hex" "..\%BUILD_DIR%\SeatController_STM32.hex" > nul 2>nul
) else (
    echo [ERROR] STM32 output files not found!
    goto :error
)

cd ..

REM ===========================================================================
REM 2. ARDUINO NANO - I2C TO UART BRIDGE
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 2. Building Arduino Nano - I2C to UART Bridge                  │
echo └─────────────────────────────────────────────────────────────────┘

cd Arduino_Nano_Bridge

REM Check if Arduino CLI is available
where /q arduino-cli.exe
if !errorlevel! neq 0 (
    echo [WARNING] Arduino CLI not found, attempting alternative methods...
    
    REM Try Arduino IDE installation
    if exist "C:\Program Files (x86)\Arduino\arduino_debug.exe" (
        set ARDUINO_CMD="C:\Program Files (x86)\Arduino\arduino_debug.exe"
    ) else if exist "C:\Users\%USERNAME%\AppData\Local\Arduino15\arduino-cli.exe" (
        set ARDUINO_CMD="C:\Users\%USERNAME%\AppData\Local\Arduino15\arduino-cli.exe"
    ) else (
        echo [ERROR] Arduino development environment not found!
        echo Please install Arduino IDE or Arduino CLI
        goto :error
    )
) else (
    set ARDUINO_CMD=arduino-cli.exe
)

echo [INFO] Compiling Arduino Nano bridge...

REM Install required libraries
%ARDUINO_CMD% lib install "Wire" 2>nul

REM Compile for Arduino Nano
%ARDUINO_CMD% compile --fqbn arduino:avr:nano:cpu=atmega328 I2C_UART_Bridge.ino --output-dir "..\%BUILD_DIR%" > "..\%LOG_DIR%\nano_build_%TIMESTAMP%.log" 2>&1

if !errorlevel! neq 0 (
    echo [ERROR] Arduino Nano build failed! Check log: %LOG_DIR%\nano_build_%TIMESTAMP%.log
    goto :error
)

echo [SUCCESS] Arduino Nano bridge compiled successfully
if exist "..\%BUILD_DIR%\I2C_UART_Bridge.ino.hex" (
    ren "..\%BUILD_DIR%\I2C_UART_Bridge.ino.hex" "ArduinoNano_Bridge.hex"
    echo [INFO] Output: %BUILD_DIR%\ArduinoNano_Bridge.hex
)

cd ..

REM ===========================================================================
REM 3. ARDUINO UNO - SPI EEPROM HIL
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 3. Building Arduino Uno - SPI EEPROM HIL                       │
echo └─────────────────────────────────────────────────────────────────┘

cd Arduino_Uno_EEPROM

echo [INFO] Compiling Arduino Uno EEPROM HIL...

REM Install required libraries
%ARDUINO_CMD% lib install "SPI" "EEPROM" 2>nul

REM Compile for Arduino Uno
%ARDUINO_CMD% compile --fqbn arduino:avr:uno SPI_EEPROM_HIL.ino --output-dir "..\%BUILD_DIR%" > "..\%LOG_DIR%\uno_build_%TIMESTAMP%.log" 2>&1

if !errorlevel! neq 0 (
    echo [ERROR] Arduino Uno build failed! Check log: %LOG_DIR%\uno_build_%TIMESTAMP%.log
    goto :error
)

echo [SUCCESS] Arduino Uno EEPROM HIL compiled successfully
if exist "..\%BUILD_DIR%\SPI_EEPROM_HIL.ino.hex" (
    ren "..\%BUILD_DIR%\SPI_EEPROM_HIL.ino.hex" "ArduinoUno_EEPROM.hex"
    echo [INFO] Output: %BUILD_DIR%\ArduinoUno_EEPROM.hex
)

cd ..

REM ===========================================================================
REM 4. JAVA BUS SNIFFER APPLICATION
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 4. Building Java Bus Sniffer Application                       │
echo └─────────────────────────────────────────────────────────────────┘

cd BusSnifferApp

REM Check for Java and javac
where /q javac.exe
if !errorlevel! neq 0 (
    echo [ERROR] Java compiler (javac) not found in PATH
    echo Please install JDK 8 or later
    goto :error
)

where /q java.exe
if !errorlevel! neq 0 (
    echo [ERROR] Java runtime not found in PATH
    goto :error
)

echo [INFO] Compiling Java Bus Sniffer...

REM Create build directories
if not exist "build\classes" mkdir "build\classes"
if not exist "build\lib" mkdir "build\lib"

REM Download jSerialComm if not present
if not exist "lib\jSerialComm-2.10.4.jar" (
    echo [INFO] Downloading jSerialComm library...
    if not exist "lib" mkdir "lib"
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/Fazecast/jSerialComm/releases/download/v2.10.4/jSerialComm-2.10.4.jar' -OutFile 'lib/jSerialComm-2.10.4.jar'" 2>nul
)

REM Compile Java sources
javac -cp "lib/*" -d "build/classes" src/application/*.java > "..\%LOG_DIR%\java_build_%TIMESTAMP%.log" 2>&1

if !errorlevel! neq 0 (
    echo [ERROR] Java compilation failed! Check log: %LOG_DIR%\java_build_%TIMESTAMP%.log
    goto :error
)

REM Create JAR file
echo Main-Class: application.SeatControllerBusSniffer > build/MANIFEST.MF
echo Class-Path: lib/jSerialComm-2.10.4.jar >> build/MANIFEST.MF

jar cfm "..\%BUILD_DIR%\SeatControllerBusSniffer.jar" build/MANIFEST.MF -C build/classes . > "..\%LOG_DIR%\jar_build_%TIMESTAMP%.log" 2>&1

if !errorlevel! neq 0 (
    echo [ERROR] JAR creation failed! Check log: %LOG_DIR%\jar_build_%TIMESTAMP%.log
    goto :error
)

REM Copy libraries
copy "lib\*.jar" "..\%BUILD_DIR%\lib\" > nul 2>nul
if not exist "..\%BUILD_DIR%\lib" mkdir "..\%BUILD_DIR%\lib"
copy "lib\*.jar" "..\%BUILD_DIR%\lib\" > nul

echo [SUCCESS] Java Bus Sniffer application built successfully
echo [INFO] Output: %BUILD_DIR%\SeatControllerBusSniffer.jar

cd ..

REM ===========================================================================
REM BUILD SUMMARY AND VERIFICATION
REM ===========================================================================
echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                        BUILD SUMMARY                          ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

set /a SUCCESS_COUNT=0
set /a TOTAL_COUNT=4

echo Build Results:
if exist "%BUILD_DIR%\SeatController_STM32.elf" (
    echo [✓] STM32 Nucleo F411RE ECU Application
    set /a SUCCESS_COUNT+=1
) else (
    echo [✗] STM32 Nucleo F411RE ECU Application
)

if exist "%BUILD_DIR%\ArduinoNano_Bridge.hex" (
    echo [✓] Arduino Nano I2C-UART Bridge
    set /a SUCCESS_COUNT+=1
) else (
    echo [✗] Arduino Nano I2C-UART Bridge
)

if exist "%BUILD_DIR%\ArduinoUno_EEPROM.hex" (
    echo [✓] Arduino Uno SPI EEPROM HIL
    set /a SUCCESS_COUNT+=1
) else (
    echo [✗] Arduino Uno SPI EEPROM HIL
)

if exist "%BUILD_DIR%\SeatControllerBusSniffer.jar" (
    echo [✓] Java Bus Sniffer Application
    set /a SUCCESS_COUNT+=1
) else (
    echo [✗] Java Bus Sniffer Application
)

echo.
echo Build Statistics:
echo - Successful builds: %SUCCESS_COUNT%/%TOTAL_COUNT%
echo - Build directory: %BUILD_DIR%\
echo - Log directory: %LOG_DIR%\
echo - Build timestamp: %TIMESTAMP%

REM Calculate build sizes
echo.
echo Output Files:
dir "%BUILD_DIR%\*.elf" "%BUILD_DIR%\*.hex" "%BUILD_DIR%\*.jar" 2>nul | find /V "Volume" | find /V "Directory"

if %SUCCESS_COUNT% equ %TOTAL_COUNT% (
    echo.
    echo ╔═══════════════════════════════════════════════════════════════╗
    echo ║              🎉 ALL BUILDS COMPLETED SUCCESSFULLY! 🎉          ║
    echo ║                                                               ║
    echo ║  Ready for flashing and testing. Run 'flash_all.bat' next.   ║
    echo ╚═══════════════════════════════════════════════════════════════╝
    exit /b 0
) else (
    echo.
    echo ╔═══════════════════════════════════════════════════════════════╗
    echo ║                  ⚠ SOME BUILDS FAILED ⚠                      ║
    echo ║                                                               ║
    echo ║  Check the log files for detailed error information.         ║
    echo ╚═══════════════════════════════════════════════════════════════╝
    exit /b 1
)

:error
echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                     ❌ BUILD FAILED ❌                         ║
echo ║                                                               ║
echo ║  Check the error messages above and log files for details.   ║
echo ╚═══════════════════════════════════════════════════════════════╝
exit /b 1
