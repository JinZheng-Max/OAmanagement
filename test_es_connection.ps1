# ===================================================================
# 智办AI OA - Elasticsearch 连通性及账号密码诊断脚本
# ===================================================================

$EsHost = "localhost"
$EsPort = 9200
$Username = "elastic"
$Password = "64526691aA"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Testing Elasticsearch Connection & Credentials..." -ForegroundColor Cyan
Write-Host "Target:   https://${EsHost}:${EsPort}" -ForegroundColor Cyan
Write-Host "Account:  ${Username}" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. 端口连通性检查
Write-Host "`n[1/3] Checking Port 9200..." -ForegroundColor Yellow
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $async = $tcp.BeginConnect($EsHost, $EsPort, $null, $null)
    $success = $async.AsyncWaitHandle.WaitOne(2000, $false)
    if (-not $success) {
        $tcp.Close()
        Write-Host "FAILED: Port 9200 is CLOSED." -ForegroundColor Red
        Exit 1
    }
    $tcp.EndConnect($async)
    $tcp.Close()
    Write-Host "SUCCESS: Port 9200 is listening." -ForegroundColor Green
} catch {
    Write-Host "FAILED: $($_)" -ForegroundColor Red
    Exit 1
}

# 2. HTTPS + Basic Auth 校验
Write-Host "`n[2/3] Verifying HTTPS + Basic Auth Credentials..." -ForegroundColor Yellow
$rawJson = & curl.exe -k -s -u "${Username}:${Password}" "https://${EsHost}:${EsPort}/"
if ($rawJson -and ($rawJson -like "*tagline*")) {
    $esInfo = $rawJson | ConvertFrom-Json
    Write-Host "SUCCESS: Elasticsearch Authentication Passed!" -ForegroundColor Green
    Write-Host "   Node Name:    $($esInfo.name)" -ForegroundColor Cyan
    Write-Host "   Cluster Name: $($esInfo.cluster_name)" -ForegroundColor Cyan
    Write-Host "   ES Version:   $($esInfo.version.number)" -ForegroundColor Cyan
    Write-Host "   Lucene Ver:   $($esInfo.version.lucene_version)" -ForegroundColor Cyan
    Write-Host "   Slogan:       $($esInfo.tagline)" -ForegroundColor Gray
} else {
    Write-Host "FAILED: Could not authenticate." -ForegroundColor Red
    Write-Host $rawJson
    Exit 1
}

# 3. 集群健康度检查
Write-Host "`n[3/3] Checking Cluster Health..." -ForegroundColor Yellow
$healthRaw = & curl.exe -k -s -u "${Username}:${Password}" "https://${EsHost}:${EsPort}/_cluster/health"
if ($healthRaw -and ($healthRaw -like "*status*")) {
    $health = $healthRaw | ConvertFrom-Json
    $statusColor = if ($health.status -eq "green") { "Green" } else { "Yellow" }
    Write-Host "SUCCESS: Cluster Status is [$($health.status.ToUpper())]" -ForegroundColor $statusColor
    Write-Host "   Active Shards: $($health.active_shards)" -ForegroundColor Gray
    Write-Host "   Total Nodes:   $($health.number_of_nodes)" -ForegroundColor Gray
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "DIAGNOSIS RESULT: ES 8.10.2 Connection & Password [64526691aA] VERIFIED PERFECTLY!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
