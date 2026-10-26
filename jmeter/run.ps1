param(
    [string]$JMeterHome = 'C:\Users\NB.PRO\Downloads\apache-jmeter-5.6.3',
    [string]$BaseUrl = 'http://localhost:8080',
    [ValidateRange(1, 1000)][int]$Iterations = 10,
    [ValidateRange(5, 300)][int]$TimeoutSeconds = 30,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label = 'run',
    [switch]$RetryConflicts,
    [ValidatePattern('^[a-zA-Z0-9_,-]*$')][string]$CleanupRunIds = '',
    [switch]$Gui
)
$ErrorActionPreference = 'Stop'
$jmeter = Join-Path $JMeterHome 'bin\jmeter.bat'
if (-not (Test-Path -LiteralPath $jmeter)) { throw "JMeter not found: $jmeter" }
if (-not $Gui) {
    $ready = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        try {
            $probe = Invoke-WebRequest -Uri "$($BaseUrl.TrimEnd('/'))/api/auth/me" -UseBasicParsing -TimeoutSec 2
            $ready = [int]$probe.StatusCode -eq 200
        } catch {
            if ($null -ne $_.Exception.Response) {
                $ready = [int]$_.Exception.Response.StatusCode -in @(401, 403)
            }
        }
        if ($ready) { break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "Application not ready: $BaseUrl" }
}
$runId = $Label + '_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '_' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$output = Join-Path $PSScriptRoot "results\$runId"
New-Item -ItemType Directory -Path $output -Force | Out-Null
$arguments = @(
    '-t', (Join-Path $PSScriptRoot 'concurrency.jmx'),
    '-j', (Join-Path $output 'jmeter.log'),
    "-JsuiteDir=$($PSScriptRoot.Replace('\', '/'))",
    "-JbaseUrl=$BaseUrl", "-JrunId=$runId", "-Jiterations=$Iterations",
    "-JtimeoutSeconds=$TimeoutSeconds",
    "-JretryConflicts=$($RetryConflicts.IsPresent.ToString().ToLowerInvariant())",
    "-JcleanupRunIds=$CleanupRunIds",
    "-JevidenceFile=$(Join-Path $output 'evidence.jsonl')"
)
if ($Gui) {
    & $jmeter @arguments
    exit $LASTEXITCODE
}
$arguments += @('-n', '-l', (Join-Path $output 'results.jtl'), '-e', '-o', (Join-Path $output 'report'))
& $jmeter @arguments
if ($LASTEXITCODE -ne 0) { throw "JMeter exited with code $LASTEXITCODE. See $output" }
$samples = @(Import-Csv -LiteralPath (Join-Path $output 'results.jtl'))
$expectedSamples = 2 * 11 * $Iterations
if ($samples.Count -ne $expectedSamples) { throw "Incomplete run: $($samples.Count)/$expectedSamples samples. See $output" }
$failures = @($samples | Where-Object { $_.success -ne 'true' })
Write-Host "Results: $output"
Write-Host "Scenarios: $($samples.Count); failed: $($failures.Count)"
if ($failures.Count -gt 0) {
    $failures | Group-Object label | ForEach-Object {
        [pscustomobject]@{ Scenario = $_.Name; Failed = $_.Count; Example = $_.Group[0].responseMessage.Substring(0, [Math]::Min(300, $_.Group[0].responseMessage.Length)) }
    } | Format-Table -Wrap
    exit 1
}
exit 0
