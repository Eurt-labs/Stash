[Setup]
AppName=Stash
AppVersion=1.2.0
AppPublisher=Eurt-labs
DefaultDirName={localappdata}\Programs\Stash
DefaultGroupName=Stash
DisableProgramGroupPage=yes
OutputDir=build-installer
OutputBaseFilename=StashSetup
Compression=lzma2/max
SolidCompression=yes
PrivilegesRequired=lowest
UninstallDisplayIcon={app}\Stash.exe
DisableWelcomePage=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a shortcut on the Desktop"; GroupDescription: "Additional shortcuts:"
Name: "verifydeps"; Description: "Verify that 'yt-dlp' and 'ffmpeg' are installed (Important)"; GroupDescription: "Additional options:"; Flags: checkedonce

[Files]
Source: "app\build\compose\binaries\main\app\Stash\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Stash"; Filename: "{app}\Stash.exe"
Name: "{userdesktop}\Stash"; Filename: "{app}\Stash.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\Stash.exe"; Description: "Launch Stash"; Flags: nowait postinstall skipifsilent

[Code]
function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = wpSelectTasks then
  begin
    if not WizardIsTaskSelected('verifydeps') then
    begin
      if MsgBox('Warning: Stash requires ''yt-dlp'' and ''ffmpeg'' to function. Running without them will cause downloads to fail. Are you sure you want to proceed?', mbConfirmation, MB_YESNO) = IDNO then
      begin
        Result := False;
      end;
    end;
  end;
end;
