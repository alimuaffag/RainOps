param(
    [ValidateSet('start', 'stop', 'status')][string]$Action = 'status',
    [string]$PgBin = 'C:\Program Files\PostgreSQL\17\bin'
)
$ErrorActionPreference = 'Stop'
$rainRoot = Split-Path $PSScriptRoot -Parent
$rainData = Join-Path $rainRoot '.local/pgdata'
if (!(Test-Path "$rainData/PG_VERSION")) { throw 'Local database has not been initialized.' }
$rainCtl = Join-Path $PgBin 'pg_ctl.exe'
switch ($Action) {
    'start' {
        & $rainCtl -D $rainData status *> $null
        if ($LASTEXITCODE -eq 0) { Write-Output 'RainOps database is already running.'; exit 0 }
        & $rainCtl -D $rainData -l "$rainRoot/.local/postgres.log" -o '-h 127.0.0.1 -p 5433' -w start
    }
    'stop' { & $rainCtl -D $rainData -m fast -w stop }
    'status' { & $rainCtl -D $rainData status }
}
exit $LASTEXITCODE
