# Restaurant microservices demo — cloud native + DevOps assignment

This workspace contains a simple restaurant-style microservices sample with the following components:

- `menu-service` (Java Spring Boot) — serves menu items, uses PostgreSQL
- `order-service` (Python FastAPI) — creates orders, uses PostgreSQL
- `bill-service` (Node.js) — calculates bill by querying order and menu services, stores results in PostgreSQL
- `review-service` (Go) — accepts reviews, stores in PostgreSQL

Each service has its own Dockerfile and (example) Kubernetes manifests in `k8s/`. Terraform under `terraform/` contains a starter template for GKE and Cloud SQL instances on GCP.

Included extras:
- `k8s/kong-deployment.yaml`: lightweight Kong (DB-less) demo for routing API traffic to services. For production, install Kong via Helm.
- `cloudbuild.yaml`: Cloud Build template to build & push images and deploy k8s manifests.

What I added in this session
- k8s manifests for `bill-service` and `review-service`
- example Kong deployment & declarative routes
- extended Terraform `main.tf` to create Cloud SQL instances for order, bill and review
- `cloudbuild.yaml` for CI/CD on GCP

Next steps (recommended)
1. Replace `gcr.io/YOUR_PROJECT/...` image tags in `k8s/` manifests with your real Artifact Registry/GCR image paths or use image substitution in Cloud Build.
2. Provision GKE and Cloud SQL using Terraform (fill variables and backend). Create DB users and network ACLs. Consider using private IPs for Cloud SQL.
3. Secure secrets: create Kubernetes Secrets for DB credentials and refer to them in `envFrom` or `valueFrom` instead of inlining plaintext.
4. Install Kong Ingress Controller via Helm for a production-like API gateway and use Ingress resources or KongIngress for route configuration.
5. Add DevSecOps scanning in CI:
   - Trivy for container images
   - Bandit for Python
   - OWASP Dependency-Check or mvn plugin for Java
   - npm audit for Node
   - gosec for Go
6. Add GitHub Actions or Cloud Build triggers to run lint/tests, scans, build images and run deploys.

How to try locally (minikube / kind)
1. Build images locally and load into your cluster (or use Cloud Build to push to GCR). Replace image tags in `k8s/` manifests.
2. kubectl apply -f k8s/
3. (If using Kong DB-less) kubectl apply -n kong -f k8s/kong-deployment.yaml

Notes about Windows PowerShell
- When running gcloud or kubectl commands in PowerShell, be sure to set env variables using `$env:VAR = 'value'` or pass via the command line.

If you want, I can now:
- wire Kubernetes Secrets and ConfigMaps for DB credentials,
- add Helm charts or kustomize overlays,
- create GitHub Actions workflow that runs scans and deploys,
- or expand the Terraform to include private IP Cloud SQL and VPC peering.

Tell me which next step you want me to implement and I'll continue.
# Restaurant Microservices Assignment (Cloud-native + DevOps + DevSecOps on GCP)

This repository is a scaffold for a cloud-native restaurant application implementing microservices, Docker, Kubernetes, Terraform (GCP), CI/CD, and DevSecOps. It contains four services (each with its own DB) and a minimal UI behind Kong API Gateway.

Services
- menu-service (Java Spring Boot) — exposes menu items (Postgres)
- order-service (Python FastAPI) — create orders (Postgres)
- bill-service (Node.js) — compute bills from orders and menu (Postgres)
- review-service (Go) — accept reviews (Postgres)
- ui — minimal web UI

Key artifacts
- `terraform/` — GCP skeleton: provider, GKE cluster, Cloud SQL instances (fill variables before apply)
- `k8s/` — Kubernetes manifests and templates for services, DBs and Kong hints
- `.github/workflows/ci-cd.yaml` — GitHub Actions workflow to build, scan, push and deploy

Assumptions and notes
- This scaffold focuses on clarity and minimal, functional code. You will need to supply GCP project IDs, a service account key (JSON) and set appropriate secrets in GitHub (see workflow).
- For local development you can run services with Docker Compose or `kind`/`minikube` and use local Postgres instances.
- Kong is recommended to be installed via Helm into the cluster; `k8s/` contains route examples.

Quick local run (docker-based, minimal)
1. Build images for services (example):
   - menu-service: `docker build -t menu-service:local ./menu-service`
2. Start Postgres instances (one per service) or use a single local Postgres with different DB names.
3. Run each service with environment variables pointing to the DB.

GCP Deploy (high-level)
1. Populate `terraform/terraform.tfvars` with values (project, region, zone, cluster name).
2. `terraform init` && `terraform apply` to create GKE + Cloud SQL.
3. Configure GitHub Secrets (GCP_SA_KEY) and let CI build and deploy images to GCR/Artifact Registry and apply K8s manifests.

Next steps
- Fill in Terraform variables and GCP service account details.
- Optionally add richer auth, observability (Prometheus/Grafana), and more thorough security scanning.

References
- Kong: https://docs.konghq.com
- GKE & Cloud SQL: Google Cloud docs

---
See folders for per-service README and more details.
