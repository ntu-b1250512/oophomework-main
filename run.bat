@echo off
chcp 65001 > nul
echo 正在啟動電影訂票系統...

REM 檢查是否已編譯
if not exist bin\ui\Main.class (
    echo 程式尚未編譯，正在執行編譯...
    call compile.bat
    if %errorlevel% neq 0 (
        echo 編譯失敗，無法執行程式。
        pause
        exit /b 1
    )
)

REM 執行程式
java -cp "bin;lib\jbcrypt-0.4.jar;lib\jcalendar-1.4.jar;lib\sqlite-jdbc-3.49.1.0.jar" ui.CinemaBookingGUI

pause
