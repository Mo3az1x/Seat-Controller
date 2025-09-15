@echo off
cd /d %~dp0
set CP=out;lib\jSerialComm-2.11.2.jar
echo Running...
java -cp "%CP%" application.Main
pause
