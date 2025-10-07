@echo off
echo Finding network IP...

REM A more robust way to find the primary IPv4 address
for /f "tokens=*" %%a in ('powershell -NoProfile -Command "Get-NetIPAddress -AddressFamily IPv4 -AddressState Preferred | Where-Object { ($_.InterfaceAlias -like '*Ethernet*' -or $_.InterfaceAlias -like '*Wi-Fi*') -and ($_.PrefixOrigin -ne 'WellKnown') } | Select-Object -ExpandProperty IPAddress -First 1"') do (
    set "NetworkIP=%%a"
)

if not defined NetworkIP (
    echo WARNING: Could not automatically determine IP address. Falling back to hostname.
    set "NetworkIP=%COMPUTERNAME%"
)

echo Network IP: %NetworkIP%

REM Set the DOCKERHOST variable for docker-compose to use
set DOCKERHOST=%NetworkIP%

echo Starting Docker Compose...

REM %~dp0 is a special variable that expands to the directory of the script.
REM This makes the script runnable from any location.
docker-compose -f "%~dp0docker-compose.yml" up --build