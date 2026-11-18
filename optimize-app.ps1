param([Parameter(Mandatory=$true)][string]$TvHost,[switch]$Reset)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$taskSdk=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{Join-Path $env:LOCALAPPDATA 'Android/Sdk'}
$adb=Join-Path $taskSdk 'platform-tools/adb.exe'
if(-not(Test-Path $adb)){throw 'Set ANDROID_HOME to the Android SDK with platform-tools installed'}
$serial="${TvHost}:5555"
& $adb connect $serial
if($LASTEXITCODE -ne 0){throw 'ADB connection failed'}
New-Item -ItemType Directory -Force artifacts | Out-Null
$sdk=& $adb -s $serial shell getprop ro.build.version.sdk
if($LASTEXITCODE -ne 0 -or "$sdk" -notmatch '^\s*\d+\s*$'){throw 'Cannot read device SDK'}
if([int]$sdk -ge 34){& $adb -s $serial shell pm art dump com.matrix.launcher | Set-Content "artifacts/art-before-$(Get-Date -Format yyyyMMdd-HHmmss).txt"}
if($Reset){$result=& $adb -s $serial shell cmd package compile --reset com.matrix.launcher}
else{$result=& $adb -s $serial shell cmd package compile -m speed -f com.matrix.launcher}
Write-Output $result
if($LASTEXITCODE -ne 0 -or "$result" -notmatch 'Success'){throw 'ART compilation/reset did not report success'}
if([int]$sdk -ge 34){& $adb -s $serial shell pm art dump com.matrix.launcher | Select-String 'status=|reason='}
# App-only ART compilation. No global animation, power, thermal or background-process settings.
