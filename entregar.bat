@echo off
rem Truco Ya - todo junto: arma el .jar (carpeta entrega) y sube el proyecto a GitHub.
cd /d "%~dp0"
call armar_jar.bat
if errorlevel 1 exit /b 1
call subir_a_github.bat
