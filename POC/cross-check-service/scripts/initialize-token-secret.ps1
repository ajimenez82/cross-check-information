$ErrorActionPreference = 'Stop'

if (![string]::IsNullOrEmpty($env:CONVERSATION_TOKEN_SECRET)) {
    Write-Output 'Existing conversation token secret retained.'
    return
}

$keyBytes = New-Object byte[] 32
$generator = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($keyBytes)
    $env:CONVERSATION_TOKEN_SECRET = [Convert]::ToBase64String($keyBytes)
} finally {
    $generator.Dispose()
    [Array]::Clear($keyBytes, 0, $keyBytes.Length)
}
Write-Output 'Conversation token secret initialized for this terminal; no file was written.'
