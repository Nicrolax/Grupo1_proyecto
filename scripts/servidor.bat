@echo off
rem Truco Ya - arma (o actualiza) un servidor de Minecraft 1.16.5 con Forge 36.2.39, GeckoLib y el mod.
rem Todo queda en la carpeta "servidor". Para jugar con amigos: ver SERVIDOR.md
setlocal EnableDelayedExpansion
cd /d "%~dp0.."
set "LOG=%CD%\servidor_log.txt"
set "FORGE=1.16.5-36.2.39"
set "GECKO=3.0.106"
set "JDKDIR="
for /d %%D in ("%CD%\.jdk\jdk8*") do set "JDKDIR=%%~fD"
if not defined JDKDIR (
  echo Primero ejecuta probar.bat para descargar el JDK 8.
  pause
  exit /b
)
set "JAVA_HOME=%JDKDIR%"
set "PATH=%JDKDIR%\bin;%PATH%"
echo ==== Truco Ya: armando el servidor ==== > "%LOG%"

rem ---- 1. el mod, compilado como .jar
echo [1/5] Compilando el mod (tarda un par de minutos)...
del /q "build\libs\trucoya-*.jar" 2>nul
call gradlew.bat build -x test --no-daemon --console=plain >> "%LOG%" 2>&1
if errorlevel 1 (
  echo No se pudo compilar el mod. Mira servidor_log.txt
  pause
  exit /b
)
set "MODJAR="
for %%F in ("%CD%\build\libs\trucoya-*.jar") do (
  echo %%~nF | findstr /i "sources" >nul || set "MODJAR=%%~fF"
)
if not defined MODJAR (
  echo No se pudo compilar el mod. Mira servidor_log.txt
  pause
  exit /b
)
echo     listo: !MODJAR!

rem ---- 2. Forge para servidor
if not exist "servidor" mkdir "servidor"
if not exist "servidor\mods" mkdir "servidor\mods"
if not exist "servidor\forge-%FORGE%.jar" (
  echo [2/5] Descargando e instalando Forge %FORGE% para servidor...
  powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol='Tls12'; Invoke-WebRequest -UseBasicParsing 'https://maven.minecraftforge.net/net/minecraftforge/forge/%FORGE%/forge-%FORGE%-installer.jar' -OutFile 'servidor\forge-installer.jar'" >> "%LOG%" 2>&1
  if not exist "servidor\forge-installer.jar" (
    echo No se pudo descargar Forge. Revisa la conexion. Detalle en servidor_log.txt
    pause
    exit /b
  )
  pushd servidor
  java -jar forge-installer.jar --installServer >> "%LOG%" 2>&1
  del /q forge-installer.jar forge-installer.jar.log 2>nul
  popd
) else (
  echo [2/5] Forge %FORGE% ya estaba instalado.
)
if not exist "servidor\forge-%FORGE%.jar" (
  echo La instalacion de Forge fallo. Detalle en servidor_log.txt
  pause
  exit /b
)

rem ---- 3. GeckoLib (primero se busca la que ya bajo Gradle; si no, se descarga)
set "GECKOJAR=servidor\mods\geckolib-forge-1.16.5-%GECKO%.jar"
if not exist "%GECKOJAR%" (
  echo [3/5] Buscando GeckoLib %GECKO%...
  for /r "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\software.bernie.geckolib" %%G in (geckolib-forge-1.16.5-%GECKO%.jar) do (
    if exist "%%G" if not exist "%GECKOJAR%" copy /y "%%G" "%GECKOJAR%" >nul
  )
  if not exist "%GECKOJAR%" (
    powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol='Tls12'; Invoke-WebRequest -UseBasicParsing 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/software/bernie/geckolib/geckolib-forge-1.16.5/%GECKO%/geckolib-forge-1.16.5-%GECKO%.jar' -OutFile '%GECKOJAR%'" >> "%LOG%" 2>&1
  )
  if not exist "%GECKOJAR%" (
    echo No se encontro GeckoLib. Bajala a mano ^(geckolib-forge-1.16.5-%GECKO%.jar^) y ponela en servidor\mods
  )
) else (
  echo [3/5] GeckoLib ya estaba.
)

rem ---- 4. el mod en la carpeta mods (se reemplaza la version vieja)
echo [4/5] Copiando Truco Ya al servidor...
del /q "servidor\mods\trucoya-*.jar" 2>nul
copy /y "!MODJAR!" "servidor\mods\" >nul
rem lo mismo que necesita cada jugador en su carpeta .minecraft\mods
if not exist "servidor\para_los_jugadores" mkdir "servidor\para_los_jugadores"
del /q "servidor\para_los_jugadores\trucoya-*.jar" 2>nul
copy /y "!MODJAR!" "servidor\para_los_jugadores\" >nul
if exist "%GECKOJAR%" copy /y "%GECKOJAR%" "servidor\para_los_jugadores\" >nul

rem ---- 5. la EULA de Minecraft y el arranque
if not exist "servidor\eula.txt" (
  echo.
  echo Para abrir un servidor hay que aceptar la EULA de Minecraft: https://aka.ms/MinecraftEULA
  set /p "ACEPTO=Aceptas la EULA de Minecraft? (S/N): "
  if /i "!ACEPTO!"=="S" (
    echo eula=true> "servidor\eula.txt"
  ) else (
    echo Sin aceptar la EULA el servidor no arranca. Podes volver a correr servidor.bat cuando quieras.
    pause
    exit /b
  )
)
(
  echo @echo off
  echo rem Truco Ya - arranca el servidor ^(cerrarlo escribiendo "stop" en esta ventana^)
  echo cd /d "%%~dp0"
  echo "%JDKDIR%\bin\java.exe" -Xms1G -Xmx3G -jar forge-%FORGE%.jar nogui
  echo pause
) > "servidor\iniciar.bat"
echo [5/5] Listo.
echo.
echo   Para arrancar el servidor: servidor\iniciar.bat
echo   Cada jugador necesita Forge %FORGE% y los dos .jar de servidor\para_los_jugadores en su carpeta mods.
echo   Las opciones del mod quedan en servidor\world\serverconfig\trucoya-server.toml
echo.
set /p "YA=Arrancar el servidor ahora? (S/N): "
if /i "!YA!"=="S" (
  start "Servidor Truco Ya" /d "%CD%\servidor" cmd /c iniciar.bat
)
endlocal
