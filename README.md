# Restaurant Microservices System

## About The Project
A modern, distributed restaurant management system built using microservices architecture. The system handles menu management, order processing, billing, reviews, and real-time event monitoring through multiple independent services.

## Architecture Details
The system consists of the following core microservices:

- `menu-service` (Java/Spring Boot) — Menu management with MongoDB
- `order-service` (Python) — Order processing with MongoDB
- `bill-service` (Node.js) — Bill calculation and payment processing with MongoDB
- `review-service` (Go) — Customer review management with MongoDB
- `event-streaming-service` (Node.js) - Real-time log monitoring and visualization
- `ui` - Frontend interface for the system

## Detailed Architecture Overview

### System Architecture
```
┌─────────────────┐     ┌──────────────┐
│   Kong Gateway  │◄────┤  UI Service  │
└────────┬────────┘     └──────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│            Service Layer                 │
│                                         │
│  ┌──────┐  ┌───────┐  ┌─────┐  ┌─────┐ │
│  │ Menu │  │ Order │  │Bill │  │Review│ │
│  └──┬───┘  └───┬───┘  └──┬──┘  └──┬──┘ │
└─────│──────────│─────────│─────────│────┘
      │          │         │         │
      ▼          ▼         ▼         ▼
┌─────────────────────────────────────────┐
│           Message Broker                 │
│          (RabbitMQ Events)              │
└─────────────────────────────────────────┘
      │          │         │         │
      ▼          ▼         ▼         ▼
┌─────────────────────────────────────────┐
│         Event Streaming Service          │
└─────────────────────────────────────────┘
      │          │         │         │
      ▼          ▼         ▼         ▼
┌─────────────────────────────────────────┐
│             Databases                    │
│   (MongoDB + Postgres for Reviews)       │
└─────────────────────────────────────────┘
```

### CQRS Architecture (Menu Service Example)
```
┌───────────────┐
│   Commands    │
│ - CreateMenu  │
│ - UpdateMenu  │──┐
│ - DeleteMenu  │  │
└───────────────┘  │     ┌────────────────┐
                   ├────►│  Event Store    │
┌───────────────┐  │     │   (MongoDB)    │
│   Queries     │  │     └────────┬───────┘
│ - GetMenu     │  │              │
│ - ListMenus   │◄─┘              │
└───────────────┘         ┌───────▼───────┐
                         │  Projections   │
                         │  (Read Model)  │
                         └───────────────┘
```

### Change Data Capture (CDC) Flow
```
┌──────────────┐    ┌─────────────┐    ┌──────────────┐
│   MongoDB    │    │  Change     │    │   Event      │
│   OpLog      │───►│  Stream     │───►│   Bus        │
└──────────────┘    │  Listener   │    │ (RabbitMQ)   │
                    └─────────────┘    └──────┬───────┘
                                             │
                    ┌─────────────┐          │
                    │  Service    │          │
                    │  Consumers  │◄─────────┘
                    └─────────────┘
```



## Technology Stack

### Backend
- **Java/Spring Boot**: Menu service with MongoDB integration
- **Python**: Order service with FastAPI
- **Node.js**: Bill service and Event streaming
- **Go**: Review service
- **MongoDB**: Primary database with replica set
- **RabbitMQ**: Message broker for service communication
- **WebSocket**: Real-time event streaming

### Frontend
- HTML5, CSS3, JavaScript
- Real-time log visualization
- Responsive design

### DevOps & Infrastructure
- **Docker**: Containerization
- **Kubernetes**: Container orchestration
- **Kong**: API Gateway
- **Terraform**: Infrastructure as Code
- **GitHub Actions**: CI/CD pipeline
- **Cloud Platform**: GCP ready

### Architectural Patterns

#### CQRS & CDC by Service

This project mixes CQRS and CDC patterns. Below is a per-service summary (what exists in the codebase today).

- Menu Service (Java / Spring Boot)
  - Implements CQRS in the codebase (separate command/query packages under `src/main/java/.../cqrs`).
  - CDC: `MenuChangeStreamListener` watches the `menu_items` collection and publishes events to RabbitMQ.
  - RabbitMQ exchange: `menu.events` with routing keys: `menu.created`, `menu.updated`, `menu.deleted`.
  - Read model / projections: maintained separately (projections updated from events / change stream).
  - Example event payload (menu.created):
    ```json
    { "id": "<objId>", "name": "Burger", "description": "...", "price": 7.99 }
    ```

