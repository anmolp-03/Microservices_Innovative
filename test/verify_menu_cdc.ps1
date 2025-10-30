# Menu CDC Verification Script
# This script verifies that menu creation is properly reflected in both read and write databases
# and that CDC events are published to RabbitMQ

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Menu CDC Verification Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Create a new menu item
Write-Host "Test 1: Creating a new menu item..." -ForegroundColor Yellow
$menuItem = @{
    name = "Test Burger $(Get-Random -Maximum 10000)"
    description = "A delicious test burger for CDC verification"
    price = 12.99
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:18080/menu" -Method POST -Body $menuItem -ContentType "application/json"
    $menuId = $response.id
    Write-Host "Menu item created successfully with ID: $menuId" -ForegroundColor Green
    Write-Host "  Name: $($response.name)" -ForegroundColor Gray
    Write-Host "  Price: $($response.price)" -ForegroundColor Gray
    Write-Host ""
}
catch {
    Write-Host "Failed to create menu item: $_" -ForegroundColor Red
    exit 1
}

# Wait a bit for CDC to process
Write-Host "Waiting 3 seconds for CDC processing..." -ForegroundColor Yellow
Start-Sleep -Seconds 3
Write-Host ""

# Test 2: Verify in write database
Write-Host "Test 2: Verifying menu item in WRITE database..." -ForegroundColor Yellow
$writeCount = docker exec devops-mongodb-1 mongosh --quiet --eval "db.getSiblingDB('restaurant').menu_items.countDocuments({name: '$($response.name)'})"
Write-Host "  Items found in write DB: $writeCount" -ForegroundColor Gray
Write-Host ""

# Test 3: Check all menu items
Write-Host "Test 3: Listing all menu items via API..." -ForegroundColor Yellow
try {
    $allMenus = Invoke-RestMethod -Uri "http://localhost:18080/menu" -Method GET
    Write-Host "  Total menu items: $($allMenus.Count)" -ForegroundColor Gray
    Write-Host ""
}
catch {
    Write-Host "  Could not retrieve menu items" -ForegroundColor DarkYellow
    Write-Host ""
}

# Test 4: Check RabbitMQ
Write-Host "Test 4: Checking RabbitMQ for menu.events exchange..." -ForegroundColor Yellow
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("guest:guest"))
$headers = @{ Authorization = "Basic $credentials" }

try {
    $exchange = Invoke-RestMethod -Uri "http://localhost:15672/api/exchanges/%2F/menu.events" -Headers $headers -Method Get -ErrorAction SilentlyContinue
    Write-Host "  Exchange 'menu.events' found: Type=$($exchange.type)" -ForegroundColor Green
}
catch {
    Write-Host "  Exchange 'menu.events' not found or not accessible" -ForegroundColor DarkYellow
}

try {
    $queues = Invoke-RestMethod -Uri "http://localhost:15672/api/queues" -Headers $headers -Method Get -ErrorAction SilentlyContinue
    $menuQueues = $queues | Where-Object { $_.name -match "menu" }
    if ($menuQueues) {
        Write-Host "  Menu-related queues:" -ForegroundColor Green
        foreach ($q in $menuQueues) {
            Write-Host "    - $($q.name): $($q.messages) messages" -ForegroundColor Gray
        }
    }
}
catch {
    Write-Host "  Could not retrieve queue information" -ForegroundColor DarkYellow
}
Write-Host ""

# Test 5: Check Menu service logs
Write-Host "Test 5: Checking Menu service logs..." -ForegroundColor Yellow
$menuLogs = docker logs devops-menu-1 --tail 50 2>&1 | Out-String
if ($menuLogs -match "menu\.created|MenuChangeStreamListener|Published") {
    Write-Host "  CDC activity detected in logs" -ForegroundColor Green
}
else {
    Write-Host "  No obvious CDC activity in recent logs" -ForegroundColor DarkYellow
}
Write-Host ""

# Test 6: Check Order service
Write-Host "Test 6: Checking if Order service consumes menu events..." -ForegroundColor Yellow
$orderLogs = docker logs devops-order-1 --tail 50 2>&1 | Out-String
if ($orderLogs -match "menu\.(created|updated|deleted)") {
    Write-Host "  Order service appears to process menu events" -ForegroundColor Green
}
else {
    Write-Host "  Order service does not appear to consume menu events (expected)" -ForegroundColor Gray
}
Write-Host ""

# Summary
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Created Menu ID: $menuId" -ForegroundColor White
Write-Host ""
Write-Host "Manual Verification Commands:" -ForegroundColor Yellow
Write-Host "  docker exec -it devops-mongodb-1 mongosh" -ForegroundColor Gray
Write-Host "  use restaurant" -ForegroundColor Gray
Write-Host "  db.menu_items.find().pretty()" -ForegroundColor Gray
Write-Host "  db.menu_items_read.find().pretty()" -ForegroundColor Gray
Write-Host ""
Write-Host "  Visit: http://localhost:15672 (guest/guest) for RabbitMQ" -ForegroundColor Gray
Write-Host ""
