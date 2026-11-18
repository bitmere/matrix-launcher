param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,
    [switch]$FromClipboard,
    [string]$PublicText,
    [switch]$Append
)
$ErrorActionPreference='Stop'
$sdk=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{Join-Path $env:LOCALAPPDATA 'Android/Sdk'}
$adb=Join-Path $sdk 'platform-tools/adb.exe'
if(-not(Test-Path $adb)){throw 'Set ANDROID_HOME to an SDK containing platform-tools'}
# Never paste into another app or an unfocused field. No dumps are written to disk.
$window=(& $adb -s $Serial shell dumpsys window | Select-String 'mCurrentFocus=') -join "`n"
$ime=(& $adb -s $Serial shell dumpsys input_method) -join "`n"
if($window -notmatch 'com\.matrix\.launcher(?:\.preview)?/' -or
   $ime -notmatch 'mInputShown=true' -or $ime -notmatch 'packageName=com\.matrix\.launcher(?:\.preview)?(?:\s|$)'){
    throw 'Open the API text field in Matrix Compose and press OK to show its keyboard first.'
}
if($FromClipboard -and $PublicText){throw 'Choose clipboard or PublicText, not both'}
$value=$null
try {
    if($FromClipboard){$value=Get-Clipboard -Raw}
    elseif($PublicText){$value=$PublicText}
    else {
        $secure=Read-Host 'Paste the value, then press Enter (hidden; not saved)' -AsSecureString
        $pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try{$value=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)}
        finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer);$secure.Dispose()}
    }
    $value=$value.Trim()
    # Only credential-safe characters understood literally by Android's input command.
    # Spaces, percent substitutions and all shell metacharacters are refused.
    if($value -notmatch '^[A-Za-z0-9._~:/+=-]{1,2048}$'){
        throw 'Unsupported characters. Use the phone keyboard for text containing spaces or other symbols.'
    }
    if(-not $Append){
        $result=& $adb -s $Serial shell input keycombination 113 29 2>&1
        if($LASTEXITCODE -ne 0){throw 'Select-all is unavailable on this TV. Clear the field manually, then use -Append.'}
    }
    $result=& $adb -s $Serial shell input text $value 2>&1
    if($LASTEXITCODE -ne 0){throw 'TV input failed. Check the selected field.'}
    Write-Output 'Pasted into the selected field. Review on the TV, then choose Save yourself.'
} finally {
    $value=$null;$result=$null;$ime=$null
    Remove-Variable value,result,ime -ErrorAction SilentlyContinue
}
# Never submits the form, logs the value, stores credentials or changes another app's account.
