$jlptDesktopDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$jlptPackagedExecutable = Join-Path $jlptDesktopDirectory "dist\JLPT Kotoba.exe"
$jlptInstallerScript = Join-Path $jlptDesktopDirectory "installer\JLPT Kotoba.iss"
$jlptLocalPrograms = Join-Path ([Environment]::GetFolderPath("LocalApplicationData")) "Programs"
$jlptCompilerCandidates = @(
    (Join-Path $jlptLocalPrograms "Inno Setup 6\ISCC.exe"),
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
    "C:\Program Files\Inno Setup 6\ISCC.exe"
)
$jlptCompiler = $jlptCompilerCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

if (-not (Test-Path -LiteralPath $jlptPackagedExecutable)) {
    throw "Build the desktop executable first: $jlptPackagedExecutable"
}

if (-not $jlptCompiler) {
    throw "Inno Setup 6 is required. Download it from https://jrsoftware.org/isdl.php"
}

& $jlptCompiler $jlptInstallerScript
if ($LASTEXITCODE -ne 0) {
    throw "Inno Setup failed with exit code $LASTEXITCODE"
}

$jlptOutput = Join-Path (Split-Path -Parent $jlptDesktopDirectory) "JLPT Kotoba Setup.exe"
if (-not (Test-Path -LiteralPath $jlptOutput)) {
    throw "Installer output was not created: $jlptOutput"
}

Get-Item -LiteralPath $jlptOutput | Select-Object FullName, Length, LastWriteTime
