$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$App = Join-Path $Root "app"
$Build = Join-Path $Root "build"
$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
$Bt = Join-Path $Sdk "build-tools\35.0.0"
$Plat = Join-Path $Sdk "platforms\android-35\android.jar"
$Jbr = "C:\Program Files\Android\Android Studio\jbr\bin"

$Aapt2 = Join-Path $Bt "aapt2.exe"
$D8 = Join-Path $Bt "d8.bat"
$Zipalign = Join-Path $Bt "zipalign.exe"
$Apksigner = Join-Path $Bt "apksigner.bat"
$Javac = Join-Path $Jbr "javac.exe"
$Keytool = Join-Path $Jbr "keytool.exe"

$Keystore = Join-Path $Root "release.keystore"
$Alias = "konoha"
$StorePass = "konoha123"

if (Test-Path $Build) { Remove-Item -Recurse -Force $Build }
New-Item -ItemType Directory -Force -Path "$Build\compiled", "$Build\gen", "$Build\classes", "$Build\dex" | Out-Null

Write-Host "[1/6] aapt2 compile resources..."
& $Aapt2 compile --dir "$App\res" -o "$Build\compiled\res.zip"

Write-Host "[2/6] aapt2 link..."
& $Aapt2 link `
    -o "$Build\base.apk" `
    -I $Plat `
    --manifest "$App\AndroidManifest.xml" `
    --java "$Build\gen" `
    --min-sdk-version 24 `
    --target-sdk-version 35 `
    --version-code 1 `
    --version-name "1.0.0" `
    -A "$App\assets" `
    "$Build\compiled\res.zip"

Write-Host "[3/6] javac sources..."
$JavaFiles = Get-ChildItem -Recurse -Filter *.java -Path "$App\src","$Build\gen" | ForEach-Object { $_.FullName }
& $Javac -encoding UTF-8 -source 8 -target 8 -bootclasspath $Plat -classpath $Plat -d "$Build\classes" $JavaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed ($LASTEXITCODE)" }

Write-Host "[4/6] d8 dex..."
$Classes = Get-ChildItem -Recurse -Filter *.class -Path "$Build\classes" | ForEach-Object { $_.FullName }
& $D8 --lib $Plat --min-api 24 --output "$Build\dex" @Classes

Write-Host "[5/6] add dex to apk + zipalign..."
Push-Location "$Build\dex"
& "$Jbr\jar.exe" uf "$Build\base.apk" classes.dex
Pop-Location

& $Zipalign -f -p 4 "$Build\base.apk" "$Build\aligned.apk"

Write-Host "[6/6] sign apk..."
& $Apksigner sign `
    --ks $Keystore `
    --ks-key-alias $Alias `
    --ks-pass "pass:$StorePass" `
    --key-pass "pass:$StorePass" `
    --out "$Root\KonohaTokenOnly.apk" `
    "$Build\aligned.apk"

& $Apksigner verify --print-certs "$Root\KonohaTokenOnly.apk" 2>&1 | Select-Object -First 3 | Write-Host
Write-Host ""
Write-Host "DONE -> $Root\KonohaTokenOnly.apk"
