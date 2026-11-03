param([switch]$Run)
$ErrorActionPreference = 'Stop'
$service = Split-Path -Parent $PSScriptRoot
function Save-Json($Path, $Value) {
    [IO.File]::WriteAllText($Path, ($Value | ConvertTo-Json -Depth 50), [Text.UTF8Encoding]::new($false))
}
$cases = @(Get-Content (Join-Path $PSScriptRoot 'fixed-attribution-cases.json') -Raw -Encoding UTF8 | ConvertFrom-Json)
$prompt = [IO.File]::ReadAllText((Join-Path $service 'infrastructure/src/main/resources/openai/unified/instructions.md'))
$start = $prompt.IndexOf("## Publication argument content")
$end = $prompt.IndexOf('Preserve modal and evidential strength', $start)
if ($start -lt 0 -or $end -le $start) { throw 'Attribution rule boundaries not found' }
$instructions = $prompt.Substring($start, $end - $start) + "`nIsolated evaluation: classify only the supplied fixed paraphrases, not the truth of the economic claim. No browsing. Preserve ambiguity rather than invent adoption. Explain in Spanish."
$payload = [ordered]@{
    proposition = 'La subida del SMI de 2019 redujo el empleo agregado frente al escenario sin subida.'
    materialType = 'Fixed paraphrases of reviewed articles, not verbatim originals. Borderline cases require manual review.'
    cases = @($cases | ForEach-Object { [ordered]@{id=$_.id;url=$_.url;text=$_.text} })
}
$inputJson = $payload | ConvertTo-Json -Depth 10 -Compress
$hash = [Security.Cryptography.SHA256]::Create()
try { $fingerprint = ([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($instructions + $inputJson)))).Replace('-','').ToLowerInvariant() } finally { $hash.Dispose() }
$target = Join-Path $service ('bootstrap/target/attribution-eval-powershell/' + $fingerprint.Substring(0,12))
New-Item -ItemType Directory -Force -Path $target | Out-Null
Save-Json (Join-Path $target 'input.json') $payload
Write-Output "Prepared: $target"
if (-not $Run) { Write-Output 'Dry run: no API call or credentials read.'; return }
$config = [IO.File]::ReadAllText((Join-Path $service 'config/application-local.yml'))
function Setting([string]$Name) {
    $match = [regex]::Match($config, ('(?m)^' + [regex]::Escape($Name) + ':\s*(.*?)\s*$'))
    if (-not $match.Success) { throw "Missing setting: $Name" }
    return $match.Groups[1].Value.Trim().Trim([char]34,[char]39)
}
$headers = @{Authorization=('Bearer ' + (Setting 'OPENAI_API_KEY'));'OpenAI-Beta'='agents=v1'}
function Request([string]$Path, $Body = $null) {
    $parameters = @{Uri=('https://api.openai.com/v1/' + $Path);Headers=$headers;TimeoutSec=30;Method='Get'}
    if ($null -ne $Body) {
        $parameters.Method='Post'
        $parameters.ContentType='application/json; charset=utf-8'
        $parameters.Body=[Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 50 -Compress))
    }
    try { Invoke-RestMethod @parameters } catch { throw 'API request failed. No automatic resubmission; inspect saved state before retrying.' }
}
$stateFile = Join-Path $target 'state.json'
$responseFile = Join-Path $target 'response.json'
if (Test-Path $responseFile) {
    $result = Get-Content $responseFile -Raw -Encoding UTF8 | ConvertFrom-Json
} else {
    if (Test-Path $stateFile) {
        $state = Get-Content $stateFile -Raw -Encoding UTF8 | ConvertFrom-Json
        if (-not $state.session) { throw 'Submission uncertain. Do not delete state or resubmit without reconciliation.' }
    } else {
        $agentId = Setting 'OPENAI_POLITICAL_ANALYSIS_AGENT_ID'
        $state = [ordered]@{fingerprint=$fingerprint;model='gpt-5.4';reasoning='medium';revision=(Setting 'AGENT_REVISION');session=$null}
        $fields = @('id','position','stanceOwner','explanation','attributionContext')
        $properties = [ordered]@{}
        foreach ($field in $fields) { $properties[$field]=@{type='string'} }
        $properties.position=@{type='string';enum=@('SUPPORTS','QUESTIONS','MIXED','NO_EXPLICIT_POSITION')}
        $schema = @{type='object';additionalProperties=$false;required=@('assessments');properties=@{assessments=@{type='array';items=@{type='object';additionalProperties=$false;required=$fields;properties=$properties}}}}
        $agent = @{model='gpt-5.4';service_tier='default';reasoning=@{effort='medium'};instructions=$instructions;tools=@();multi_agent=@{enabled=$false};text=@{format=@{type='json_schema';schema=$schema}}}
        Save-Json $stateFile $state
        $session = Request 'agents/sessions' @{agent_id=$agentId;agent=$agent;environment=@{type='none'};input=$inputJson;stream=$false}
        if (-not $session.id) { throw 'No session ID returned; submission uncertain.' }
        $state.session=$session.id
        Save-Json $stateFile $state
    }
    $deadline = [DateTime]::UtcNow.AddMinutes(5)
    $turn = $null
    do {
        $turns = @((Request ("agents/sessions/" + $state.session + '/turns?order=desc&limit=1')).data)
        if ($turns.Count -gt 0) { $turn=$turns[0] }
        if ($turn -and $turn.status -in @('completed','failed','cancelled')) { break }
        Start-Sleep -Seconds 3
    } while ([DateTime]::UtcNow -lt $deadline)
    if (-not $turn -or $turn.status -notin @('completed','failed','cancelled')) { throw 'Still running. Repeat -Run to resume the same session.' }
    Save-Json (Join-Path $target 'turn.json') $turn
    if ($turn.status -ne 'completed') { throw ("Remote state: " + $turn.status) }
    $page = Request ("agents/sessions/" + $state.session + '/items?order=desc&limit=100')
    $messages = @($page.data | Where-Object { $_.turn_id -eq $turn.id -and $_.role -eq 'assistant' -and $_.phase -eq 'final_answer' })
    if ($messages.Count -ne 1) { throw 'Unexpected final message count; inspect saved session.' }
    $raw = (@($messages[0].content | Where-Object {$_.type -eq 'output_text'} | ForEach-Object {$_.text}) -join '')
    $result = $raw | ConvertFrom-Json
    Save-Json $responseFile $result
}
$actualIds = @($result.assessments | ForEach-Object {$_.id} | Sort-Object)
if (($actualIds -join ',') -ne (($cases.id | Sort-Object) -join ',')) { throw 'Invalid case coverage' }
$failed = $false
foreach ($row in $result.assessments) {
    $expected = ($cases | Where-Object {$_.id -eq $row.id}).expected
    $outcome = if ($null -eq $expected) {'REVIEW'} elseif ($expected -eq $row.position) {'PASS'} else {'FAIL'}
    if ($outcome -eq 'FAIL') {$failed=$true}
    Write-Output ("{0} {1}: {2} | {3}" -f $outcome,$row.id,$row.position,$row.explanation)
}
Write-Output "Result: $responseFile"
if ($failed) { throw 'At least one fixed expectation failed.' }
