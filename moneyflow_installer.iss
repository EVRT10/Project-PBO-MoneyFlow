; Script Inno Setup untuk membuat installer MoneyFlow Pro
; Buka file ini di Inno Setup Compiler dan klik Run/Compile untuk membuat Installer.

#define MyAppName "MoneyFlow Pro"
#define MyAppVersion "1.0"
#define MyAppPublisher "MoneyFlow Team"
#define MyAppExeName "MoneyFlow.exe"

[Setup]
; AppId secara unik mengidentifikasi aplikasi ini di Windows (digunakan untuk Uninstaller).
AppId={{D1A2E3C4-B5A6-4C5D-9E8F-7A6B5C4D3E2F}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}

; 1. Default ke folder Program Files standar
DefaultDirName={autopf}\{#MyAppName}
DisableProgramGroupPage=yes

; Meminta hak akses Administrator agar bisa menginstal ke Program Files
PrivilegesRequired=admin

; Icon kustom untuk file installer MoneyFlowSetup.exe
SetupIconFile=moneyflow_icon.ico

; Gambar kustom untuk installer wizard (Welcome & Finished page)
WizardImageFile=moneyflow_setup_large.bmp

; Ikon kecil kustom di sudut kanan atas halaman wizard
WizardSmallImageFile=moneyflow_setup_small.bmp

OutputDir=dist-installer
OutputBaseFilename=MoneyFlowSetup
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; File executable utama
Source: "dist\MoneyFlow\MoneyFlow.exe"; DestDir: "{app}"; Flags: ignoreversion
; File konfigurasi kategori
Source: "dist\MoneyFlow\categories.csv"; DestDir: "{app}"; Flags: ignoreversion
; File database transaksi (PENTING: onlyifdoesntexist agar data transaksi lama user tidak terhapus saat instal ulang/update!)
Source: "dist\MoneyFlow\data.csv"; DestDir: "{app}"; Flags: ignoreversion onlyifdoesntexist
; Folder aplikasi (JAR & Class Files) dan JRE runtime
Source: "dist\MoneyFlow\app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "dist\MoneyFlow\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
; Folder cache emojis jika sudah terbuat
Source: "dist\MoneyFlow\emojis\*"; DestDir: "{app}\emojis"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\{#MyAppExeName}"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]

// 2. Kustomisasi Tema Visual agar Senada dengan MoneyFlow
procedure InitializeWizard();
begin
  // Mengubah font default ke Segoe UI agar lebih modern
  WizardForm.Font.Name := 'Segoe UI';

  // Mengubah warna latar belakang wizard halaman dalam (Inner Page)
  // BGR format: $FAF0F3 = RGB(243, 240, 250) (Sama dengan BG_MAIN / Ungu sangat terang di aplikasi)
  WizardForm.InnerPage.Color := $FAF0F3;
  WizardForm.Color := $FAF0F3;

  // Mengubah warna panel header atas halaman dalam menjadi ungu gelap senada dengan sidebar
  // BGR format: $380E1C = RGB(28, 14, 56) (Sama dengan BG_SIDEBAR / Daisy Bush di aplikasi)
  WizardForm.MainPanel.Color := $380E1C;

  // Menyesuaikan warna teks pada header panel atas agar terbaca jelas (putih)
  WizardForm.PageNameLabel.Font.Color := $FFFFFF;
  WizardForm.PageNameLabel.Font.Style := [fsBold];
  
  WizardForm.PageDescriptionLabel.Font.Color := $D8D5E5; // Ungu muda sangat terang
end;
