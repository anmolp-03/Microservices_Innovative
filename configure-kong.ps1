# Kong API Gateway Configuration Script
# This script sets up routes for all microservices

$KONG_ADMIN_URL = "http://localhost:18083"

Write-Host "Configuring Kong API Gateway..." -ForegroundColor Green

# Wait for Kong to be ready
Write-Host "Waiting for Kong to be ready..." -ForegroundColor Yellow
$retries = 0
$maxRetries = 30
while ($retries -lt $maxRetries) {
    try {
        $response = Invoke-RestMethod -Uri "$KONG_ADMIN_URL/status" -Method Get -ErrorAction Stop
        Write-Host "Kong is ready!" -ForegroundColor Green
        break
    } catch {
        $retries++
        Write-Host "Waiting for Kong... ($retries/$maxRetries)" -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}

if ($retries -eq $maxRetries) {
    Write-Host "Kong failed to start!" -ForegroundColor Red
    exit 1
}

# Function to create or update service
function Set-KongService {
    param($Name, $Url)
    
    Write-Host "Setting up service: $Name" -ForegroundColor Cyan
    
    # Try to get existing service
    try {
        $existing = Invoke-RestMethod -Uri "$KONG_ADMIN_URL/services/$Name" -Method Get -ErrorAction Stop
        # Update existing service
        $body = @{
            url = $Url
        } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$KONG_ADMIN_URL/services/$Name" -Method Patch -Body $body -ContentType "application/json"
        Write-Host "  Updated service: $Name" -ForegroundColor Green
    } catch {
        # Create new service
        $body = @{
            name = $Name
            url = $Url
        } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$KONG_ADMIN_URL/services" -Method Post -Body $body -ContentType "application/json"
        Write-Host "  Created service: $Name" -ForegroundColor Green
    }
}

# Function to create or update route
function Set-KongRoute {
    param($ServiceName, $RouteName, $Paths)
    
    Write-Host "Setting up route: $RouteName for service $ServiceName" -ForegroundColor Cyan
    
    # Try to get existing route
    try {
        $existing = Invoke-RestMethod -Uri "$KONG_ADMIN_URL/routes/$RouteName" -Method Get -ErrorAction Stop
        # Update existing route
        $body = @{
            paths = @($Paths)
            strip_path = $false
        } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$KONG_ADMIN_URL/routes/$RouteName" -Method Patch -Body $body -ContentType "application/json"
        Write-Host "  Updated route: $RouteName" -ForegroundColor Green
    } catch {
        # Create new route
        $body = @{
            name = $RouteName
            paths = @($Paths)
            strip_path = $false
        } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$KONG_ADMIN_URL/services/$ServiceName/routes" -Method Post -Body $body -ContentType "application/json"
        Write-Host "  Created route: $RouteName" -ForegroundColor Green
    }
}

# Configure Menu Service
Set-KongService -Name "menu-service" -Url "http://menu:8080"
Set-KongRoute -ServiceName "menu-service" -RouteName "menu-route" -Paths "/menu"

# Configure Order Service
Set-KongService -Name "order-service" -Url "http://order:8000"
Set-KongRoute -ServiceName "order-service" -RouteName "order-route" -Paths "/orders"

# Configure Bill Service
Set-KongService -Name "bill-service" -Url "http://bill:3000"
Set-KongRoute -ServiceName "bill-service" -RouteName "bill-route" -Paths "/bills"

# Configure Review Service
Set-KongService -Name "review-service" -Url "http://review:4000"
Set-KongRoute -ServiceName "review-service" -RouteName "review-route" -Paths "/reviews"

Write-Host "`nKong configuration complete!" -ForegroundColor Green
Write-Host "Services configured:" -ForegroundColor Cyan
Write-Host "  Menu Service:   http://localhost:18082/menu" -ForegroundColor White
Write-Host "  Order Service:  http://localhost:18082/orders" -ForegroundColor White
Write-Host "  Bill Service:   http://localhost:18082/bills" -ForegroundColor White
Write-Host "  Review Service: http://localhost:18082/reviews" -ForegroundColor White
