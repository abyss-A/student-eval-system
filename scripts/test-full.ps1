$ErrorActionPreference = 'Stop'

function Ensure-Java17 {
  $java17 = 'D:\LenovoSoftstore\java-17'
  if (-not (Test-Path $java17)) { return }
  $env:JAVA_HOME = $java17
  if ($env:Path -notlike "*$java17\bin*") {
    $env:Path = "$java17\bin;$env:Path"
  }
}

function Wait-HttpReady {
  param(
    [Parameter(Mandatory = $true)][string]$Url,
    [int]$TimeoutSec = 120
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSec)
  while ((Get-Date) -lt $deadline) {
    try {
      $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -Method Get -TimeoutSec 5
      if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) { return $true }
    } catch {
      $status = $_.Exception.Response.StatusCode.value__
      if ($status -ge 200 -and $status -lt 500) { return $true }
      Start-Sleep -Milliseconds 800
    }
  }
  return $false
}

function Assert-ExitCode {
  param([string]$StepName)
  if ($LASTEXITCODE -ne 0) {
    throw "$StepName failed with exit code $LASTEXITCODE"
  }
}

Ensure-Java17

$mavenCmd = 'mvn'
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
  $fallbackMaven = 'D:\LenovoSoftstore\Install\IDEA\IntelliJ IDEA 2022.3.2\plugins\maven\lib\maven3\bin\mvn.cmd'
  if (-not (Test-Path $fallbackMaven)) {
    throw 'Maven command not found. Install Maven or update scripts/test-full.ps1 fallback path.'
  }
  $mavenCmd = $fallbackMaven
}

$backendProc = $null
$frontendProc = $null
$startedBackend = $false
$startedFrontend = $false

Write-Host '[Full] Backend tests...' -ForegroundColor Cyan
Push-Location backend
& $mavenCmd -q test
Assert-ExitCode -StepName 'Backend full tests'
Pop-Location

Write-Host '[Full] Frontend build check...' -ForegroundColor Cyan
Push-Location frontend
npm run build
Assert-ExitCode -StepName 'Frontend build'
Pop-Location

try {
  $backendReady = Wait-HttpReady -Url 'http://localhost:8080/api/v1/semesters/active' -TimeoutSec 3
  if (-not $backendReady) {
    Write-Host '[Full] Starting backend server...' -ForegroundColor Cyan
    $backendCmd = "Set-Location '$PWD\backend'; .\scripts\run-backend.ps1"
    $backendProc = Start-Process -FilePath powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $backendCmd -PassThru
    $startedBackend = $true
  } else {
    Write-Host '[Full] Reusing existing backend on :8080' -ForegroundColor DarkCyan
  }

  $frontendReady = Wait-HttpReady -Url 'http://localhost:5173/login' -TimeoutSec 3
  if (-not $frontendReady) {
    Write-Host '[Full] Starting frontend dev server...' -ForegroundColor Cyan
    $frontendCmd = "Set-Location '$PWD\frontend'; npm run dev"
    $frontendProc = Start-Process -FilePath powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $frontendCmd -PassThru
    $startedFrontend = $true
  } else {
    Write-Host '[Full] Reusing existing frontend on :5173' -ForegroundColor DarkCyan
  }

  if (-not (Wait-HttpReady -Url 'http://localhost:8080/api/v1/semesters/active' -TimeoutSec 420)) {
    throw 'Backend server startup timeout on http://localhost:8080'
  }
  if (-not (Wait-HttpReady -Url 'http://localhost:5173/login' -TimeoutSec 180)) {
    throw 'Frontend server startup timeout on http://localhost:5173'
  }

  Push-Location frontend
  Write-Host '[Full] Playwright full E2E...' -ForegroundColor Cyan
  npm run test:e2e:full
  Assert-ExitCode -StepName 'Playwright full E2E'

  Write-Host '[Full] Playwright visual suite...' -ForegroundColor Cyan
  npm run test:visual
  Assert-ExitCode -StepName 'Playwright visual suite'
  Pop-Location
} finally {
  if ($startedFrontend -and $frontendProc -and -not $frontendProc.HasExited) { Stop-Process -Id $frontendProc.Id -Force }
  if ($startedBackend -and $backendProc -and -not $backendProc.HasExited) { Stop-Process -Id $backendProc.Id -Force }
}

Write-Host '[Full] All checks completed.' -ForegroundColor Green
