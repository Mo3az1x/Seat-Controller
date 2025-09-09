@echo off
cd /d %~dp0
if not exist out mkdir out
set JAR=lib\jSerialComm-2.11.2.jar
echo Compiling...
javac -cp "%JAR%" -d out src\application\*.java
if errorlevel 1 (
  echo Build FAILED.
  pause
  exit /b 1
)
echo Build SUCCESS.
pause
