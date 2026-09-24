$ErrorActionPreference = 'Stop'

$serviceDirectory = Split-Path -Parent $PSScriptRoot
$configDirectory = Join-Path $serviceDirectory 'config'
$configPath = Join-Path $configDirectory 'application-local.yml'
if (Test-Path -LiteralPath $configPath) {
    Write-Output 'Existing local configuration retained.'
    return
}

# Preserve the environment key when available; otherwise generate a local key.
$secret = $env:CONVERSATION_TOKEN_SECRET
if ([string]::IsNullOrEmpty($secret)) {
    $keyBytes = New-Object byte[] 32
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($keyBytes)
        $secret = [Convert]::ToBase64String($keyBytes)
    } finally {
        $generator.Dispose()
        [Array]::Clear($keyBytes, 0, $keyBytes.Length)
    }
}

try {
    $decoded = [Convert]::FromBase64String($secret)
    if ($decoded.Length -ne 32) { throw 'Invalid key length.' }
} catch {
    throw 'Conversation token secret must be Base64 encoding 32 bytes.'
} finally {
    if ($null -ne $decoded) { [Array]::Clear($decoded, 0, $decoded.Length) }
}

New-Item -ItemType Directory -Path $configDirectory -Force | Out-Null
$content = "# Local secrets. Never commit or share this file.`nCONVERSATION_TOKEN_SECRET: '$secret'`n# Required only when the openai profile is explicitly enabled.`nOPENAI_API_KEY: ''`nOPENAI_POLITICAL_ANALYSIS_AGENT_ID: ''`n"
[IO.File]::WriteAllText($configPath, $content)
$secret = $null
Write-Output 'Local configuration created. Activate dev,local from the service directory.'
