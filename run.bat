@echo off
echo Compiling Java files...
javac -cp ".;lib/ojdbc8.jar" src/*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Running the application...
java -cp "src;lib/ojdbc8.jar" HospitalManagementSystem

pause 