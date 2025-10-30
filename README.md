# Restaurant Microservices System

## About The Project
A modern, distributed restaurant management system built using microservices architecture. The system handles menu management, order processing, billing, reviews, and real-time event monitoring through multiple independent services.

## Architecture Overview
The system consists of the following core microservices:

- `menu-service` (Java/Spring Boot) — Menu management with MongoDB
- `order-service` (Python) — Order processing with MongoDB
- `bill-service` (Node.js) — Bill calculation and payment processing with MongoDB
- `review-service` (Go) — Customer review management with MongoDB
- `event-streaming-service` (Node.js) - Real-time log monitoring and visualization
- `ui` - Frontend interface for the system

How to try locally (minikube / kind)
1. Build images locally and load into your cluster (or use Cloud Build to push to GCR). Replace image tags in `k8s/` manifests.
2. kubectl apply -f k8s/
3. (If using Kong DB-less) kubectl apply -n kong -f k8s/kong-deployment.yaml

## Technology Stack

### Backend
- **Java/Spring Boot**: Menu service with MongoDB integration
- **Python**: Order service with FastAPI
- **Node.js**: Bill service and Event streaming
- **Go**: Review service
- **MongoDB**: Primary database with replica set
- **RabbitMQ**: Message broker for service communication
- **WebSocket**: Real-time event streaming

### DevOps & Infrastructure
- **Docker**: Containerization
- **Kubernetes**: Container orchestration
- **Kong**: API Gateway
- **Terraform**: Infrastructure as Code
- **GitHub Actions**: CI/CD pipeline
- **Cloud Platform**: GCP ready

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

## Deployment

### Local Development
Use Docker Compose for local development and testing:
```bash
docker-compose up --build -d
```

### Cloud Deployment (GCP)
1. Configure Terraform variables in `terraform/terraform.tfvars`:
   - Project ID
   - Region/Zone
   - Cluster configuration

2. Initialize and apply Terraform:
```bash
cd terraform
terraform init
terraform apply
```

3. Configure GitHub Actions secrets for CI/CD:
   - GCP_SA_KEY
   - PROJECT_ID
   - Other required credentials

## Documentation
- [Test Automation Guide](docs/test-automation.md)
- [Menu CDC Verification Results](docs/menu-cdc-verification-results.md)
- [Bill Service Root Cause Analysis](docs/bill-service-root-cause.md)

## Contributing
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Contributors
Anmol Panjwani
Mahek Mehta
Shivani Padhiyar
