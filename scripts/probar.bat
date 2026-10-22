@echo off
rem Truco Ya - compila y prueba el mod. Deja todo el detalle en probar_log.txt
setlocal
cd /d "%~dp0.."
set "LOG=%CD%\probar_log.txt"
echo ==== Truco Ya: prueba %date% %time% ==== > "%LOG%"

call :preparar_jdk
if not defined JDKDIR (
  echo ERROR: no se pudo obtener el JDK 8>> "%LOG%"
  echo ERROR: no se pudo obtener el JDK 8. Ver probar_log.txt
  goto fin
)

echo [2/3] Pruebas del motor de Truco (la primera vez descarga Forge y Minecraft, puede tardar 10-30 min)...
echo ==== coreTests ====>> "%LOG%"
call gradlew.bat coreTests --no-daemon --console=plain >> "%LOG%" 2>&1
echo coreTests exit %ERRORLEVEL% >> "%LOG%"

echo [3/3] Compilando el mod completo...
echo ==== build ====>> "%LOG%"
call gradlew.bat build --no-daemon --console=plain >> "%LOG%" 2>&1
echo build exit %ERRORLEVEL% >> "%LOG%"

:fin
echo ==== FIN ====>> "%LOG%"
echo.
echo Listo. El resultado quedo en probar_log.txt
pause
exit /b

:preparar_jdk
set "JDKDIR="
for /d %%D in ("%CD%\.jdk\jdk8*") do set "JDKDIR=%%~fD"
if defined JDKDIR goto usar_jdk
echo [1/3] Descargando JDK 8 (Eclipse Temurin, unos 100 MB)...
echo ==== descarga JDK 8 ====>> "%LOG%"
if not exist "%CD%\.jdk" mkdir "%CD%\.jdk"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '.jdk\jdk8.zip'; Expand-Archive -Path '.jdk\jdk8.zip' -DestinationPath '.jdk' -Force; Remove-Item '.jdk\jdk8.zip'" >> "%LOG%" 2>&1
for /d %%D in ("%CD%\.jdk\jdk8*") do set "JDKDIR=%%~fD"
if not defined JDKDIR exit /b
:usar_jdk
set "JAVA_HOME=%JDKDIR%"
set "PATH=%JDKDIR%\bin;%PATH%"
echo JAVA_HOME=%JAVA_HOME%>> "%LOG%"
"%JDKDIR%\bin\java.exe" -version >> "%LOG%" 2>&1
exit /b
