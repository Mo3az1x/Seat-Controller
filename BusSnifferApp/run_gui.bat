@echo off
cd /d %~dp0
set CP=out;lib\jSerialComm-2.11.2.jar
echo Running GUI...
java -cp "%CP%" application.BusSnifferGUI
pause
