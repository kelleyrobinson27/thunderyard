@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>&1
if errorlevel 1 (
  echo Docker is required. Install Docker Desktop or another Compose runtime.
  exit /b 1
)

if /i not "%~1"=="down" goto start

docker compose down %2 %3 %4 %5
if errorlevel 1 exit /b 1
echo Kafka stopped.
exit /b 0

:start
echo Starting Kafka via docker compose...
docker compose up -d --wait --wait-timeout 120
if errorlevel 1 exit /b 1

echo.
docker compose ps
echo.
echo Topics:
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
if errorlevel 1 exit /b 1

echo.
echo Kafka is listening on localhost:9092.
echo Next:
echo   1. Postgres on localhost:5432 - see README Database
echo   2. mvnw.cmd spring-boot:run   or IntelliJ DemoApplication
echo   3. src\test\curl\runAll.sh    from Cygwin or Git Bash
echo.
echo If the Spring app was already running, restart it so consumers attach.
exit /b 0
