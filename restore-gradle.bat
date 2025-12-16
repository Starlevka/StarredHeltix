@echo off
echo Restoring Gradle Wrapper files...

REM Create gradle/wrapper directory if it doesn't exist
if not exist "gradle\wrapper" mkdir gradle\wrapper

REM Download gradle-wrapper.jar
echo Downloading gradle-wrapper.jar...
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar', 'gradle\wrapper\gradle-wrapper.jar')"

REM Download gradle-wrapper.properties
echo Downloading gradle-wrapper.properties...
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties', 'gradle\wrapper\gradle-wrapper.properties')"

echo Gradle wrapper restored!
pause
