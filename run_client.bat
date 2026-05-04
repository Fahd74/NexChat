@echo off
title NexChat Client
echo [1/2] Compiling files...
javac common/*.java server/*.java client/*.java ui/*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed. Please check your code.
    pause
    exit /b %errorlevel%
)

echo [2/2] Starting GUI Client...
java ui.ChatGUI
pause
