@echo off
REM ============================================
REM Pacman AI Project - Build Script
REM ============================================

echo.
echo ========================================
echo    Building Pacman AI Project
echo ========================================
echo.

REM Create directories
echo [1/5] Creating directories...
if not exist "dist" mkdir dist
if not exist "build" mkdir build
if not exist "build\classes" mkdir build\classes

REM Clean previous build
echo [2/5] Cleaning previous build...
if exist "build\classes\*" del /Q /S build\classes\* >nul 2>&1

REM Compile all Java files
echo [3/5] Compiling Java files...
javac -cp "src;libs\*" -d build\classes src\client\*.java src\server\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

REM Copy resources to build directory
echo [4/5] Copying resources...
if not exist "build\classes\resources" mkdir build\classes\resources
copy /Y resources\*.png build\classes\resources\ >nul 2>&1
if not exist "build\classes\resources\sounds" mkdir build\classes\resources\sounds
if exist "resources\sounds\*" copy /Y resources\sounds\* build\classes\resources\sounds\ >nul 2>&1

REM Create JAR file
echo [5/5] Creating JAR file...
cd build\classes
jar cfm ..\..\dist\PacmanAI.jar ..\..\MANIFEST.MF *
cd ..\..

REM Copy resources to dist for running from dist folder
if not exist "dist\resources" mkdir dist\resources
copy /Y resources\*.png dist\resources\ >nul 2>&1
if not exist "dist\resources\sounds" mkdir dist\resources\sounds
if exist "resources\sounds\*" copy /Y resources\sounds\* dist\resources\sounds\ >nul 2>&1
copy /Y Readme.md dist\ >nul 2>&1
copy /Y RUNNING_GUIDE.md dist\ >nul 2>&1

REM Copy source code to dist
echo Creating source archive...
if not exist "dist\src" mkdir dist\src
if not exist "dist\src\client" mkdir dist\src\client
if not exist "dist\src\server" mkdir dist\src\server
copy /Y src\client\*.java dist\src\client\ >nul 2>&1
copy /Y src\server\*.java dist\src\server\ >nul 2>&1
copy /Y src\server\README.md dist\src\server\ >nul 2>&1

REM Copy test files
if not exist "dist\test" mkdir dist\test
copy /Y test\*.java dist\test\ >nul 2>&1

REM Copy libs
if not exist "dist\libs" mkdir dist\libs
copy /Y libs\*.jar dist\libs\ >nul 2>&1

echo.
echo ========================================
echo    Build Complete!
echo ========================================
echo.
echo Output files in 'dist' folder:
echo   - PacmanAI.jar (executable)
echo   - src\client\ (client source code)
echo   - src\server\ (server source code)
echo   - test\ (test files)
echo   - libs\ (dependencies)
echo   - resources\ (images and sounds)
echo.
echo To run: java -jar dist\PacmanAI.jar
echo.
pause
