@echo off
rem Truco Ya - compila el mod y deja en la carpeta "entrega" los dos .jar para instalar:
rem   trucoya-<version>.jar  y  geckolib-forge-1.16.5-3.0.106.jar
setlocal EnableDelayedExpansion
cd /d "%~dp0.."
set "LOG=%CD%\armar_jar_log.txt"
set "GECKO=3.0.106"
echo ==== Truco Ya: armar el .jar %date% %time% ==== > "%LOG%"
rem archivos que una actualizacion dejo de usar (lista en borrar.txt)
if exist "borrar.txt" (
  for /f "usebackq delims=" %%f in ("borrar.txt") do if exist "%%f" del /q "%%f"
  del /q "borrar.txt"
)
set "JDKDIR="
for /d %%D in ("%CD%\.jdk\jdk8*") do set "JDKDIR=%%~fD"
if not defined JDKDIR (
  echo Primero ejecuta probar.bat para descargar el JDK 8.
  echo FALTA_JDK >> "%LOG%"
  timeout /t 15
  exit /b 1
)
set "JAVA_HOME=%JDKDIR%"
set "PATH=%JDKDIR%\bin;%PATH%"
echo Compilando el mod (un par de minutos)...
del /q "build\libs\trucoya-*.jar" 2>nul
call gradlew.bat build -x test --no-daemon --console=plain >> "%LOG%" 2>&1
if errorlevel 1 (
  echo No se pudo compilar. Detalle en armar_jar_log.txt
  echo BUILD_FALLO >> "%LOG%"
  timeout /t 15
  exit /b 1
)
set "MODJAR="
for %%F in ("%CD%\build\libs\trucoya-*.jar") do (
  echo %%~nF | findstr /i "sources" >nul || set "MODJAR=%%~fF"
)
if not defined MODJAR (
  echo No aparecio el .jar. Detalle en armar_jar_log.txt
  echo SIN_JAR >> "%LOG%"
  timeout /t 15
  exit /b 1
)
if not exist "entrega" mkdir "entrega"
del /q "entrega\trucoya-*.jar" 2>nul
copy /y "!MODJAR!" "entrega\" >nul
set "GECKOJAR=entrega\geckolib-forge-1.16.5-%GECKO%.jar"
if not exist "%GECKOJAR%" (
  for /r "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\software.bernie.geckolib" %%G in (geckolib-forge-1.16.5-%GECKO%.jar) do (
    if exist "%%G" if not exist "%GECKOJAR%" copy /y "%%G" "%GECKOJAR%" >nul
  )
)
echo JAR_OK !MODJAR! >> "%LOG%"
dir /b entrega >> "%LOG%"
echo ==== FIN ==== >> "%LOG%"
echo.
echo Listo: los .jar quedaron en la carpeta "entrega".
timeout /t 10
exit /b 0
