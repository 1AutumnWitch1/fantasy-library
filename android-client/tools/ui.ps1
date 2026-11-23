# 模拟器 UI 自动化小工具：按文字点击 / 截图。
# 仅用于本机联调与验收取证，不参与 App 编译。
#   . D:\project\fantasy-library\android-client\tools\ui.ps1
#   Tap-Label "动漫"
#   Shot "03-anime"

$script:Adb = "D:\soft\learn\platform-tools\adb.exe"
$script:ShotDir = Join-Path (Split-Path $PSScriptRoot -Parent) "screenshots"
$script:UiDump = Join-Path $env:TEMP "fantasy_ui.xml"

function Get-UiText {
    & $script:Adb shell uiautomator dump /sdcard/fantasy_ui.xml 2>&1 | Out-Null
    & $script:Adb pull /sdcard/fantasy_ui.xml $script:UiDump 2>&1 | Out-Null
    if (-not (Test-Path $script:UiDump)) { return @() }
    $xml = [System.IO.File]::ReadAllText($script:UiDump, [System.Text.Encoding]::UTF8)
    return [regex]::Matches($xml, 'text="([^"]*)"[^>]*bounds="\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]"') |
        ForEach-Object {
            [pscustomobject]@{
                Text   = $_.Groups[1].Value
                X      = [int](([int]$_.Groups[2].Value + [int]$_.Groups[4].Value) / 2)
                Y      = [int](([int]$_.Groups[3].Value + [int]$_.Groups[5].Value) / 2)
                Left   = [int]$_.Groups[2].Value
                Top    = [int]$_.Groups[3].Value
            }
        } | Where-Object { $_.Text -ne "" }
}

function Tap-Label {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [int]$Index = 0,
        [switch]$Exact
    )
    $nodes = Get-UiText | Where-Object {
        if ($Exact) { $_.Text -eq $Label } else { $_.Text -like "*$Label*" }
    }
    $node = @($nodes)[$Index]
    if (-not $node) {
        Write-Host "NOT FOUND: $Label"
        return $false
    }
    & $script:Adb shell input tap $node.X $node.Y | Out-Null
    Write-Host ("tap '{0}' -> ({1},{2})" -f $node.Text, $node.X, $node.Y)
    Start-Sleep -Milliseconds 700
    return $true
}

function Shot {
    param([Parameter(Mandatory = $true)][string]$Name)
    if (-not (Test-Path $script:ShotDir)) { New-Item -ItemType Directory -Force -Path $script:ShotDir | Out-Null }
    $remote = "/sdcard/fantasy_shot.png"
    $local = Join-Path $script:ShotDir "$Name.png"
    & $script:Adb shell screencap -p $remote | Out-Null
    & $script:Adb pull $remote $local 2>&1 | Out-Null
    Write-Host "screenshot -> $local"
}

function Show-Ui {
    Get-UiText | Select-Object -First 40 | Format-Table Text, X, Y -AutoSize
}
