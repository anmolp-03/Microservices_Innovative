param(
    [string]$BaseUrl = "http://localhost:18082",
    [int]$PollAttempts = 12,
    [int]$PollDelaySeconds = 5
)

function Write-Step {
    param([string]$Message)
    Write-Host "[STEP] $Message" -ForegroundColor Cyan
}

function Fail-Test {
    param([string]$Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
    exit 1
}

try {
    Write-Step "Seeding menu item"
    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    $menuBody = @{
        name        = "E2E Pizza $timestamp"
        description = "Automated test item"
        price       = 19.95
    } | ConvertTo-Json

    $menuResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/menu" -Body $menuBody -ContentType 'application/json'
    $menuId = $menuResponse.id
    if (-not $menuId) {
        Fail-Test "Menu creation failed"
    }
    Write-Step "Menu item created: $menuId"

    Write-Step "Creating order"
    $orderBody = @{
        customerId = "automated-$timestamp"
        items      = @(
            @{
                menuId   = $menuId
                quantity = 2
            }
        )
    } | ConvertTo-Json

    $orderResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/orders" -Body $orderBody -ContentType 'application/json'
    $orderId = $orderResponse.order_id
    if (-not $orderId) {
        Fail-Test "Order creation failed"
    }
    Write-Step "Order created: $orderId"

    Write-Step "Polling for generated bill"
    $bill = $null
    for ($i = 0; $i -lt $PollAttempts; $i++) {
        try {
            $bill = Invoke-RestMethod -Uri "$BaseUrl/bills/order/$orderId" -ErrorAction Stop
            break
        } catch {
            Start-Sleep -Seconds $PollDelaySeconds
        }
    }

    if (-not $bill) {
        Fail-Test "Bill not generated within $($PollAttempts * $PollDelaySeconds)s"
    }

    Write-Step "Bill retrieved: $($bill._id)"

    $expectedTotal = [math]::Round(([decimal]$menuResponse.price) * 2, 2)
    $expectedTax = [math]::Round($expectedTotal * 0.1, 2)
    $expectedFinal = [math]::Round($expectedTotal + $expectedTax, 2)

    if ([decimal]$bill.totalAmount -ne $expectedTotal) {
        Fail-Test "Unexpected totalAmount. Expected $expectedTotal got $($bill.totalAmount)"
    }
    if ([decimal]$bill.tax -ne $expectedTax) {
        Fail-Test "Unexpected tax. Expected $expectedTax got $($bill.tax)"
    }
    if ([decimal]$bill.finalAmount -ne $expectedFinal) {
        Fail-Test "Unexpected finalAmount. Expected $expectedFinal got $($bill.finalAmount)"
    }
    if ($bill.orderId -ne $orderId) {
        Fail-Test "Bill orderId mismatch"
    }

    Write-Host "[PASS] Bill generation pipeline verified" -ForegroundColor Green
    exit 0
} catch {
    Fail-Test $_.Exception.Message
}
