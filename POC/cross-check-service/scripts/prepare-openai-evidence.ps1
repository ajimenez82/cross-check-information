param(
    [ValidateNotNullOrEmpty()]
    [string]$Model = 'gpt-5.4-mini'
)

$ErrorActionPreference = 'Stop'
$serviceDirectory = Split-Path -Parent $PSScriptRoot
$resourceDirectory = Join-Path $serviceDirectory 'infrastructure/src/main/resources/openai/evidence'
$outputDirectory = Join-Path $serviceDirectory 'bootstrap/target'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

# Prepare both reviewed configurations; this script never reads credentials or calls OpenAI.
foreach ($stageName in @('research', 'synthesis')) {
    $tools = @()
    if ($stageName -eq 'research') { $tools = @(@{ type = 'web_search'; mode = 'live' }) }
    $definition = [ordered]@{
        model = $Model
        service_tier = 'default'
        reasoning = @{ effort = 'low' }
        instructions = [IO.File]::ReadAllText((Join-Path $resourceDirectory "$stageName.md"))
        tools = $tools
        multi_agent = @{ enabled = $false }
        text = @{ format = @{ type = 'json_schema'; schema = (Get-Content -LiteralPath (Join-Path $resourceDirectory "$stageName.schema.json") -Raw -Encoding UTF8 | ConvertFrom-Json) } }
    }
    $outputPath = Join-Path $outputDirectory "political-evidence-$stageName.request.json"
    [IO.File]::WriteAllText($outputPath, ($definition | ConvertTo-Json -Depth 80))
    Write-Output "Prepared $outputPath. No remote change."
}
