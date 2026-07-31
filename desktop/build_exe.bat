@echo off
cd /d "%~dp0"
py -m pip install -r requirements.txt
py -m PyInstaller --noconfirm --clean --windowed --onefile --name "JLPT Kotoba" --icon assets\app_icon.ico --add-data "assets;assets" --add-data "data;data" --collect-all customtkinter app.py
echo.
echo Done. Share this file:
echo %~dp0dist\JLPT Kotoba.exe
pause
