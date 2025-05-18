@echo off
cd /d "%~dp0"
setlocal EnableDelayedExpansion

echo Carpeta actual: %cd%

REM Verificar si existe .env
if exist ".env" (
    echo El archivo .env existe en la carpeta.
    echo Si contiene claves sensibles, elimínalo o muévelo antes de continuar.
    echo Deseas continuar de todas formas? (S/N)
    set /p RESPUESTA=

    if /I "!RESPUESTA!" NEQ "S" (
        echo Operacion cancelada.
        pause
        exit /b
    )
)

echo Eliminando repositorio Git anterior...
rmdir /s /q .git

echo Inicializando nuevo repositorio...
git init

echo Agregando remoto...
git remote add origin https://github.com/ManuelCaceresDuocUC/RealBarLacteo.git

echo Verificando que .env este en .gitignore...
findstr /C:".env" .gitignore >nul || echo .env>>.gitignore

echo Agregando archivos...
git add .

echo Commit inicial...
git commit -m "Historial limpio sin archivos sensibles"

echo Subiendo al repositorio con --force...
git push origin main --force

echo Proceso finalizado correctamente.
pause
