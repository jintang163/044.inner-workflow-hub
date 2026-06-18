@echo off
echo ========================================
echo  InnerWorkflow AI Service
echo ========================================
echo.

cd /d "%~dp0"

echo [1/4] Checking Python environment...
python --version
if %errorlevel% neq 0 (
    echo ERROR: Python is not installed or not in PATH
    pause
    exit /b 1
)

echo.
echo [2/4] Installing dependencies...
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo WARNING: Some dependencies may have failed to install
    echo Continuing anyway...
)

echo.
echo [3/5] Generating gRPC code from proto...
python -m grpc_tools.protoc -I./proto --python_out=./proto --grpc_python_out=./proto ./proto/approval_ai.proto
if %errorlevel% neq 0 (
    echo ERROR: Failed to generate gRPC code
    pause
    exit /b 1
)

echo.
echo [4/5] Fixing gRPC imports for package structure...
python -c "import sys; path='./proto/approval_ai_pb2_grpc.py'; content=open(path).read(); content=content.replace('import approval_ai_pb2 as', 'from . import approval_ai_pb2 as'); open(path,'w').write(content); print('Imports fixed successfully')"

echo.
echo [5/5] Starting AI gRPC server on port 50051...
echo.
echo Server logs will be displayed below.
echo Press Ctrl+C to stop the server.
echo.

python server.py

if %errorlevel% neq 0 (
    echo.
    echo Server stopped with error code %errorlevel%
    pause
    exit /b %errorlevel%
)

pause
