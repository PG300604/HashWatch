<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM Includes automatic JAVA_HOME validation & recovery after JDK updates
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set "WRAPPER_DIR=%~dp0"
set "WRAPPER_PS1=%WRAPPER_DIR%mvnw.cmd"

@REM Validate JAVA_HOME and auto-recover if a system update upgraded the JDK directory
if not exist "%JAVA_HOME%\bin\java.exe" (
    for /f "usebackq tokens=*" %%j in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$env:RESOLVE_JAVA_HOME='1'; Invoke-Expression $([System.IO.File]::ReadAllText($env:WRAPPER_PS1))"` ) do (
        set "JAVA_HOME=%%j"
        set "PATH=%%j\bin;%PATH%"
    )
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-Expression $([System.IO.File]::ReadAllText($env:WRAPPER_PS1))"
if errorlevel 1 exit /b 1

for /f "usebackq tokens=*" %%i in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$env:MVN_CMD_PATH_CHECK='1'; Invoke-Expression $([System.IO.File]::ReadAllText($env:WRAPPER_PS1))"` ) do set "MVN_CMD=%%i"

call "%MVN_CMD%" %*
exit /b %ERRORLEVEL%
: end batch / begin powershell #>

$ErrorActionPreference = "Stop"

if ($env:RESOLVE_JAVA_HOME -eq '1') {
    $candidates = @(
        [Environment]::GetEnvironmentVariable("JAVA_HOME", "User"),
        [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
    )
    foreach ($cand in $candidates) {
        if ($cand) {
            $trimmed = $cand.TrimEnd('\', '/')
            if (Test-Path (Join-Path $trimmed "bin\java.exe")) {
                Write-Output $trimmed
                exit 0
            }
        }
    }
    $searchDirs = @("C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java", "C:\Program Files\Microsoft")
    foreach ($dir in $searchDirs) {
        if (Test-Path $dir) {
            $jdks = Get-ChildItem -Path $dir -Directory -Filter "jdk*" | Sort-Object Name -Descending
            foreach ($jdk in $jdks) {
                if (Test-Path (Join-Path $jdk.FullName "bin\java.exe")) {
                    Write-Output $jdk.FullName
                    exit 0
                }
            }
        }
    }
    exit 0
}

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
