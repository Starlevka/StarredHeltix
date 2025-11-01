@echo off
REM start-server.bat
REM Script to start the StarredHeltix Network Server on Windows

echo Starting StarredHeltix Network Server...

REM Check if Node.js is installed
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Node.js is not installed. Please install Node.js to run the server.
    exit /b 1
)

REM Check if npm is installed
npm --version >nul 2>&1
if %errorlevel% neq 0 (
    echo npm is not installed. Please install Node.js (which includes npm) to run the server.
    exit /b 1
)

REM Install dependencies if node_modules doesn't exist
if not exist "node_modules" (
    echo Installing dependencies...
    npm install
)

REM Start the server
echo Server starting on http://localhost:3000
echo Press Ctrl+C to stop the server
node server.js