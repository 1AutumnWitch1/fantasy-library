[CmdletBinding()]
param(
    [string] $Mc = 'D:\minio\mc.exe',
    [string] $AdminUser = 'libraryadmin'
)
$ErrorActionPreference = 'Stop'
$EnvFile = Join-Path (Split-Path -Parent $PSScriptRoot) '.env'
$Endpoint = 'http://127.0.0.1:9000'
$Bucket = 'fantasy-library-media'
if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) { throw 'Backend .env is missing.' }
if (-not (Test-Path -LiteralPath $Mc -PathType Leaf)) { throw 'The MinIO client mc.exe is missing.' }

$original = [IO.File]::ReadAllText($EnvFile)
function Read-Property([string] $Name) {
    $found = [regex]::Matches($original, ('(?m)^\s*' + [regex]::Escape($Name) + '[^\S\r\n]*=[^\S\r\n]*([^\r\n]+)'))
    if ($found.Count -gt 0) { return $found[$found.Count - 1].Groups[1].Value.Trim() }
    return ''
}
function New-LocalAlias([string] $User, [string] $Password) {
    return ('http://{0}:{1}@127.0.0.1:9000' -f [uri]::EscapeDataString($User), [uri]::EscapeDataString($Password))
}
function Invoke-Mc([string[]] $Arguments) {
    $null = & $Mc @Arguments 2>&1
    return $LASTEXITCODE
}

$existingUser = Read-Property 'MINIO_ACCESS_KEY'
$existingSecret = Read-Property 'MINIO_SECRET_KEY'
if ($existingUser -and $existingSecret) {
    try {
        $env:MC_HOST_libraryprobe = New-LocalAlias $existingUser $existingSecret
        if ((Invoke-Mc @('ls','libraryprobe/fantasy-library-media')) -eq 0) {
            Write-Host '项目已有可用的 MinIO 访问密钥。'
            return
        }
    } finally { Remove-Item Env:MC_HOST_libraryprobe -ErrorAction SilentlyContinue }
    throw '项目已存在访问密钥，但无法访问本机媒体桶，已有设置已保留。'
}

function Get-BucketErrorCode($Records) {
    foreach ($record in $Records) {
        $json = $record.ToString() | ConvertFrom-Json -ErrorAction SilentlyContinue
        $code = $json.error.cause.error.Code
        if ($code -match '^[A-Za-z][A-Za-z0-9]+$') { return $code }
    }
    return ''
}

function Invoke-McRoot([string[]] $Arguments) {
    $null = & $Mc --config-dir $aliasDir @Arguments 2>&1
    return $LASTEXITCODE
}

