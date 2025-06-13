#!/bin/bash

# 電影訂票系統編譯腳本
# 適用於 Windows Git Bash 環境

echo "正在編譯電影訂票系統..."

# 建立 bin 目錄
mkdir -p bin

# 編譯所有 Java 檔案 (Windows 使用分號作為 classpath 分隔符)
echo "編譯 Java 檔案..."
javac -d bin -cp "lib/jbcrypt-0.4.jar;lib/jcalendar-1.4.jar;lib/sqlite-jdbc-3.49.1.0.jar;src" $(find src -name "*.java")

if [ $? -eq 0 ]; then
    echo "編譯成功！"
    echo ""
    echo "使用以下指令執行程式："
    echo "java -cp \"bin;lib/jbcrypt-0.4.jar;lib/jcalendar-1.4.jar;lib/sqlite-jdbc-3.49.1.0.jar\" ui.Main"
    echo ""
    echo "或者執行："
    echo "./run.sh"
else
    echo "編譯失敗！請檢查錯誤訊息。"
    exit 1
fi
