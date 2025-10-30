# Bill Service RabbitMQ Failure – Root Cause Report

## Overview
The bill service was unable to consume `order.created` events and therefore never generated new bills. The problem manifested as repeated container restarts with `PRECONDITION_FAILED` errors coming from RabbitMQ.

## Impact
- Newly created orders were never processed into bills.
- The `bill.generated` exchange never received messages, blocking downstream consumers.
- Manual bill lookups (`GET /bills/order/{orderId}`) returned 404 for new orders.

## Root Cause
RabbitMQ already contained the exchanges/queues created by the order service:
- Exchange `order.events`: `direct`, **non-durable**.
- Queue `order.created`: **non-durable**.

The bill service asserted these entities as durable. When RabbitMQ compared the incoming declaration (`durable=true`) with the existing definition (`durable=false`), it closed the channel with `PRECONDITION_FAILED`.

## Resolution
1. Updated `bill_service.js` to align with the broker’s topology:
   - Assert `bill.events` as durable (this exchange is created by bill-service itself).
   - Assert `order.events` and `order.created` as non-durable to match the order service.
   - Bind queues after ensuring declarations succeed.
2. Rebuilt and redeployed the bill-service container.
3. Added diagnostic logging and stricter numeric handling inside the bill generator for easier future triage.

## Verification
- Created a new menu item and order via Kong (`http://localhost:18082`).
- Observed bill-service logs showing receipt of the `order.created` event and successful bill generation.
- Confirmed `restaurant.bills` collection contains the new bill document with the expected totals.
- Verified `GET /bills/order/{orderId}` returns the generated bill.

## Follow-up Actions
- Add automated coverage (see `test/billing_e2e_test.ps1`).
- Decide on durability strategy for RabbitMQ exchanges/queues and document agreed conventions for future services.