try { $Host.UI.RawUI.WindowTitle = '幻想图书馆 - MinIO 授权' } catch {}
Write-Host '授权幻想图书馆读写自己的私有媒体桶。'
Write-Host ('请输入 MinIO 管理账号 ' + $AdminUser + ' 的密码，输入时不会显示。')
$aliasDir = Join-Path ([IO.Path]::GetTempPath()) ('fantasy-minio-auth-' + [guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($aliasDir) | Out-Null
$policyFile = $null
$accessKey = 'flmedia-' + [guid]::NewGuid().ToString('N').Substring(0,12)
$secretKey = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(20)).ToLowerInvariant()
$created = $false
$saved = $false
$utf8 = [Text.UTF8Encoding]::new($false)

try {
    $authenticated = $false
    foreach ($attempt in 1..3) {
        $secure = Read-Host 'MinIO 启动时设置的密码' -AsSecureString
        $password = ([Net.NetworkCredential]::new('', $secure)).Password
        $secure.Dispose()
        try {
            $response = & $Mc --config-dir $aliasDir --json alias set librarysetup $Endpoint $AdminUser $password 2>&1
            $code = $LASTEXITCODE
        } finally { $password = $null }
        if ($code -eq 0) {
            $response = & $Mc --config-dir $aliasDir --json ls 'librarysetup/fantasy-library-media' 2>&1
            $code = $LASTEXITCODE
        }
        if ($code -eq 0) {
            $authenticated = $true
            break
        }
        $code = Get-BucketErrorCode $response
        if ($code -eq 'SignatureDoesNotMatch') {
            Write-Warning "密码校验未通过（第 $attempt 次），请确认大小写和键盘输入。"
            continue
        }
        if ($code -eq 'InvalidAccessKeyId') { throw 'MinIO 未识别当前账号名，请确认启动时设置的 MINIO_ROOT_USER。' }
        if ($code -eq 'NoSuchBucket') { throw '找不到 fantasy-library-media 存储桶。' }
        if ($code -eq 'AccessDenied') { throw 'MinIO 拒绝访问媒体桶，请检查账号权限或许可证状态。' }
        throw '无法连接 MinIO 媒体桶。请确认 MinIO 还在运行，并保留这个窗口中的提示。'
    }
    if (-not $authenticated) { throw '连续三次密码校验失败。' }
    $policyFile = [IO.Path]::GetTempFileName()
    $policy = @{
        Version = '2012-10-17'
        Statement = @(
            @{
                Effect = 'Allow'
                Action = @('s3:GetBucketLocation','s3:ListBucket','s3:ListBucketMultipartUploads')
                Resource = @("arn:aws:s3:::$Bucket")
            },
            @{
                Effect = 'Allow'
                Action = @('s3:GetObject','s3:PutObject','s3:DeleteObject','s3:AbortMultipartUpload','s3:ListMultipartUploadParts')
                Resource = @("arn:aws:s3:::$Bucket/*")
            }
        )
    }
    [IO.File]::WriteAllText($policyFile,($policy | ConvertTo-Json -Depth 7),$utf8)
    $result = & $Mc --config-dir $aliasDir --json admin accesskey create librarysetup/ --access-key $accessKey --secret-key $secretKey --name 'Fantasy Library media' --policy $policyFile 2>&1
    if ($LASTEXITCODE -ne 0) {
        $createCode = Get-BucketErrorCode $result
        if ($createCode) { throw "MinIO 无法创建项目访问密钥（$createCode）。" }
        $jsonError = $result | ForEach-Object { $_.ToString() | ConvertFrom-Json -ErrorAction SilentlyContinue } | Select-Object -First 1
        $createMessage = [string]$jsonError.error.message
        if ($createMessage -match '(?i)policy|secret key length|access key length|subnet|license|required|not supported') {
            $safeDetail = $createMessage.Replace($accessKey, '[redacted]').Replace($secretKey, '[redacted]')
            throw "MinIO 无法创建项目访问密钥。$safeDetail"
        }
        throw 'MinIO 无法创建项目访问密钥。'
    }
    $created = $true
    $env:MC_HOST_libraryprobe = New-LocalAlias $accessKey $secretKey
    if ((Invoke-Mc @('ls','libraryprobe/fantasy-library-media')) -ne 0) {
        throw '新密钥无法读取媒体桶。'
    }

    $settings = [ordered]@{
        MEDIA_STORAGE_ENABLED = 'false'
        MINIO_ENDPOINT = $Endpoint
        MINIO_BUCKET = $Bucket
        MINIO_ACCESS_KEY = $accessKey
        MINIO_SECRET_KEY = $secretKey
    }
    $revised = $original
    foreach ($key in $settings.Keys) {
        $line = "$key=$($settings[$key])"
        $pattern = '(?m)^[^\S\r\n]*' + [regex]::Escape($key) + '[^\S\r\n]*=[^\r\n]*'
        if ($revised -match $pattern) { $revised = [regex]::Replace($revised,$pattern,$line) }
        else { $revised = $revised.TrimEnd([char]13,[char]10) + [Environment]::NewLine + $line + [Environment]::NewLine }
    }
    $temporary = $EnvFile + '.minio-' + [guid]::NewGuid().ToString('N') + '.tmp'
    try {
        [IO.File]::WriteAllText($temporary,$revised,$utf8)
        [IO.File]::Move($temporary,$EnvFile,$true)
    } finally {
        if (Test-Path -LiteralPath $temporary -PathType Leaf) { Remove-Item -LiteralPath $temporary -Force }
    }
    $saved = $true
    Write-Host '授权完成，已保存项目专用密钥。现在可以返回对话。'
} finally {
    if ($created -and -not $saved) { $null = Invoke-McRoot @('admin','accesskey','rm','librarysetup/',$accessKey) }
    if (Test-Path -LiteralPath $aliasDir -PathType Container) {
        $candidate = (Resolve-Path -LiteralPath $aliasDir).Path
        $tempRoot = (Resolve-Path -LiteralPath ([IO.Path]::GetTempPath())).Path.TrimEnd([IO.Path]::DirectorySeparatorChar)
        if ((Split-Path -Parent $candidate) -eq $tempRoot -and (Split-Path -Leaf $candidate) -like 'fantasy-minio-auth-*') {
            Remove-Item -LiteralPath $candidate -Force -Recurse
        }
    }
    Remove-Item Env:MC_HOST_libraryprobe -ErrorAction SilentlyContinue
    if ($policyFile -and (Test-Path -LiteralPath $policyFile -PathType Leaf)) { Remove-Item -LiteralPath $policyFile -Force }
}
