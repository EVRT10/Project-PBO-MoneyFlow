@echo off
echo =======================================
echo Building MoneyFlow Runnable JAR...
echo =======================================

rem Create build directory if it doesn't exist
if not exist "bin" mkdir "bin"

rem Compile the Java files
echo Compiling source files...
javac -d bin src/*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

rem Create the manifest file
echo Creating manifest...
echo Manifest-Version: 1.0 > manifest.txt
echo Main-Class: MainApp >> manifest.txt
echo. >> manifest.txt

rem Build the JAR
echo Packaging JAR file...
jar cfm MoneyFlow.jar manifest.txt -C bin .
if %errorlevel% neq 0 (
    echo Packaging failed!
    pause
    exit /b %errorlevel%
)

rem Clean up temporary files
echo Cleaning up temporary files...
del manifest.txt
rmdir /s /q bin

echo =======================================
echo Build Successful! Created MoneyFlow.jar
echo =======================================
pause
