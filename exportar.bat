@echo off
rem Truco Ya - copia las librerias de compilacion a build\cp (uso interno para verificar el codigo)
setlocal
cd /d "%~dp0"
set "LOG=%~dp0exportar_log.txt"
set "JDKDIR="
for /d %%D in ("%~dp0.jdk\jdk8*") do set "JDKDIR=%%~fD"
if not defined JDKDIR (
  echo Primero ejecuta probar.bat
  pause
  exit /b
)
set "JAVA_HOME=%JDKDIR%"
set "PATH=%JDKDIR%\bin;%PATH%"
echo Copiando librerias...
call gradlew.bat exportClasspath --no-daemon --console=plain > "%LOG%" 2>&1
echo exit %ERRORLEVEL% >> "%LOG%"
dir /b build\cp >> "%LOG%"
echo ==== FIN ==== >> "%LOG%"
exit
