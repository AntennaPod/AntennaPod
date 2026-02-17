@echo off
setlocal
set VERSION=%1
if "%VERSION%"=="" (
    echo Usage: %~n0 ^<version^>
    exit /b 1
)
set ROOT=%~dp0..\
set OUTPUT_DIR=%ROOT%archives
set APK=%ROOT%app\build\outputs\apk\free\debug\app-free-debug.apk
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if not exist "%APK%" (
    echo APK not found at %APK%
    echo Build it first, e.g. .\gradlew.bat assembleFreeDebug
    exit /b 1
)
set DEST=%OUTPUT_DIR%\%VERSION%.zip
powershell -NoLogo -Command "Compress-Archive -Path '%APK%' -DestinationPath '%DEST%' -Force" >nul
if %ERRORLEVEL% NEQ 0 (
    echo Failed to create archive.
    exit /b 1
)
echo Archived %APK% to %DEST%
endlocal
