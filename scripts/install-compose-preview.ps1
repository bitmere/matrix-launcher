param([Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,[switch]$Compile)
$ErrorActionPreference='Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot
$sdk=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{Join-Path $env:LOCALAPPDATA 'Android/Sdk'}
$adb=Join-Path $sdk 'platform-tools/adb.exe'
$package='com.matrix.launcher'
$manifest=Get-Content artifacts/release-manifest.json -Raw | ConvertFrom-Json
if($manifest.applicationId -ne $package){throw 'Build manifest is not the isolated Compose candidate'}
$apk=Join-Path $repoRoot "artifacts/$($manifest.apk)"
$hash=(Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
if($hash -ne $manifest.sha256){throw 'Candidate APK checksum does not match its build manifest'}
if($Serial -match ':'){
    & $adb connect $Serial
    if($LASTEXITCODE -ne 0){throw 'ADB connection failed'}
}
& $adb -s $Serial push $apk /data/local/tmp/matrix-compose-candidate.apk
if($LASTEXITCODE -ne 0){throw 'Candidate upload failed'}
$result=& $adb -s $Serial shell pm install -r /data/local/tmp/matrix-compose-candidate.apk
Write-Output $result
if($LASTEXITCODE -ne 0 -or "$result" -notmatch 'Success'){throw 'Candidate install failed'}
$installed=((& $adb -s $Serial shell pm path $package | Select-Object -First 1) -replace '^package:','').Trim()
if($installed -notmatch '^/data/app/[A-Za-z0-9_~=/+.-]+/base\.apk$'){throw 'Unexpected installed path'}
$deviceHash=(& $adb -s $Serial shell sha256sum $installed).Split(' ')[0].Trim().ToLowerInvariant()
if($LASTEXITCODE -ne 0 -or $deviceHash -ne $hash){throw 'Installed candidate differs from the measured APK'}
& $adb -s $Serial shell rm -f /data/local/tmp/matrix-compose-candidate.apk
if($Compile){
    $compiled=& $adb -s $Serial shell cmd package compile -m speed -f $package
    Write-Output $compiled
    if($LASTEXITCODE -ne 0 -or "$compiled" -notmatch 'Success'){throw 'App compilation failed; inspect the TV ART service'}
    # Firmware can substitute a profile-guided filter; report actual state, not just command success.
    & $adb -s $Serial shell dumpsys package $package | Select-String 'arm.*\[status='
}
& $adb -s $Serial shell am start -W -n "$package/com.matrix.launcher.ComposeHomeActivity"
if($LASTEXITCODE -ne 0){throw 'Candidate launch command failed; inspect logcat'}
Start-Sleep -Milliseconds 1500
$running=(& $adb -s $Serial shell pidof $package) -join ''
if(-not $running.Trim()){throw 'Candidate exited after launch; inspect its crash log before continuing'}
[ordered]@{version=$manifest.version;applicationId=$package;sha256=$hash;device=$Serial;compileRequested=[bool]$Compile;verifiedUtc=[DateTime]::UtcNow.ToString('o')} |
    ConvertTo-Json | Set-Content artifacts/compose-install-receipt.json -Encoding utf8
Write-Output "Verified candidate $($manifest.version), SHA256 $hash"
# No Home/accessibility/permission/default-app changes. TV-listings permission is requested in the UI.
