$mysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

if (-not (Test-Path $mysqlExe)) {
    Write-Host "MySQL executable not found at: $mysqlExe" -ForegroundColor Red
    exit 1
}

$schema = (Resolve-Path "$PSScriptRoot\sql\01_schema.sql").Path.Replace('\', '/')
$seed   = (Resolve-Path "$PSScriptRoot\sql\02_seed_data.sql").Path.Replace('\', '/')

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Initializing LMS Database in MySQL...  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Please enter your MySQL root password when prompted:" -ForegroundColor Yellow

& $mysqlExe -u root -p -e "source $schema"
if ($LASTEXITCODE -eq 0) {
    Write-Host "[1/2] 01_schema.sql imported successfully!" -ForegroundColor Green
} else {
    Write-Host "Failed to import 01_schema.sql. Check password and try again." -ForegroundColor Red
    exit 1
}

& $mysqlExe -u root -p -e "source $seed"
if ($LASTEXITCODE -eq 0) {
    Write-Host "[2/2] 02_seed_data.sql imported successfully!" -ForegroundColor Green
    Write-Host "Database setup complete! You can now run .\run.ps1" -ForegroundColor Cyan
} else {
    Write-Host "Failed to import 02_seed_data.sql." -ForegroundColor Red
    exit 1
}
