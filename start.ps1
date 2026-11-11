$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Test-Path '.env')) { throw 'Copy .env.example to .env and configure your local MySQL first.' }
Get-Content '.env' -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line.Split('=', 2)
        if ($parts.Length -eq 2 -and $parts[0] -match '^[A-Z][A-Z0-9_]*$') {
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
        }
    }
}
$javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
& $javaCommand -jar 'release/xunwu-demo-3.0.0.jar'
exit $LASTEXITCODE
