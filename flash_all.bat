@echo off
REM ===========================================================================
REM Seat Controller ECU Project - Complete Flash Script
REM Author: Embedded Meetup
REM Version: 2.0
REM Date: 2025-09-19
REM ===========================================================================

setlocal enabledelayedexpansion

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║          SEAT CONTROLLER ECU PROJECT FLASH SYSTEM            ║
echo ║                   Complete Flash Process                     ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Set flash configuration
set BUILD_DIR=build
set LOG_DIR=logs
set TIMESTAMP=%date:~10,4%-%date:~4,2%-%date:~7,2%_%time:~0,2%-%time:~3,2%-%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Create log directory
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [%time%] Starting complete flash process...
echo.

REM Verify build files exist
echo [INFO] Verifying build outputs...

if not exist "%BUILD_DIR%\SeatController_STM32.elf" (
    echo [ERROR] STM32 ELF file not found! Run 'build_all.bat' first.
    goto :error
)

if not exist "%BUILD_DIR%\ArduinoNano_Bridge.hex" (
    echo [WARNING] Arduino Nano HEX file not found!
)

if not exist "%BUILD_DIR%\ArduinoUno_EEPROM.hex" (
    echo [WARNING] Arduino Uno HEX file not found!
)

echo [SUCCESS] Build verification completed.
echo.

REM ===========================================================================
REM 1. FLASH STM32 NUCLEO F411RE - MAIN ECU APPLICATION
REM ===========================================================================
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 1. Flashing STM32 Nucleo F411RE - Main ECU Application         │
echo └─────────────────────────────────────────────────────────────────┘

REM Check for ST-LINK utility
where /q STM32_Programmer_CLI.exe
if !errorlevel! neq 0 (
    REM Try alternative locations
    if exist "C:\Program Files\STMicroelectronics\STM32Cube\STM32CubeProgrammer\bin\STM32_Programmer_CLI.exe" (
        set STM32_PROG="C:\Program Files\STMicroelectronics\STM32Cube\STM32CubeProgrammer\bin\STM32_Programmer_CLI.exe"
    ) else if exist "C:\Program Files (x86)\STMicroelectronics\STM32 ST-LINK Utility\ST-LINK Utility\STM32_Programmer_CLI.exe" (
        set STM32_PROG="C:\Program Files (x86)\STMicroelectronics\STM32 ST-LINK Utility\ST-LINK Utility\STM32_Programmer_CLI.exe"
    ) else (
        echo [ERROR] STM32 Programmer CLI not found!
        echo Please install STM32CubeProgrammer or STM32 ST-LINK Utility
        echo Download from: https://www.st.com/en/development-tools/stm32cubeprog.html
        goto :error
    )
) else (
    set STM32_PROG=STM32_Programmer_CLI.exe
)

echo [INFO] Connecting to STM32 Nucleo F411RE...

REM Connect to ST-LINK
%STM32_PROG% -c port=SWD reset=HWrst > "%LOG_DIR%\stm32_connect_%TIMESTAMP%.log" 2>&1
if !errorlevel! neq 0 (
    echo [ERROR] Failed to connect to STM32! Check connections.
    echo [INFO] Make sure:
    echo        - STM32 Nucleo is connected via USB
    echo        - ST-LINK drivers are installed
    echo        - No other debugger is connected
    goto :error
)

echo [SUCCESS] Connected to STM32 Nucleo F411RE

REM Flash the application
echo [INFO] Erasing and programming STM32 flash memory...
%STM32_PROG% -c port=SWD reset=HWrst -e all -w "%BUILD_DIR%\SeatController_STM32.elf" -v -rst > "%LOG_DIR%\stm32_flash_%TIMESTAMP%.log" 2>&1

if !errorlevel! neq 0 (
    echo [ERROR] STM32 flash programming failed! Check log: %LOG_DIR%\stm32_flash_%TIMESTAMP%.log
    goto :error
)

echo [SUCCESS] STM32 Nucleo F411RE programmed successfully
echo [INFO] Application should start automatically after reset

REM ===========================================================================
REM 2. FLASH ARDUINO NANO - I2C TO UART BRIDGE
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 2. Flashing Arduino Nano - I2C to UART Bridge                  │
echo └─────────────────────────────────────────────────────────────────┘

