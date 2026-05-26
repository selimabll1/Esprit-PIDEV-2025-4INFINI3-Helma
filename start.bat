@echo off
REM Script de démarrage pour Helma Bank
REM Ce script configure Java et démarre l'application

setlocal enabledelayedexpansion

echo.
echo ============================================
echo  Helma Bank - Script de Demarrage
echo ============================================
echo.

REM Chercher Java dans les chemin courants
echo Recherche de Java...

if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    echo [OK] Java trouve: !JAVA_HOME!
) else if exist "C:\Program Files\Java\jdk17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk17"
    echo [OK] Java trouve: !JAVA_HOME!
) else if exist "C:\Program Files\Java\openjdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\openjdk-17"
    echo [OK] Java trouve: !JAVA_HOME!
) else (
    echo [ERREUR] Java 17+ non trouve!
    echo.
    echo Veuillez installer Java 17 (JDK) depuis:
    echo https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
    echo.
    pause
    exit /b 1
)

REM Configurer JAVA_HOME et PATH
set "PATH=!JAVA_HOME!\bin;!PATH!"

REM Verifier que Java fonctionne
echo.
echo Verification de Java...
java -version

if %errorlevel% neq 0 (
    echo [ERREUR] Java n'a pas pu demarrer!
    pause
    exit /b 1
)

echo [OK] Java fonctionne correctement!
echo.

REM Demarrer l'application
echo Demarrage de Helma Bank...
echo.

call .\mvnw.cmd spring-boot:run

pause
