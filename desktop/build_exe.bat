@echo off
cd /d "%~dp0"
py -m pip install -r requirements.txt
py -m PyInstaller --noconfirm --clean "JLPT Kotoba.spec"
echo.
echo Done. Share this file:
echo %~dp0dist\JLPT Kotoba.exe
pause
