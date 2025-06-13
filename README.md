# 電影訂票系統 (Cinema Booking System)

這是一個使用 Java 開發的電影訂票系統，提供圖形化使用者介面，支援會員註冊、登入、電影瀏覽、座位選擇、訂票等功能。

## 系統特色

- **圖形化介面**：使用 Java Swing 開發的現代化GUI
- **座位選擇**：支援大廳/小廳兩種影廳類型，提供視覺化座位選擇
- **會員系統**：註冊、登入、密碼加密保護
- **管理員功能**：電影管理、場次管理、訂票管理
- **電影評論**：使用者可對電影發表評論
- **年齡分級檢查**：根據電影分級限制購票
- **資料持久化**：使用 SQLite 資料庫儲存所有資料

## 系統需求

### 軟體需求

- **Java Development Kit (JDK) 11 或以上版本**
- **Windows/Linux/macOS**（支援跨平台）

### 外部函式庫

系統使用以下外部函式庫（已包含在 `lib` 資料夾中）：

- `jbcrypt-0.4.jar` - 密碼加密
- `jcalendar-1.4.jar` - 日期選擇器元件
- `sqlite-jdbc-3.49.1.0.jar` - SQLite 資料庫連接

## 專案結構

```
oophomework-main/
├── README.md                    # 專案說明文件
├── cinema_booking.db           # SQLite 資料庫檔案（執行後自動產生）
├── sources.txt                 # 編譯用的原始檔案清單
├── bin/                        # 編譯後的 .class 檔案
├── data/                       # JSON 資料檔案
│   ├── movie_info.json         # 電影資訊
│   ├── big_room.json          # 大廳座位配置
│   └── small_room.json        # 小廳座位配置
├── lib/                        # 外部函式庫
│   ├── jbcrypt-0.4.jar
│   ├── jcalendar-1.4.jar
│   └── sqlite-jdbc-3.49.1.0.jar
└── src/                        # 原始程式碼
    ├── dao/                    # 資料存取層
    ├── exception/              # 自訂例外類別
    ├── model/                  # 資料模型
    ├── service/                # 業務邏輯層
    ├── ui/                     # 使用者介面
    └── util/                   # 工具類別
```

## 快速開始

### 1. 下載專案

```bash
# 如果是從 GitHub 下載
git clone [repository-url]
cd oophomework-main
```

### 2. 簡化的編譯和執行方法

#### 方法一：使用提供的腳本（推薦）

**Windows 使用者：**
```cmd
# 編譯程式
compile.bat

# 執行程式
run.bat
```

**Git Bash 使用者：**
```bash
# 編譯程式
./compile.sh

# 執行程式
./run.sh
```

#### 方法二：手動編譯

### 2a. 編譯專案

#### 在 Windows 使用 Command Prompt：

```cmd
# 建立 bin 目錄（如果不存在）
mkdir bin

# 編譯所有 Java 檔案
javac -d bin -cp "lib\jbcrypt-0.4.jar;lib\jcalendar-1.4.jar;lib\sqlite-jdbc-3.49.1.0.jar;src" src\ui\Main.java src\dao\*.java src\model\*.java src\service\*.java src\util\*.java src\exception\*.java
```

#### 在 Windows 使用 Git Bash：

```bash
# 建立 bin 目錄（如果不存在）
mkdir -p bin

# 編譯所有 Java 檔案（注意：在Windows下使用分號作為classpath分隔符）
javac -d bin -cp "lib/jbcrypt-0.4.jar;lib/jcalendar-1.4.jar;lib/sqlite-jdbc-3.49.1.0.jar;src" $(find src -name "*.java")
```

#### 在 Linux/macOS：

```bash
# 建立 bin 目錄（如果不存在）
mkdir -p bin

# 編譯所有 Java 檔案
javac -d bin -cp "lib/jbcrypt-0.4.jar:lib/jcalendar-1.4.jar:lib/sqlite-jdbc-3.49.1.0.jar:src" $(find src -name "*.java")
```

### 3. 執行程式

#### 在 Windows (Command Prompt 或 Git Bash)：

```cmd
java -cp "bin;lib\jbcrypt-0.4.jar;lib\jcalendar-1.4.jar;lib\sqlite-jdbc-3.49.1.0.jar" ui.Main
```

#### 在 Linux/macOS：

```bash
java -cp "bin:lib/jbcrypt-0.4.jar:lib/jcalendar-1.4.jar:lib/sqlite-jdbc-3.49.1.0.jar" ui.Main
```

## 使用說明

### 初次執行

