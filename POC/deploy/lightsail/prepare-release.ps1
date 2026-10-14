param([Parameter(Mandatory=$true)][ValidatePattern('^[a-z0-9][a-z0-9._-]{0,63}$')][string]$ReleaseTag)
$ErrorActionPreference = 'Stop'
$pocDirectory = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$outputDirectory = Join-Path $PSScriptRoot "artifacts/$ReleaseTag"
if (Test-Path -LiteralPath $outputDirectory) { throw 'Release directory already exists; use a new tag.' }
New-Item -ItemType Directory -Path $outputDirectory | Out-Null
Push-Location $pocDirectory
try {
    & podman build --platform linux/amd64 -f cross-check-service/Containerfile -t "localhost/cross-check-api:$ReleaseTag" cross-check-service
    if ($LASTEXITCODE -ne 0) { throw 'API build failed.' }
    & podman build --platform linux/amd64 -f webapp/Containerfile -t "localhost/cross-check-web:$ReleaseTag" webapp
    if ($LASTEXITCODE -ne 0) { throw 'Web build failed.' }
    foreach ($component in 'api','web') {
        $archive = Join-Path $outputDirectory "$component.tar"
        & podman save --format docker-archive --output $archive "localhost/cross-check-$component`:$ReleaseTag"
        if ($LASTEXITCODE -ne 0) { throw "Image export failed: $component" }
    }
    foreach ($file in 'compose.yaml','Caddyfile','nginx.conf','.env.example','crosscheck-beta.service','crosscheck-backup.service','crosscheck-backup.timer','install-runtime.sh','backup-h2.sh') {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination $outputDirectory
    }
    $manifest = Get-ChildItem -LiteralPath $outputDirectory -File | Where-Object Name -ne 'SHA256SUMS' | ForEach-Object {
        $digest = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        "$digest  $($_.Name)"
    }
    [IO.File]::WriteAllLines((Join-Path $outputDirectory 'SHA256SUMS'), $manifest, [Text.UTF8Encoding]::new($false))
    Write-Output "Release prepared at $outputDirectory. No credentials included. Set RELEASE_TAG=$ReleaseTag on the server."
} finally { Pop-Location }
