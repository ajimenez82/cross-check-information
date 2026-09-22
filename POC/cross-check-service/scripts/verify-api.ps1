param(
    [string]$BaseUrl = 'http://127.0.0.1:8080'
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

function Send-AnalysisRequest {
    param([hashtable]$Payload)

    $json = $Payload | ConvertTo-Json -Depth 10
    $requestParameters = @{
        Uri = "$BaseUrl/api/analysis/start"
        Method = 'Post'
        ContentType = 'application/json; charset=utf-8'
        Body = [Text.Encoding]::UTF8.GetBytes($json)
    }
    Invoke-RestMethod @requestParameters
}

$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health"
if ($health.status -ne 'UP') {
    throw 'The service is not healthy.'
}

$first = Send-AnalysisRequest -Payload @{ text = 'Development smoke test'; conversationToken = $null }
if ($first.conversationToken.Split('.').Count -ne 5 -or !$first.analysis.title.Contains('simulado')) {
    throw 'Expected a simulated development response.'
}
if (!$first.analysis.analyzedAt) {
    throw 'The server timestamp is missing.'
}

$followUp = Send-AnalysisRequest -Payload @{
    text = 'Development follow-up'
    conversationToken = $first.conversationToken
}
if (!$followUp.analysis.title.StartsWith('Seguimiento simulado') -or
        $followUp.conversationToken -eq $first.conversationToken) {
    throw 'The simulated conversation did not continue as expected.'
}

Write-Output 'PASS: health, new analysis, server timestamp and simulated follow-up.'