- Review Service (Go)
  - CDC: The service starts a MongoDB change stream on the `reviews` collection and publishes `review.submitted` events.
  - RabbitMQ exchange: `review.events` with routing key `review.submitted`.
  - CQRS: the code reads from a `reviews_read` collection for query operations (`GetReview`, `GetReviewsByOrder`) — this is a read-side projection that can be maintained asynchronously (CQRS read model).
  - Example event payload (review.submitted):
    ```json
    { "id": "<id>", "orderId": "<orderId>", "rating": 5, "comment": "Great!" }
    ```

- Order Service (Python)
  - CDC: `order_service.py` starts a MongoDB change stream for `orders` and publishes domain events to RabbitMQ.
  - RabbitMQ exchange: `order.events` with routing keys: `order.created`, `order.updated`.
  - Typical usage: producers write to `orders` (command side), change stream publishes events for downstream consumers.
  - Example event payload (order.created):
    ```json
    { "id": "<id>", "customerId": "c123", "items": [...], "status": "PENDING" }
    ```

- Bill Service (Node.js)
  - Event-driven consumer + CDC producer:
    - Consumes `order.created` events to generate bills (synchronous/asynchronous bill generation).
    - Persists bills to `bills` collection and exposes a MongoDB change stream that publishes `bill.generated` events.
  - RabbitMQ exchanges:
    - Consumes from `order.events` / `order.created`
    - Publishes to `bill.events` with routing key `bill.generated`.
  - Read model: `bills` collection (used for queries). The service also publishes events for other consumers.
  - Example event payload (bill.generated):
    ```json
    { "id": "<id>", "orderId": "<orderId>", "finalAmount": 12.34 }
    ```

- Event Streaming Service (Node.js)
  - Infrastructure consumer: subscribes to `logs.*` (exchanges asserted in code: `logs.menu`, `logs.order`, `logs.bill`, `logs.review`) and broadcasts to WebSocket/Socket.IO clients.
  - Purpose: central real-time view of domain events and system logs.

### Patterns & How They Fit Together

- Commands write to the primary datastore for a service (MongoDB). Many services use the database as the source-of-truth for commands.
- Change streams (CDC) detect those writes and publish domain/integration events to RabbitMQ.
- Consumers subscribe to RabbitMQ to react (e.g., Bill Service reacts to `order.created`).
- Some services maintain a purpose-built read model (e.g., `reviews_read`), enabling CQRS-style queries.

This layout lets services remain decoupled and scale read/write paths independently. The current codebase contains concrete CDC listeners for Menu, Order, Bill and Review; Menu and Review include explicit read-side projection logic consistent with CQRS.

### Service Endpoints (via Kong Gateway - http://localhost:18082)

#### Menu Service (Port: 18080)
- `GET /menu` - List all menu items
- `POST /menu` - Add new menu item
- `GET /menu/{id}` - Get menu item by ID
- `PUT /menu/{id}` - Update menu item
- `DELETE /menu/{id}` - Delete menu item

#### Order Service (Port: 8000)
- `POST /orders` - Create new order
- `GET /orders` - List all orders
- `GET /orders/{id}` - Get order by ID
- `PUT /orders/{id}` - Update order status

#### Bill Service (Port: 3000)
- `POST /bills` - Create new bill
- `GET /bills` - List all bills
- `GET /bills/{id}` - Get bill by ID
- `PUT /bills/{id}` - Update bill status

#### Review Service (Port: 4000)
- `POST /reviews` - Create new review
- `GET /reviews` - List all reviews
- `GET /reviews/{id}` - Get review by ID
- `PUT /reviews/{id}` - Update review
- `DELETE /reviews/{id}` - Delete review

#### Event Streaming Service (Port: 13100)
- `WS /logs` - WebSocket endpoint for real-time log streaming

### Infrastructure Details
### Component Ports
- Kong API Gateway: 
  - Proxy: 18082
  - Admin API: 18083
- Menu Service: 18080
- Order Service: 8000
- Bill Service: 3000
- Review Service: 4000
- Event Streaming: 13100
- RabbitMQ: 
  - AMQP: 5672
  - Management UI: 15672
- MongoDB: 27017

## Getting Started

### Prerequisites
- Docker and Docker Compose
- PowerShell (for Windows)
- Git
- Postman (for testing)
### Installation & Setup

1. Clone the repository:
```bash
git clone https://github.com/anmolp-03/Microservices_Innovative.git
cd devops
```

2. Start all services using Docker Compose:
```bash
docker-compose up --build -d
```

3. Configure Kong API Gateway:
```powershell
./configure-kong.ps1
```

## Testing
The repository includes various testing scripts:
```powershell
# Run E2E tests
./test/e2e-test.ps1

# Test billing pipeline
./test/billing_e2e_test.ps1

# Verify menu CDC
./test/verify_menu_cdc.ps1
```

