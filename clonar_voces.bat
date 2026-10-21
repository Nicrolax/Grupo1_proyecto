@echo off
rem Truco Ya - clona las voces de los Fundadores para los dialogos nuevos (instala todo en .clonar)
setlocal
cd /d "%~dp0"
set "LOG=%~dp0clonar_voces_log.txt"
echo ==== clonar voces %date% %time% ==== > "%LOG%"
if not exist ".clonar" mkdir ".clonar"
if not exist ".clonar\uv\uv.exe" (
  echo Bajando uv... >> "%LOG%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri https://github.com/astral-sh/uv/releases/latest/download/uv-x86_64-pc-windows-msvc.zip -OutFile .clonar\uv.zip; Expand-Archive -Path .clonar\uv.zip -DestinationPath .clonar\uv -Force" >> "%LOG%" 2>&1
)
if not exist ".clonar\uv\uv.exe" (
  echo UV_FALTA >> "%LOG%"
  exit /b 1
)
set "UV_CACHE_DIR=%~dp0.clonar\cache"
set "UV_PYTHON_INSTALL_DIR=%~dp0.clonar\python"
set "HF_HOME=%~dp0.clonar\hf"
set "TTS_HOME=%~dp0.clonar\tts"
set "COQUI_TOS_AGREED=1"
set "PYTHONIOENCODING=utf-8"
if not exist ".clonar\venv\Scripts\python.exe" ".clonar\uv\uv.exe" venv --python 3.11 ".clonar\venv" >> "%LOG%" 2>&1
".clonar\uv\uv.exe" pip install --python ".clonar\venv\Scripts\python.exe" "coqui-tts==0.28.0" "torch==2.8.*" "torchaudio==2.8.*" >> "%LOG%" 2>&1
echo PIP_EXIT %errorlevel% >> "%LOG%"
".clonar\venv\Scripts\python.exe" tools\clonar_voces.py >> "%LOG%" 2>&1
echo CLONAR_EXIT %errorlevel% >> "%LOG%"
echo ==== FIN ==== >> "%LOG%"