if not exist "%BUILD_DIR%\ArduinoNano_Bridge.hex" (
    echo [WARNING] Arduino Nano HEX file not found, skipping...
    goto :flash_uno
)

REM Check for Arduino CLI or avrdude
where /q arduino-cli.exe
if !errorlevel! neq 0 (
    REM Try to find avrdude directly
    where /q avrdude.exe
    if !errorlevel! neq 0 (
        if exist "C:\Program Files (x86)\Arduino\hardware\tools\avr\bin\avrdude.exe" (
            set AVRDUDE_CMD="C:\Program Files (x86)\Arduino\hardware\tools\avr\bin\avrdude.exe"
            set AVRDUDE_CONF="C:\Program Files (x86)\Arduino\hardware\tools\avr\etc\avrdude.conf"
        ) else (
            echo [ERROR] Arduino development environment not found!
            echo Please install Arduino IDE or Arduino CLI
            goto :flash_uno
        )
    ) else (
        set AVRDUDE_CMD=avrdude.exe
        set AVRDUDE_CONF=
    )
) else (
    set ARDUINO_CMD=arduino-cli.exe
)

echo [INFO] Please connect Arduino Nano and note the COM port
echo [INFO] Common ports: COM3, COM4, COM5, COM6
echo.

REM Auto-detect COM port for Arduino Nano
echo [INFO] Scanning for Arduino Nano...
for /L %%i in (3,1,20) do (
    mode COM%%i: BAUD=9600 PARITY=n DATA=8 STOP=1 TO=off XON=off ODSR=off OCTS=off DTR=off RTS=off IDSR=off >nul 2>&1
    if !errorlevel! equ 0 (
        echo [INFO] Found potential Arduino on COM%%i
        set NANO_PORT=COM%%i
        goto :flash_nano_now
    )
)

REM Manual port selection
set /p NANO_PORT="Enter COM port for Arduino Nano (e.g., COM4): "

:flash_nano_now
if defined ARDUINO_CMD (
    echo [INFO] Programming Arduino Nano on %NANO_PORT%...
    %ARDUINO_CMD% upload -p %NANO_PORT% -f --fqbn arduino:avr:nano:cpu=atmega328 --input-file "%BUILD_DIR%\ArduinoNano_Bridge.hex" > "%LOG_DIR%\nano_flash_%TIMESTAMP%.log" 2>&1
) else (
    echo [INFO] Programming Arduino Nano using avrdude...
    %AVRDUDE_CMD% -C%AVRDUDE_CONF% -patmega328p -carduino -P%NANO_PORT% -b57600 -D -Uflash:w:"%BUILD_DIR%\ArduinoNano_Bridge.hex":i > "%LOG_DIR%\nano_flash_%TIMESTAMP%.log" 2>&1
)

if !errorlevel! neq 0 (
    echo [ERROR] Arduino Nano flash failed! Check log: %LOG_DIR%\nano_flash_%TIMESTAMP%.log
    echo [INFO] Common issues:
    echo        - Wrong COM port selected
    echo        - Arduino not in bootloader mode
    echo        - Cable connection issue
) else (
    echo [SUCCESS] Arduino Nano programmed successfully on %NANO_PORT%
)

:flash_uno
REM ===========================================================================
REM 3. FLASH ARDUINO UNO - SPI EEPROM HIL
REM ===========================================================================
echo.
echo ┌─────────────────────────────────────────────────────────────────┐
echo │ 3. Flashing Arduino Uno - SPI EEPROM HIL                       │
echo └─────────────────────────────────────────────────────────────────┘

if not exist "%BUILD_DIR%\ArduinoUno_EEPROM.hex" (
    echo [WARNING] Arduino Uno HEX file not found, skipping...
    goto :flash_summary
)

echo [INFO] Please connect Arduino Uno and note the COM port
echo [INFO] Make sure it's different from Arduino Nano port
echo.

