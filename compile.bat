@echo off
chcp 65001 > nul
echo 正在編譯電影訂票系統...

REM 建立 bin 目錄
if not exist bin mkdir bin

REM 編譯所有 Java 檔案
echo 編譯 Java 檔案...
javac -d bin -cp "lib\jbcrypt-0.4.jar;lib\jcalendar-1.4.jar;lib\sqlite-jdbc-3.49.1.0.jar;src" src\ui\Main.java src\dao\*.java src\model\*.java src\service\*.java src\util\*.java src\exception\*.java

if %errorlevel% equ 0 (
    echo 編譯成功！
    echo.
    echo 使用以下指令執行程式：
    echo java -cp "bin;lib\jbcrypt-0.4.jar;lib\jcalendar-1.4.jar;lib\sqlite-jdbc-3.49.1.0.jar" ui.CinemaBookingGUI
    echo.
    echo 或者執行：
    echo run.bat
) else (
    echo 編譯失敗！請檢查錯誤訊息。
    pause
    exit /b 1
)

pause
