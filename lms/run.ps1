# Set JAVA_HOME using the installed OpenJDK 17
$env:JAVA_HOME = "C:\Users\mathi\OneDrive\Documents\Documents - Copy\Documents\Downloads\eclipse-java-2022-09-R-win32-x86_64\eclipse\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.4.v20220903-1038\jre"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

if ($env:DB_PASSWORD) {
    $env:DB_PASSWORD = $env:DB_PASSWORD.Trim()
}

# Kill any process already using port 8080
$portPid = (netstat -ano | Select-String ":8080\s.*LISTENING" | ForEach-Object { ($_ -split "\s+")[-1] } | Select-Object -First 1)
if ($portPid) {
    Write-Host "Killing existing process on port 8080 (PID $portPid)..." -ForegroundColor Yellow
    taskkill /PID $portPid /F | Out-Null
    Start-Sleep -Seconds 1
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Starting LearnHub LMS (Embedded Jetty) " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Using Java: $env:JAVA_HOME" -ForegroundColor Gray
Write-Host "URL: http://localhost:8080/lms" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server.`n" -ForegroundColor Yellow

mvn org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:run
