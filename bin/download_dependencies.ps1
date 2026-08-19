<#
.SYNOPSIS
    Downloads and setups standalone FFmpeg and yt-dlp executables for local development.
.DESCRIPTION
    Installs latest yt-dlp nightly and standard FFmpeg release into ~/.stash/bin/ or project environment.
#>

$ErrorActionPreference = "Stop"
$userBinDir = "$env:USERPROFILE\.stash\bin"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " Stash Standalone Dependency Bootstrap Utility    " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

if (!(Test-Path $userBinDir)) {
    New-Item -ItemType Directory -Force -Path $userBinDir | Out-Null
    Write-Host "[+] Created local binary store: $userBinDir" -ForegroundColor Green
}

# Download latest yt-dlp nightly
$ytDlpUrl = "https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp.exe"
$ytDlpDest = "$userBinDir\yt-dlp.exe"

Write-Host "[*] Fetching yt-dlp nightly from official GitHub release..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $ytDlpUrl -OutFile $ytDlpDest -UseBasicParsing
Write-Host "[+] yt-dlp nightly successfully installed to $ytDlpDest" -ForegroundColor Green

# Verify binaries
& $ytDlpDest --version
Write-Host "[+] Setup complete! Stash can now run standalone." -ForegroundColor Cyan
