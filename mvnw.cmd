<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set "WRAPPER_DIR=%~dp0"
set "WRAPPER_PS1=%WRAPPER_DIR%mvnw.cmd"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-Expression $([System.IO.File]::ReadAllText($env:WRAPPER_PS1))"
if errorlevel 1 exit /b 1

for /f "usebackq tokens=*" %%i in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$env:MVN_CMD_PATH_CHECK='1'; Invoke-Expression $([System.IO.File]::ReadAllText($env:WRAPPER_PS1))"` ) do set "MVN_CMD=%%i"

call "%MVN_CMD%" %*
exit /b %ERRORLEVEL%
: end batch / begin powershell #>

$ErrorActionPreference = "Stop"
$wrapperDir = $env:WRAPPER_DIR
$propsFile = Join-Path $wrapperDir ".mvn\wrapper\maven-wrapper.properties"
$props = Get-Content $propsFile | Where-Object { $_ -match '=' -and -not $_.StartsWith('#') }
$distUrl = ""
foreach ($line in $props) {
    $parts = $line.Split('=', 2)
    if ($parts[0].Trim() -eq "distributionUrl") {
        $distUrl = $parts[1].Trim()
    }
}

$zipName = Split-Path -Leaf $distUrl
$baseName = $zipName -replace '-bin\.zip$', ''
$m2WrapperDir = Join-Path $HOME ".m2\wrapper\dists\$baseName"
$mvnHome = Join-Path $m2WrapperDir $baseName
$mvnCmd = Join-Path $mvnHome "bin\mvn.cmd"

if ($env:MVN_CMD_PATH_CHECK -eq '1') {
    Write-Output $mvnCmd
    exit 0
}

if (-not (Test-Path $mvnCmd)) {
    Write-Host "[Maven Wrapper] Downloading $distUrl ..."
    New-Item -ItemType Directory -Force -Path $m2WrapperDir | Out-Null
    $zipPath = Join-Path $m2WrapperDir $zipName
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $distUrl -OutFile $zipPath
    Write-Host "[Maven Wrapper] Extracting $zipName ..."
    Expand-Archive -Path $zipPath -DestinationPath $m2WrapperDir -Force
    Remove-Item $zipPath -Force
}