REM Auto-detect COM port for Arduino Uno
echo [INFO] Scanning for Arduino Uno...
for /L %%i in (3,1,20) do (
    if not "%%i"=="%NANO_PORT:~3%" (
        mode COM%%i: BAUD=9600 PARITY=n DATA=8 STOP=1 TO=off XON=off ODSR=off OCTS=off DTR=off RTS=off IDSR=off >nul 2>&1
        if !errorlevel! equ 0 (
            echo [INFO] Found potential Arduino on COM%%i
            set UNO_PORT=COM%%i
            goto :flash_uno_now
        )
    )
)

REM Manual port selection
set /p UNO_PORT="Enter COM port for Arduino Uno (e.g., COM5): "

:flash_uno_now
if defined ARDUINO_CMD (
    echo [INFO] Programming Arduino Uno on %UNO_PORT%...
    %ARDUINO_CMD% upload -p %UNO_PORT% -f --fqbn arduino:avr:uno --input-file "%BUILD_DIR%\ArduinoUno_EEPROM.hex" > "%LOG_DIR%\uno_flash_%TIMESTAMP%.log" 2>&1
) else (
    echo [INFO] Programming Arduino Uno using avrdude...
    %AVRDUDE_CMD% -C%AVRDUDE_CONF% -patmega328p -carduino -P%UNO_PORT% -b115200 -D -Uflash:w:"%BUILD_DIR%\ArduinoUno_EEPROM.hex":i > "%LOG_DIR%\uno_flash_%TIMESTAMP%.log" 2>&1
)

if !errorlevel! neq 0 (
    echo [ERROR] Arduino Uno flash failed! Check log: %LOG_DIR%\uno_flash_%TIMESTAMP%.log
    echo [INFO] Common issues:
    echo        - Wrong COM port selected
    echo        - Arduino not in bootloader mode
    echo        - Cable connection issue
) else (
    echo [SUCCESS] Arduino Uno programmed successfully on %UNO_PORT%
)

:flash_summary
REM ===========================================================================
REM FLASH SUMMARY AND SYSTEM VERIFICATION
REM ===========================================================================
echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                       FLASH SUMMARY                          ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

echo System Configuration:
echo - STM32 Nucleo F411RE (Master ECU): Programmed
if defined NANO_PORT (
    echo - Arduino Nano Bridge (I2C-UART): %NANO_PORT%
) else (
    echo - Arduino Nano Bridge (I2C-UART): Not programmed
)
if defined UNO_PORT (
    echo - Arduino Uno EEPROM HIL (SPI): %UNO_PORT%
) else (
    echo - Arduino Uno EEPROM HIL (SPI): Not programmed
)

echo.
echo Connection Diagram:
echo   STM32 Nucleo F411RE
echo   ├── I2C ──→ Arduino Nano ──→ UART ──→ PC (Bus Sniffer)
echo   └── SPI ──→ Arduino Uno (EEPROM HIL)

echo.
echo Next Steps:
echo 1. Connect hardware according to connection diagram
echo 2. Open Bus Sniffer: java -jar %BUILD_DIR%\SeatControllerBusSniffer.jar
echo 3. Run system tests: test_all.bat
echo 4. Monitor I2C communication and EEPROM operations

echo.
echo Pin Connections:
echo STM32 Nucleo F411RE:
echo   - I2C1_SCL (PB8) → Arduino Nano A5 (SCL)
echo   - I2C1_SDA (PB9) → Arduino Nano A4 (SDA)
echo   - SPI1_SCK (PA5) → Arduino Uno D13 (SCK)
echo   - SPI1_MOSI (PA7) → Arduino Uno D11 (MOSI)
echo   - SPI1_MISO (PA6) → Arduino Uno D12 (MISO)
echo   - PA4 (CS) → Arduino Uno D10 (SS)

echo Arduino Nano Bridge:
echo   - D0 (TX) → PC USB-Serial RX
echo   - D1 (RX) → PC USB-Serial TX
echo   - Baud Rate: 115200

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║            🎉 FLASH PROCESS COMPLETED! 🎉                     ║
echo ║                                                               ║
echo ║  System ready for testing. Run 'test_all.bat' to verify.     ║
echo ╚═══════════════════════════════════════════════════════════════╝

exit /b 0

:error
echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                    ❌ FLASH FAILED ❌                          ║
echo ║                                                               ║
echo ║  Check the error messages above and log files for details.   ║
echo ╚═══════════════════════════════════════════════════════════════╝
exit /b 1
