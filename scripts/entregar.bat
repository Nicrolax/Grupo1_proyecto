@echo off
rem Truco Ya - todo junto: arma el .jar (carpeta entrega) y sube el proyecto a GitHub.
cd /d "%~dp0.."
call scripts\armar_jar.bat
if errorlevel 1 exit /b 1
call scripts\subir_a_github.bat
