# 一条命令完成「提交 + 推送」
#
# 用法（在 IDEA 的 Terminal 里，任意目录）：
#   D:\project\fantasy-library\tools\push.ps1 "feat: 新增某某功能"
#
# 或者先 cd 到仓库根目录：
#   cd D:\project\fantasy-library
#   .\tools\push.ps1 "fix: 修复某某问题"
#
# 说明：
#   - 没有改动时会直接提示，不会产生空提交
#   - 推送失败（断网/代理）时会提示重试命令，不会丢提交
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Message
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

Write-Host "[1/3] 暂存改动..." -ForegroundColor Cyan
git add -A

$pending = git status --porcelain
if (-not $pending) {
    Write-Host "没有需要提交的改动。" -ForegroundColor Yellow
    exit 0
}

Write-Host "[2/3] 提交：$Message" -ForegroundColor Cyan
git commit -m $Message
if ($LASTEXITCODE -ne 0) {
    Write-Host "提交失败，已中止。" -ForegroundColor Red
    exit 1
}

Write-Host "[3/3] 推送到 GitHub..." -ForegroundColor Cyan
git push
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "提交已完成，但推送失败（多半是网络/代理）。稍后重试：" -ForegroundColor Red
    Write-Host "    git push" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "完成。最新提交：" -ForegroundColor Green
git log --oneline -1
