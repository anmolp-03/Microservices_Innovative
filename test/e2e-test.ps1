# e2e-test.ps1: End-to-end test of the microservices chain
# Note: Run after starting all containers with DB initialization

$ErrorActionPreference = 'Stop'

Write-Host "Testing Menu Service (add item)..."
$menuItem = @{
    name = "Pepperoni Pizza"
    price = 12.99
} | ConvertTo-Json

$menuResp = Invoke-RestMethod -Uri "http://localhost:8080/menu" -Method Post -Body $menuItem -ContentType "application/json"
$menuId = $menuResp.id
Write-Host "Created menu item with ID: $menuId"

Write-Host "`nTesting Order Service (create order)..."
$order = @{
    customer = "John Doe"
    items = @(
        @{
            menu_id = $menuId
            qty = 2
        }
    )
} | ConvertTo-Json

$orderResp = Invoke-RestMethod -Uri "http://localhost:8000/orders" -Method Post -Body $order -ContentType "application/json"
$orderId = $orderResp.order_id
Write-Host "Created order with ID: $orderId"

Write-Host "`nTesting Bill Service (generate bill)..."
$billReq = @{
    order_id = $orderId
} | ConvertTo-Json

$billResp = Invoke-RestMethod -Uri "http://localhost:3000/bills" -Method Post -Body $billReq -ContentType "application/json"
Write-Host "Generated bill. Total: $($billResp.total)"

Write-Host "`nTesting Review Service (post review)..."
$review = @{
    customer = "John Doe"
    rating = 5
    comment = "Great pizza and fast service!"
} | ConvertTo-Json

$reviewResp = Invoke-RestMethod -Uri "http://localhost:4000/reviews" -Method Post -Body $review -ContentType "application/json"
Write-Host "Posted review successfully"

Write-Host "`nEnd-to-end test completed successfully!"