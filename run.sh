#!/bin/bash

# 電影訂票系統執行腳本
# 適用於 Windows Git Bash 環境

echo "正在啟動電影訂票系統..."

# 檢查是否已編譯
if [ ! -d "bin" ] || [ ! -f "bin/ui/Main.class" ]; then
    echo "程式尚未編譯，正在執行編譯..."
    ./compile.sh
    if [ $? -ne 0 ]; then
        echo "編譯失敗，無法執行程式。"
        exit 1
    fi
fi

# 執行程式 (Windows 使用分號作為 classpath 分隔符)
java -cp "bin;lib/jbcrypt-0.4.jar;lib/jcalendar-1.4.jar;lib/sqlite-jdbc-3.49.1.0.jar" ui.Main
