@echo off
rem Truco Ya - abre Minecraft 1.16.5 con el mod (usa el JDK 8 que descarga probar.bat)
setlocal
cd /d "%~dp0"
set "LOG=%~dp0jugar_log.txt"
set "JDKDIR="
for /d %%D in ("%~dp0.jdk\jdk8*") do set "JDKDIR=%%~fD"
if not defined JDKDIR (
  echo Primero ejecuta probar.bat para descargar el JDK 8.
  pause
  exit /b
)
set "JAVA_HOME=%JDKDIR%"
set "PATH=%JDKDIR%\bin;%PATH%"
echo Abriendo Minecraft con Truco Ya... (el detalle queda en jugar_log.txt)
call gradlew.bat runClient --no-daemon --console=plain > "%LOG%" 2>&1
echo runClient exit %ERRORLEVEL% >> "%LOG%"
echo Minecraft se cerro. Detalle en jugar_log.txt
pause
