# quickswitch-assist 便捷构建/运行脚本（Windows）
#
#   用法：
#     .\dev.ps1 build        构建【用户版】jar（默认关 HUD、无诊断日志）
#     .\dev.ps1 dev          构建【开发版】jar（HUD 默认开、输出全部诊断日志）
#     .\dev.ps1 runClient    启动带模组的客户端（自动用开发版，否则看不到排查信息）
#     .\dev.ps1 clean        清理
#
# 两个版本的源码是同一份，只差编译期常量 Edition.DEV 与产物名。
#
# 26.2 起 Minecraft 跑在 Java 25 上（1.21.11 是 21），所以这里强制要求 JDK 25+；
# 用 JDK 21 会因为 --release 25 直接编译失败。
#
# 非 Windows 平台直接： ./gradlew build （或 build -Pedition=dev）

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Task = @("build")
)

$ErrorActionPreference = "Stop"

# 不管从哪个目录调用，都切到脚本所在的工程根目录 —— Gradle 是按当前目录找 settings.gradle 的。
Set-Location -LiteralPath $PSScriptRoot

# 「dev」是便捷别名，等价于 build -Pedition=dev
if ($Task.Length -ge 1 -and $Task[0] -eq "dev") {
    $Task = @("build", "-Pedition=dev")
}

# runClient 一律走开发版：不带诊断信息的客户端测不出东西
if ($Task -contains "runClient" -and $Task -notcontains "-Pedition=dev") {
    $Task += "-Pedition=dev"
}

# 判断一个 JDK 是不是 >= 25（读 JDK 自带的 release 文件，避免为探测再起一个 JVM）
function Test-Jdk25 {
    param([string]$Root)
    $release = Join-Path $Root "release"
    if (-not (Test-Path $release)) { return $false }
    $line = Select-String -Path $release -Pattern '^JAVA_VERSION="?(\d+)' | Select-Object -First 1
    if (-not $line) { return $false }
    return ([int]$line.Matches[0].Groups[1].Value -ge 25)
}

# 找一个可用的 JDK：JAVA_HOME → PATH 上的 java → 常见安装目录里扫一遍（不写死任何个人路径）。
$jdk = $null

if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe")) -and (Test-Jdk25 $env:JAVA_HOME)) {
    $jdk = $env:JAVA_HOME
}

if (-not $jdk) {
    $onPath = Get-Command java -ErrorAction SilentlyContinue
    if ($onPath) {
        # 从 ...\bin\java.exe 反推出 JDK 根目录
        $root = Split-Path (Split-Path $onPath.Source -Parent) -Parent
        if (Test-Jdk25 $root) { $jdk = $root }
    }
}

if (-not $jdk) {
    $jdkRoots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Zulu",
        "C:\Program Files\BellSoft",
        (Join-Path $env:LOCALAPPDATA "Programs\Eclipse Adoptium")
    )
    foreach ($r in $jdkRoots) {
        if (-not (Test-Path $r)) { continue }
        $cands = Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^jdk-?\d' } |
            Sort-Object Name -Descending
        foreach ($c in $cands) {
            if (Test-Jdk25 $c.FullName) { $jdk = $c.FullName; break }
        }
        if ($jdk) { break }
    }
}

if (-not $jdk) {
    throw "找不到 JDK 25 或更高版本。26.2 需要 Java 25，请安装后设置 JAVA_HOME 环境变量再运行。"
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
# 这里刻意不设代理。需要代理请通过 ~/.gradle/gradle.properties 或 GRADLE_OPTS 自行配置，
# 不要写死在脚本里 —— 否则代理没开时整个构建会连带失败。
$env:JAVA_OPTS = "-Djava.net.preferIPv4Stack=true"
Write-Host "[quickswitch-assist] JAVA_HOME = $jdk" -ForegroundColor Cyan
Write-Host "[quickswitch-assist] gradle $($Task -join ' ')" -ForegroundColor Cyan

# 找 Gradle：PATH → GRADLE_HOME → 本机 wrapper 缓存里已解压的发行版 → 最后才用 gradlew 联网下载。
# 这样即便暂时连不上下载站，也能用缓存里的现成发行版构建。
function Resolve-Gradle {
    $onPath = Get-Command gradle -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    $roots = @()
    if ($env:GRADLE_HOME) { $roots += $env:GRADLE_HOME }
    $distRoot = Join-Path $env:USERPROFILE ".gradle\wrapper\dists"
    if (Test-Path $distRoot) {
        $roots += Get-ChildItem $distRoot -Directory -ErrorAction SilentlyContinue |
            ForEach-Object { Get-ChildItem $_.FullName -Directory -ErrorAction SilentlyContinue } |
            ForEach-Object { Get-ChildItem $_.FullName -Directory -Filter "gradle-*" -ErrorAction SilentlyContinue } |
            Where-Object { $_.Name -match '^gradle-\d' } |
            Sort-Object Name -Descending |
            Select-Object -ExpandProperty FullName
    }
    foreach ($r in $roots) {
        $bat = Join-Path $r "bin\gradle.bat"
        if (Test-Path $bat) { return $bat }
    }
    return $null
}

$gradle = Resolve-Gradle
if ($gradle) {
    Write-Host "[quickswitch-assist] 用本机 Gradle: $gradle" -ForegroundColor Cyan
    & $gradle --console=plain @Task
} else {
    Write-Host "[quickswitch-assist] 本机没有现成 Gradle，改用 gradlew（首次会联网下载发行版）" -ForegroundColor Yellow
    & (Join-Path $PSScriptRoot "gradlew.bat") @Task
}
exit $LASTEXITCODE
