param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AdbArgs
)
$ErrorActionPreference = 'Continue'
$adb = if ($env:ANDROID_HOME -and (Test-Path (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'))) {
    Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
} else {
    'adb.exe'
}
$serial = if ($env:TV_SERIAL) { $env:TV_SERIAL } else { '192.168.88.10:5555' }

# Ensure connected to TV
& $adb connect $serial 2>$null | Out-Null

if ($AdbArgs.Count -eq 0) {
    & $adb -s $serial shell
} else {
    & $adb -s $serial @AdbArgs
}
