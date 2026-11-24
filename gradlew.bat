@echo off
setlocal
set APP_HOME=%~dp0
set GRADLE_VERSION=8.9
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set DIST_DIR=%GRADLE_USER_HOME%\wrapper\custom\gradle-%GRADLE_VERSION%
set ZIP_DIR=%GRADLE_USER_HOME%\wrapper\dists\custom-gradle-%GRADLE_VERSION%
set GRADLE_BIN=%DIST_DIR%\bin\gradle.bat
set ZIP_FILE=%ZIP_DIR%\gradle-%GRADLE_VERSION%-bin.zip

if not exist "%GRADLE_BIN%" (
  if not exist "%ZIP_DIR%" mkdir "%ZIP_DIR%"
  if not exist "%GRADLE_USER_HOME%\wrapper\custom" mkdir "%GRADLE_USER_HOME%\wrapper\custom"
  if not exist "%ZIP_FILE%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_FILE%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP_FILE%' -DestinationPath '%ZIP_DIR%' -Force"
  if errorlevel 1 exit /b 1
  if not exist "%DIST_DIR%" move "%ZIP_DIR%\gradle-%GRADLE_VERSION%" "%DIST_DIR%" >nul
)

call "%GRADLE_BIN%" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
