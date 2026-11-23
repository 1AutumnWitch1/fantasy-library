[CmdletBinding()]
param(
    [switch] $Apply,
    [string] $Java = 'java',
    [string] $JdbcJar,
    [string] $Mc = 'D:\minio\mc.exe'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$backend = Join-Path $root 'backend'
if (-not $JdbcJar) {
    $maven = Join-Path $env:USERPROFILE '.m2/repository/com/oracle/database/jdbc/ojdbc11'
    $JdbcJar = (Get-ChildItem -LiteralPath $maven -Filter 'ojdbc11-*.jar' -File -Recurse |
        Where-Object { $_.Name -match '^ojdbc11-[\d.]+\.jar$' } | Sort-Object LastWriteTime -Descending |
        Select-Object -First 1).FullName
}
if (-not $JdbcJar -or -not (Test-Path -LiteralPath $JdbcJar -PathType Leaf)) { throw 'Oracle JDBC driver not found.' }
$inventory = Join-Path $PSScriptRoot 'MediaMigrationInventory.java'
$records = & $Java '-Dstdout.encoding=UTF-8' --class-path $JdbcJar $inventory $root
if ($LASTEXITCODE -ne 0) { throw 'Unable to verify the media references against Oracle.' }
$tab = [char]9
$entries = foreach ($item in $records) {
    $row = $item.Split($tab,2)
    if ($row.Count -ne 2) { throw 'Unexpected media inventory output.' }
    if ($row[0] -eq 'EXTERNAL') {
        Write-Host "External URLs left in Oracle: $($row[1])"
    } else {
        [PSCustomObject]@{Key=$row[0]; Source=$row[1]}
    }
}
$entries | Group-Object { $_.Key.Split('/')[0] } | ForEach-Object { Write-Host ("{0}: {1}" -f $_.Name,$_.Count) }
Write-Host "Objects in media inventory: $(@($entries).Count)"
if (-not $Apply) { return }

if (-not (Test-Path -LiteralPath $Mc -PathType Leaf)) { throw 'MinIO client not found.' }
$localSettings = [IO.File]::ReadAllText((Join-Path $backend '.env'))
function Read-LocalSetting([string] $Name) {
    $processValue = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($processValue)) { return $processValue }
    $found = [regex]::Matches($localSettings, ('(?m)^\s*' + [regex]::Escape($Name) + '[^\S\r\n]*=[^\S\r\n]*([^\r\n]+)'))
    if ($found.Count -gt 0) { return $found[$found.Count - 1].Groups[1].Value.Trim() }
    return ''
}
$endpoint = Read-LocalSetting MINIO_ENDPOINT
$bucket = Read-LocalSetting MINIO_BUCKET
$access = Read-LocalSetting MINIO_ACCESS_KEY
$secret = Read-LocalSetting MINIO_SECRET_KEY
if ($endpoint -ne 'http://127.0.0.1:9000' -or $bucket -ne 'fantasy-library-media' -or -not $access -or -not $secret) {
    throw 'Configure dedicated credentials for the local fantasy-library-media bucket before migration.'
}
$env:MC_HOST_librarymigration = 'http://{0}:{1}@127.0.0.1:9000' -f [uri]::EscapeDataString($access),[uri]::EscapeDataString($secret)
$target = "librarymigration/$bucket"
try {
    $remoteFiles = & $Mc ls --recursive --json $target 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'The media bucket could not be listed.' }
    $remote = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($line in $remoteFiles) {
        if (-not $line) { continue }
        $item = $line | ConvertFrom-Json
        if ($item.status -ne 'success') { throw 'Unable to list media bucket objects.' }
        if ($item.type -eq 'file') { [void]$remote.Add([string]$item.key) }
    }

    foreach ($entry in $entries) {
        if (-not $entry.Source -and -not $remote.Contains($entry.Key)) {
            throw ('An Oracle reference is missing both locally and in MinIO: ' + $entry.Key)
        }
    }
    $created = 0
    $verified = 0
    $remoteOnly = 0
    $scratch = [IO.Path]::GetTempFileName()
    try {
        foreach ($entry in $entries) {
            if (-not $entry.Source) { $remoteOnly++; continue }
            $object = "$target/$($entry.Key)"
            if (-not $remote.Contains($entry.Key)) {
                $null = & $Mc --custom-header "If-None-Match:*" put --json --disable-multipart $entry.Source $object 2>&1
                if ($LASTEXITCODE -ne 0) { throw ('Unable to write media object: ' + $entry.Key) }
                $created++
            }
            $null = & $Mc cp --json $object $scratch 2>&1
            if ($LASTEXITCODE -ne 0) { throw ('Unable to read back media object: ' + $entry.Key) }
            $storedHash = (Get-FileHash -LiteralPath $scratch -Algorithm SHA256).Hash
            $localHash = (Get-FileHash -LiteralPath $entry.Source -Algorithm SHA256).Hash
            if ($storedHash -ne $localHash) { throw ('MinIO content differs from its local source: ' + $entry.Key) }
            $verified++
        }
    } finally { Remove-Item -LiteralPath $scratch -Force -ErrorAction SilentlyContinue }

    Write-Host "Uploaded $created objects. Verified $verified SHA-256 checksums and $remoteOnly previously stored references."
} finally { Remove-Item Env:MC_HOST_librarymigration -ErrorAction SilentlyContinue }
