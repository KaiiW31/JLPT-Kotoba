#define MyAppName "JLPT Kotoba"
#define MyAppVersion "1.4.1"
#define MyAppPublisher "KaiiW31"
#define MyAppURL "https://github.com/KaiiW31/JLPT-Kotoba"
#define MyAppExeName "JLPT Kotoba.exe"

[Setup]
AppId={{3A1B6222-C0FF-4A48-A9C0-7E32113ED433}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/issues
AppUpdatesURL={#MyAppURL}/releases
DefaultDirName={localappdata}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\..
OutputBaseFilename=JLPT Kotoba Setup
SetupIconFile=..\assets\app_icon.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
UninstallDisplayName={#MyAppName}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
CloseApplications=yes
RestartApplications=no
SetupLogging=yes
VersionInfoVersion=1.4.1.0
VersionInfoProductVersion={#MyAppVersion}
VersionInfoProductName={#MyAppName}
VersionInfoDescription={#MyAppName} Setup
VersionInfoCompany={#MyAppPublisher}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\dist\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
procedure CopyPortableDataIfNeeded(const FileName: String);
var
  SourcePath: String;
  DestinationPath: String;
begin
  SourcePath := ExpandConstant('{src}\' + FileName);
  DestinationPath := ExpandConstant('{app}\' + FileName);
  if FileExists(SourcePath) and not FileExists(DestinationPath) then
    CopyFile(SourcePath, DestinationPath, False);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    CopyPortableDataIfNeeded('flashcard_progress.json');
    CopyPortableDataIfNeeded('custom_vocabulary.json');
  end;
end;
