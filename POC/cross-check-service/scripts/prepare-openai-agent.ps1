param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Model,
    [ValidateSet(2, 3)]
    [int]$SchemaVersion = 3
)

$ErrorActionPreference = 'Stop'
$serviceDirectory = Split-Path -Parent $PSScriptRoot
$definitionPath = Join-Path $serviceDirectory 'docs/openai/political-agent.template.json'
$instructionsPath = Join-Path $serviceDirectory 'docs/openai/political-analysis-instructions.md'
$outputDirectory = Join-Path $serviceDirectory 'bootstrap/target'
$outputPath = Join-Path $outputDirectory 'political-agent.request.json'

if ($SchemaVersion -eq 3) {
    $definitionPath = Join-Path $serviceDirectory 'docs/openai/political-agent-v3.template.json'
    $instructionsPath = Join-Path $serviceDirectory 'docs/openai/political-analysis-instructions-v3.md'
    $outputPath = Join-Path $outputDirectory 'political-agent-v3.request.json'
}

# Prepare a reviewable request only. No API key is read and no network request is sent.
$definition = Get-Content -LiteralPath $definitionPath -Raw -Encoding UTF8 | ConvertFrom-Json
$definition.model = $Model
$definition.instructions = [IO.File]::ReadAllText($instructionsPath)
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
[IO.File]::WriteAllText($outputPath, ($definition | ConvertTo-Json -Depth 50))
Write-Output "Agent request prepared at $outputPath. No remote agent was created."
