@echo off
rem Starts Pitiupi with the RPSLS plugin (Windows). Requires Java 17 or newer.
rem Abre o Pitiupi com o plugin RPSLS (Windows). Requer Java 17 ou mais novo.
cd /d "%~dp0"
where java >nul 2>nul || (echo Java not found / Java nao encontrado. Install Java 17+ ^(https://adoptium.net^). & pause & exit /b 1)
set "PITIUPI_HOME=%~dp0."
java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar pitiupi.jar
if errorlevel 1 pause
