# Automated Billing Pipeline Test

The PowerShell script `test/billing_e2e_test.ps1` exercises the menu, order, and bill services end-to-end through Kong.

## Prerequisites
- Docker compose stack is running (`docker compose up -d`).
- Kong routes are configured (run `./configure-kong.ps1` if unsure).
- PowerShell 5.1+ (installed by default on Windows).

## What the script does
1. Creates a unique menu item through Kong.
2. Places an order referencing that item.
3. Polls the bill API until the bill exists.
4. Validates bill totals (subtotal, tax, final amount) and order linkage.

## Usage
```powershell
# from repository root
docker compose up -d
./configure-kong.ps1
powershell -ExecutionPolicy Bypass -File test/billing_e2e_test.ps1
```

Optional parameters:
- `-BaseUrl` (default `http://localhost:18082`)
- `-PollAttempts` (default `12`)
- `-PollDelaySeconds` (default `5`)

Example with custom settings:
```powershell
powershell -ExecutionPolicy Bypass -File test/billing_e2e_test.ps1 -PollAttempts 20 -PollDelaySeconds 3
```

## Expected output
```
[STEP] Seeding menu item
[STEP] Menu item created: 69037...
[STEP] Creating order
[STEP] Order created: 69037...
[STEP] Polling for generated bill
[STEP] Bill retrieved: 69037...
[PASS] Bill generation pipeline verified
```

Non-zero exit codes indicate failures and are safe to plug into CI pipelines.
