# 解压脚本 - 避免 SIGKILL 的分段解压
Add-Type -AssemblyName System.IO.Compression.FileSystem

$SDK = "D:\去水印\tools\android-sdk"
$JDK_DIR = "D:\去水印\tools\jdk-17.0.2"
$JDK_TARGET = "D:\去水印\tools\jdk17"
$zip = $null

Write-Host "=== 继续解压 JDK ==="
try {
    $zip = [System.IO.Compression.ZipFile]::OpenRead("D:\去水印\tools\openjdk17.zip")
    $total = $zip.Entries.Count
    $cnt = 0
    $extracted = 0
    foreach ($entry in $zip.Entries) {
        $cnt++
        if ($cnt % 50 -eq 0) {
            Write-Host "  [$cnt/$total] $([math]::Round($extracted/1MB,0))MB"
        }
        # 跳过已存在的文件
        $outPath = Join-Path "D:\去水印\tools\jdk-17.0.2" $entry.FullName
        if ((Test-Path $outPath) -and (-not $entry.PSIsDirectory)) {
            continue
        }
        try {
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $outPath, $true)
            $extracted += $entry.Length
        } catch {
            # 目录条目忽略
        }
    }
    $zip.Dispose()
    Write-Host "  JDK 解压完成: $([math]::Round($extracted/1MB,0))MB"
} catch {
    if ($zip) { $zip.Dispose() }
    Write-Host "  JDK 解压出错: $_"
    exit 1
}

Write-Host ""
Write-Host "=== 解压 Android cmdlinetools ==="
$zip2 = [System.IO.Compression.ZipFile]::OpenRead("D:\去水印\tools\cmdtools.zip")
$total2 = $zip2.Entries.Count
$cnt2 = 0
foreach ($entry in $zip2.Entries) {
    $cnt2++
    $outPath = Join-Path "$SDK\cmdline-tools\latest" $entry.FullName
    if (-not (Test-Path $outPath)) {
        try {
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $outPath, $true)
        } catch { }
    }
    if ($cnt2 % 20 -eq 0) {
        Write-Host "  [$cnt2/$total2]"
    }
}
$zip2.Dispose()
Write-Host "  cmdtools 解压完成"

Write-Host ""
Write-Host "=== JDK bin 测试 ==="
try {
    $javaExe = "D:\去水印\tools\jdk-17.0.2\bin\java.exe"
    $javacExe = "D:\去水印\tools\jdk-17.0.2\bin\javac.exe"
    $version = & $javaExe -version 2>&1
    Write-Host "  java: $($version[0])"
    $javacVersion = & $javacExe -version 2>&1
    Write-Host "  javac: $($javacVersion[0])"
} catch {
    Write-Host "  失败: $_"
}

Write-Host ""
Write-Host "=== 验证 sdkmanager ==="
$env:JAVA_HOME = "D:\去水印\tools\jdk-17.0.2"
$env:ANDROID_HOME = $SDK
$bat = "$SDK\cmdline-tools\latest\bin\sdkmanager.bat"
if (Test-Path $bat) {
    Write-Host "  sdkmanager 存在"
    & cmd /c "set JAVA_HOME=D:\去水印\tools\jdk-17.0.2 && `"$bat`" --version" 2>&1
} else {
    Write-Host "  sdkmanager 未找到: $bat"
    Get-ChildItem "$SDK\cmdline-tools\latest" -Recurse | ForEach-Object { Write-Host "    $_" }
}

Write-Host ""
Write-Host "=== 完成 ==="
