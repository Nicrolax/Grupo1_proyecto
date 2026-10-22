@echo off
rem Truco Ya - sube el proyecto a https://github.com/Nicrolax/Grupo1_proyecto
rem Nunca borra ni pisa a la fuerza lo que ya haya en el repositorio: si tiene commits, los junta con estos.
rem La primera vez Git puede abrir una ventana para iniciar sesion en GitHub.
setlocal EnableDelayedExpansion
cd /d "%~dp0.."
set "LOG=%CD%\subir_github_log.txt"
set "REPO=https://github.com/Nicrolax/Grupo1_proyecto.git"
echo ==== Truco Ya: subir a GitHub %date% %time% ==== > "%LOG%"
where git >nul 2>nul
if errorlevel 1 (
  if exist "C:\Program Files\Git\cmd\git.exe" (
    set "PATH=C:\Program Files\Git\cmd;%PATH%"
  ) else (
    echo No esta instalado Git. Bajalo de https://git-scm.com y volve a correr este archivo.
    echo SIN_GIT >> "%LOG%"
    timeout /t 20
    exit /b 1
  )
)
git --version >> "%LOG%" 2>&1
if not exist ".git" (
  git init >> "%LOG%" 2>&1
  git branch -M main >> "%LOG%" 2>&1
)
git remote get-url origin >nul 2>nul
if errorlevel 1 (
  git remote add origin %REPO% >> "%LOG%" 2>&1
) else (
  git remote set-url origin %REPO% >> "%LOG%" 2>&1
)
rem nombre para los commits (solo en este repositorio, si no hay uno configurado)
git config user.name >nul 2>nul || git config user.name "Grupo 1 - Truco Ya"
git config user.email >nul 2>nul || git config user.email "grupo1-trucoya@users.noreply.github.com"
git config core.autocrlf true

echo Preparando los archivos...
rem vuelve a armar la lista de archivos desde cero: lo que esta en .gitignore deja de subirse
git rm -r -q --cached . >> "%LOG%" 2>&1
git add -A >> "%LOG%" 2>&1
set "VER="
for /f "tokens=2 delims==" %%v in ('findstr /b "mod_version" gradle.properties') do set "VER=%%v"
set "MSG=Truco Ya !VER!"
if exist "mensaje_commit.txt" set /p MSG=<mensaje_commit.txt
git commit -q -m "!MSG!" >> "%LOG%" 2>&1
echo COMMIT_EXIT !ERRORLEVEL! >> "%LOG%"

echo Conectando con GitHub...
set "RAMA="
git ls-remote --heads origin main 2>>"%LOG%" | findstr /c:"refs/heads/main" >nul && set "RAMA=main"
if not defined RAMA (
  git ls-remote --heads origin master 2>>"%LOG%" | findstr /c:"refs/heads/master" >nul && set "RAMA=master"
)
echo RAMA_REMOTA=!RAMA! >> "%LOG%"
if defined RAMA (
  rem el repositorio ya tiene historia: se junta (en un choque gana la version nueva, sin borrar lo demas)
  git fetch origin !RAMA! >> "%LOG%" 2>&1
  git merge "origin/!RAMA!" --allow-unrelated-histories -X ours --no-edit -m "Junta con lo que ya estaba en GitHub" >> "%LOG%" 2>&1
  echo MERGE_EXIT !ERRORLEVEL! >> "%LOG%"
  if /i "!RAMA!"=="master" (
    git push -u origin HEAD:master >> "%LOG%" 2>&1
  ) else (
    git push -u origin HEAD:main >> "%LOG%" 2>&1
  )
) else (
  git push -u origin HEAD:main >> "%LOG%" 2>&1
)
set "R=!ERRORLEVEL!"
echo PUSH_EXIT !R! >> "%LOG%"
echo ==== FIN ==== >> "%LOG%"
if "!R!"=="0" (
  echo.
  echo Listo: el proyecto esta en %REPO%
) else (
  echo.
  echo No se pudo subir. Lo mas comun: falta iniciar sesion en GitHub o la cuenta no tiene permiso
  echo de escritura en el repositorio. Detalle en subir_github_log.txt
)
timeout /t 15
exit /b !R!
