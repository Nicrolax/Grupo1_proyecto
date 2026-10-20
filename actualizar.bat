@echo off
rem Truco Ya - aplica la actualizacion (actualizacion.zip) y compila
setlocal
cd /d "%~dp0"
if not exist actualizacion.zip (
  echo No hay actualizacion.zip
  pause
  exit /b
)
echo Aplicando actualizacion...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path 'actualizacion.zip' -DestinationPath '.' -Force" > actualizar_log.txt 2>&1
del actualizacion.zip
call probar.bat
