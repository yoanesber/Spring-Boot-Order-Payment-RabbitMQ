@echo off
setlocal

REM Get host and port from command line arguments
set HOST=%1
set PORT=%2

REM If no arguments are provided, set default values
if "%HOST%"=="" set HOST=localhost
if "%PORT%"=="" set PORT=5672

REM Set maximum retries and delay between retries
set MAX_RETRIES=15
set RETRY_DELAY=2
set RETRY_COUNT=0

echo Waiting for RabbitMQ to be ready on %HOST%:%PORT%...

:retry
REM Use PowerShell to check RabbitMQ connection
powershell -Command "exit !(Test-NetConnection -ComputerName %HOST% -Port %PORT% -InformationLevel Quiet)"
if %ERRORLEVEL%==0 (
    echo RabbitMQ is up!
    goto done
)

set /a RETRY_COUNT+=1
if %RETRY_COUNT% GEQ %MAX_RETRIES% (
    echo Timeout reached. RabbitMQ is not ready.
    exit /b 1
)

echo RabbitMQ not ready yet... retrying in %RETRY_DELAY% seconds...
timeout /t %RETRY_DELAY% >nul
goto retry

:done
exit /b 0
