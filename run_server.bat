@echo off
title NexChat Server
echo [1/2] Compiling files...
javac common/*.java server/*.java client/*.java ui/*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed. Please check your code.
    pause
    exit /b %errorlevel%
)

echo [2/2] Starting Server on port 20443...
java server.Server 20443
pause
