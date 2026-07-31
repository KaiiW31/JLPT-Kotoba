@echo off
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME (
    echo JAVA_HOME is not set.
    exit /b 1
)

if not defined ANDROID_HOME (
    echo ANDROID_HOME is not set.
    exit /b 1
)

call gradlew.bat clean assembleDebug
if errorlevel 1 exit /b %errorlevel%

copy /y "app\build\outputs\apk\debug\app-debug.apk" "..\JLPT Kotoba.apk" >nul
echo Built: %~dp0..\JLPT Kotoba.apk
