$AppResourcesDir = Join-Path (Get-Location) "app-resources\windows"
if (!(Test-Path $AppResourcesDir)) {
    New-Item -ItemType Directory -Force -Path $AppResourcesDir
}

# 1. Download yt-dlp.exe
Write-Host "Downloading yt-dlp.exe..."
$YtDlpUrl = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"
Invoke-WebRequest -Uri $YtDlpUrl -OutFile (Join-Path $AppResourcesDir "yt-dlp.exe")
Write-Host "yt-dlp.exe downloaded."

# 2. Download ffmpeg release essentials
Write-Host "Downloading ffmpeg-release-essentials.zip..."
$FfmpegUrl = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
$TempZip = Join-Path (Get-Location) "ffmpeg-temp.zip"
$TempExtract = Join-Path (Get-Location) "ffmpeg-extract"

Invoke-WebRequest -Uri $FfmpegUrl -OutFile $TempZip
Write-Host "ffmpeg zip downloaded. Extracting..."

Expand-Archive -Path $TempZip -DestinationPath $TempExtract -Force

# Locate bin folder inside extracted folder
$BinFolder = Get-ChildItem -Path $TempExtract -Recurse -Directory | Where-Object { $_.Name -eq "bin" } | Select-Object -First 1

if ($BinFolder) {
    Write-Host "Copying ffmpeg.exe and ffprobe.exe..."
    Copy-Item -Path (Join-Path $BinFolder.FullName "ffmpeg.exe") -Destination (Join-Path $AppResourcesDir "ffmpeg.exe") -Force
    Copy-Item -Path (Join-Path $BinFolder.FullName "ffprobe.exe") -Destination (Join-Path $AppResourcesDir "ffprobe.exe") -Force
    Write-Host "Copy complete."
} else {
    Write-Error "bin folder not found in ffmpeg archive!"
}

# Cleanup
Write-Host "Cleaning up temporary files..."
Remove-Item -Path $TempZip -Force
Remove-Item -Path $TempExtract -Recurse -Force
Write-Host "Dependencies downloaded and placed in app-resources/windows/"
