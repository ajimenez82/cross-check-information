param([string]$Model = 'gpt-5.4-mini')
$ErrorActionPreference = 'Stop'
$serviceDirectory = Split-Path -Parent $PSScriptRoot
$resources = Join-Path $serviceDirectory 'infrastructure/src/main/resources/openai/unified'
$definition = [ordered]@{
    model = $Model
    service_tier = 'default'
    reasoning = @{ effort = 'low' }
    instructions = [IO.File]::ReadAllText((Join-Path $resources 'instructions.md'))
    tools = @(@{ type = 'web_search'; mode = 'live' })
    multi_agent = @{ enabled = $false }
    text = @{ format = @{ type = 'json_schema'; schema = (Get-Content (Join-Path $resources 'response.schema.json') -Raw -Encoding UTF8 | ConvertFrom-Json) } }
}
$outputDirectory = Join-Path $serviceDirectory 'bootstrap/target'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$outputPath = Join-Path $outputDirectory 'political-agent-unified.request.json'
[IO.File]::WriteAllText($outputPath, ($definition | ConvertTo-Json -Depth 90), [Text.UTF8Encoding]::new($false))
Write-Output "Prepared $outputPath. No credentials read and no network request sent."