1. **自動初始化**：首次執行時系統會自動建立資料庫並載入預設資料
2. **預設帳號**：
   - 管理員帳號：`admin@admin.com` / 密碼：`admin123`
   - 一般使用者：需要自行註冊

### 註冊新帳號

1. 點擊登入頁面的「前往註冊」按鈕
2. 填寫電子郵件、密碼、確認密碼和出生日期
3. 點擊「註冊」完成帳號建立

### 一般使用者功能

1. **瀏覽電影**：
   - 查看目前上映的電影
   - 閱讀電影描述和分級資訊
   - 瀏覽其他使用者的評論

2. **訂票流程**：
   - 選擇想看的電影
   - 選擇場次時間
   - 點擊「選擇座位」開啟座位選擇視窗
   - 在視覺化座位圖中選擇座位
   - 確認訂票

3. **管理訂票**：
   - 查看個人訂票記錄
   - 取消未過期的訂票

4. **電影評論**：
   - 對看過的電影發表評論

### 管理員功能

1. **電影管理**：
   - 新增電影（名稱、片長、描述、分級）
   - 移除電影

2. **場次管理**：
   - 新增場次（電影、影廳、時間）
   - 更新場次時間
   - 移除場次

3. **訂票管理**：
   - 查看所有訂票記錄
   - 更新訂票狀態（確認/取消）
   - 搜索和篩選訂票

4. **資料庫管理**：
   - 重設資料庫至預設狀態

## 系統架構

### 資料庫設計

系統使用 SQLite 資料庫，包含以下主要資料表：

- `member` - 會員資訊
- `movie` - 電影資訊
- `theater` - 影廳資訊
- `showtime` - 場次資訊
- `reservation` - 訂票記錄
- `reviews` - 電影評論

### 程式架構

採用分層架構設計：

1. **UI Layer** (`ui/`) - 使用者介面層
2. **Service Layer** (`service/`) - 業務邏輯層
3. **DAO Layer** (`dao/`) - 資料存取層
4. **Model Layer** (`model/`) - 資料模型層

## 常見問題

### Q: 在 Windows 環境下編譯失敗怎麼辦？

A: 請確認使用正確的 classpath 分隔符號：

```bash
# Windows 使用分號 (;)
javac -d bin -cp "lib/jbcrypt-0.4.jar;lib/jcalendar-1.4.jar;lib/sqlite-jdbc-3.49.1.0.jar;src" $(find src -name "*.java")

# Linux/macOS 使用冒號 (:)
javac -d bin -cp "lib/jbcrypt-0.4.jar:lib/jcalendar-1.4.jar:lib/sqlite-jdbc-3.49.1.0.jar:src" $(find src -name "*.java")
```

### Q: 出現 "package does not exist" 錯誤怎麼辦？

A: 確認外部函式庫檔案存在且路徑正確：

```bash
# 檢查 lib 資料夾中的 jar 檔案
ls -la lib/
# 應該看到：
# jbcrypt-0.4.jar
# jcalendar-1.4.jar  
# sqlite-jdbc-3.49.1.0.jar
```

### Q: 編譯時出現編碼錯誤怎麼辦？

A: 刪除 `sources.txt` 檔案，重新生成：

```bash
# Windows
dir /s /b src\*.java > sources.txt

# Linux/macOS
find src -name "*.java" > sources.txt
```

### Q: 無法啟動程式怎麼辦？

A: 檢查以下項目：

1. 確認 JDK 版本是 11 或以上
2. 確認所有 jar 檔案都在 `lib` 資料夾中
3. 確認 classpath 設定正確
4. 檢查檔案路徑分隔符號（Windows 用 `;`，Linux/macOS 用 `:`）

### Q: 資料庫損壞怎麼辦？

A: 刪除 `cinema_booking.db` 檔案，重新執行程式會自動重建資料庫。

### Q: 如何重設系統資料？

A: 使用管理員帳號登入，在「資料庫管理」頁面點擊「重設資料庫至預設狀態」。

## 開發者資訊

### 新增功能

要新增功能，請遵循現有的分層架構：

1. 在 `model/` 中定義資料模型
2. 在 `dao/` 中實作資料存取方法
3. 在 `service/` 中實作業務邏輯
4. 在 `ui/` 中實作使用者介面

### 資料庫變更

如需修改資料庫結構，請更新 `util/DBUtil.java` 中的 `initializeDatabase()` 方法。

## 授權

本專案僅供學術用途使用。

## 技術支援

如遇到技術問題，請檢查：

1. Java 版本是否正確
2. 外部函式庫是否完整
3. 檔案路徑是否正確
4. 資料庫檔案權限是否正常

---

**注意**：本系統設計用於教學目的，請勿用於商業用途。
