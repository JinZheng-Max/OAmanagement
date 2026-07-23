@echo off
title OA System - Quick Start

cd /d D:\QQ\download\OA-service\OAmanagement-main\OAmanagement-main
set JAVA_HOME=C:\Program Files\Java\jdk-21
set REDIS_PATH=D:\software\Redis
set LOCAL_DIR=D:\QQ\download\OA-service\OAmanagement-main\OAmanagement-main
set NACOS_DIR=D:\nacos

echo ============================================
echo      OA Management System - Quick Start
echo ============================================
echo.

:: Step 0: Check Nacos
echo [0/4] Checking Nacos...
curl -s --connect-timeout 3 http://localhost:8848/nacos/ >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Nacos is not running!
    echo Please start Nacos first:
    echo   cd /d %NACOS_DIR%\bin
    echo   startup.cmd -m standalone
    echo.
    pause
    exit /b 1
)
echo Nacos is running

:: Step 1: Start Redis
echo [1/4] Checking Redis...
redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo Starting Redis...
    start "Redis" "%REDIS_PATH%\redis-server.exe"
    timeout /t 3 /nobreak >nul
) else (
    echo Redis already running
)

:: Step 2: Start identity-service
echo [2/4] Starting identity-service...
start "identity-service" cmd /c "cd /d %LOCAL_DIR% && mvnw.cmd spring-boot:run -pl identity-service -Dmaven.test.skip=true"
echo Waiting 40 seconds...
timeout /t 40 /nobreak >nul

:: Step 3: Start gateway
echo [3/4] Starting oa-gateway...
start "oa-gateway" cmd /c "cd /d %LOCAL_DIR% && mvnw.cmd spring-boot:run -pl oa-gateway -Dmaven.test.skip=true"
echo Waiting 25 seconds...
timeout /t 25 /nobreak >nul

:: Step 4: Start frontend
echo [4/4] Starting frontend...
start "oa-web" cmd /c "cd /d %LOCAL_DIR%\oa-web && npm run dev"
timeout /t 5 /nobreak >nul

echo.
echo ============================================
echo      All services started!
echo      Frontend: http://localhost:5173
echo      Login: admin / Admin@123456
echo      Nacos: http://localhost:8848/nacos
echo ============================================
echo.
pause
