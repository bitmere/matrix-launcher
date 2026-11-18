param([switch]$Checks,[ValidateSet('Release','Preview','Check','Test')][string]$Mode='Release',[string[]]$Tests=@())
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
if($Tests.Count -gt 0 -and $Mode -ne 'Test'){throw '-Tests requires -Mode Test'}
# Release signing is available only through GitHub Actions secrets. Check/Test
# remain keyless; Preview is the local installable iteration path.
if($Mode -eq 'Release' -and $env:GITHUB_ACTIONS -ne 'true'){
    throw 'Use GitHub Actions for signed releases; use -Mode Preview, Check or Test locally'
}
function Test-TaskJdk17([string]$directory){
    if(-not $directory -or -not (Test-Path "$directory/release")){return $false}
    $release=Get-Content "$directory/release" -Raw
    return $release -match 'JAVA_VERSION="17\.'
}
if(-not (Test-TaskJdk17 $env:JAVA_HOME)){
    $localBuild=Join-Path $PSScriptRoot 'build.local.properties'
    if(Test-Path $localBuild){
        $jdkLine=Get-Content $localBuild | Where-Object {$_ -match '^javaHome='} | Select-Object -First 1
        if($jdkLine){$env:JAVA_HOME=$jdkLine.Substring(9).Trim()}
    }
}
if(-not (Test-TaskJdk17 $env:JAVA_HOME) -or -not (Test-Path "$env:JAVA_HOME/bin/java.exe")){throw 'Set JAVA_HOME to a JDK 17 installation'}
if(-not $env:ANDROID_HOME){$env:ANDROID_HOME=Join-Path $env:LOCALAPPDATA 'Android/Sdk'}
if(-not (Test-Path $env:ANDROID_HOME)){throw 'Set ANDROID_HOME to your Android SDK directory'}
$timer=[Diagnostics.Stopwatch]::StartNew()
$variant=if($Mode -eq 'Preview'){'Preview'}else{'Release'}
$tasks=@()
if($Mode -eq 'Test'){
    if($Tests.Count -eq 0){$tasks+=@(':core:test',":app:test${variant}UnitTest")}
}
elseif($Checks -or $Mode -eq 'Check'){$tasks+=@(':core:test',":app:test${variant}UnitTest",":app:lint${variant}")}
if($Mode -notin @('Check','Test')){$tasks+="assemble${variant}"}
# Keep Gradle's daemon warm; Check never invokes R8 or packages an APK.
$gradleArgs=@($tasks)
if($Mode -eq 'Test' -and $Tests.Count -gt 0){
    # --tests applies to the preceding task: keep core/app filters scoped to their module.
    $coreFilters=@($Tests | Where-Object {$_ -like 'com.matrix.launcher.core.*'})
    $appFilters=@($Tests | Where-Object {$_ -notlike 'com.matrix.launcher.core.*'})
    $tasks=@();$gradleArgs=@()
    if($coreFilters.Count -gt 0){
        $tasks+=':core:test';$gradleArgs+=':core:test'
        foreach($pattern in $coreFilters){$gradleArgs+=@('--tests',$pattern)}
    }
    if($appFilters.Count -gt 0){
        $task=":app:test${variant}UnitTest";$tasks+=$task;$gradleArgs+=$task
        foreach($pattern in $appFilters){$gradleArgs+=@('--tests',$pattern)}
    }
}
& ./gradlew.bat @gradleArgs --console=plain
if($LASTEXITCODE -ne 0){throw 'Custom launcher build failed'}
Write-Output ("{0} finished in {1:N1}s" -f $Mode,$timer.Elapsed.TotalSeconds)
New-Item -ItemType Directory -Force artifacts | Out-Null
@{mode=$Mode;checks=[bool]($Checks -or $Mode -in @('Check','Test'));seconds=[math]::Round($timer.Elapsed.TotalSeconds,2);tasks=$tasks;filters=$Tests;utc=[DateTime]::UtcNow.ToString('o')} |
    ConvertTo-Json -Compress | Add-Content artifacts/build-timings.jsonl
if($Mode -in @('Check','Test')){return}
$variantFolder=$variant.ToLowerInvariant()
$output=if($Mode -eq 'Preview'){'artifacts/matrix-preview.apk'}else{'artifacts/personal-home.apk'}
Copy-Item "app/build/outputs/apk/$variantFolder/app-$variantFolder.apk" $output
Get-FileHash $output -Algorithm SHA256
if($Mode -eq 'Release'){
    $metadata=Get-Content app/build/outputs/apk/release/output-metadata.json -Raw | ConvertFrom-Json
    $version=$metadata.elements[0].versionName
    $name="Matrix-Launcher-v$version.apk"
    $versioned=Join-Path artifacts $name
    Copy-Item $output $versioned
    $hash=(Get-FileHash $versioned -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $name" | Set-Content "$versioned.sha256" -Encoding ascii
    [ordered]@{version=$version;versionCode=$metadata.elements[0].versionCode;applicationId=$metadata.applicationId;channel=$(if($version -match '-beta\.'){'beta'}else{'stable'});apk=$name;sha256=$hash;buildType='release';utc=[DateTime]::UtcNow.ToString('o')} |
        ConvertTo-Json | Set-Content artifacts/release-manifest.json -Encoding utf8
}
