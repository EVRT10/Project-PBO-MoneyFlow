@echo off
echo =======================================
echo Building MoneyFlow Standalone App Image...
echo =======================================

rem Pastikan file MoneyFlow.jar sudah ada
if not exist "MoneyFlow.jar" (
    echo MoneyFlow.jar tidak ditemukan! Jalankan build_jar.bat terlebih dahulu.
    pause
    exit /b 1
)

rem Hapus folder temporary dan build lama jika ada agar bersih
if exist "dist" (
    echo Membersihkan build lama...
    rmdir /s /q dist
)
if exist "dist-input" (
    rmdir /s /q dist-input
)
if exist "custom-jre" (
    rmdir /s /q custom-jre
)

rem 1. Membuat Custom JRE stripped-down menggunakan jlink (Mengurangi ukuran drastis!)
echo Membuat runtime Java kustom yang ramping (jlink)...
jlink --add-modules java.base,java.desktop --strip-debug --no-man-pages --no-header-files --output custom-jre
if %errorlevel% neq 0 (
    echo Gagal membuat custom JRE! Pastikan JDK terdaftar di PATH komputer Anda.
    pause
    exit /b %errorlevel%
)

rem 2. Menyiapkan folder input bersih untuk jpackage
echo Menyiapkan file input...
mkdir dist-input
copy MoneyFlow.jar dist-input\

rem 3. Jalankan jpackage menggunakan folder input bersih dan custom JRE
echo Membuat standalone app image dengan ikon...
jpackage --input dist-input --dest dist --name "MoneyFlow" --main-jar MoneyFlow.jar --main-class MainApp --type app-image --icon moneyflow_icon.ico --runtime-image custom-jre
if %errorlevel% neq 0 (
    echo Gagal menjalankan jpackage!
    rmdir /s /q dist-input
    rmdir /s /q custom-jre
    pause
    exit /b %errorlevel%
)

rem 4. Otomatis menyalin database CSV ke folder output
echo Menyalin file database CSV...
copy data.csv "dist\MoneyFlow\"
copy categories.csv "dist\MoneyFlow\"

rem Menyalin folder emojis cache jika ada, atau buat folder kosong jika tidak ada agar compiler installer tidak error
if exist "emojis" (
    echo Menyalin folder cache emojis...
    xcopy /e /i /y emojis "dist\MoneyFlow\emojis"
) else (
    echo Membuat folder emojis kosong...
    mkdir "dist\MoneyFlow\emojis"
)

rem 5. Membersihkan folder temporary
echo Membersihkan file temporary build...
rmdir /s /q dist-input
rmdir /s /q custom-jre

echo =======================================
echo Build Standalone App Sukses di dist/MoneyFlow!
echo =======================================
pause
