// Terraform skeleton for GCP resources (GKE + Cloud SQL)
// NOTE: This is a starting template. Fill variables and review before applying.

terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

provider "google" {
  project = var.project
  region  = var.region
  zone    = var.zone
}

variable "project" {}
variable "region" { default = "us-central1" }
variable "zone" { default = "us-central1-a" }
variable "cluster_name" { default = "restaurant-cluster" }

resource "google_container_cluster" "primary" {
  name     = var.cluster_name
  location = var.zone

  remove_default_node_pool = true
  initial_node_count       = 1

  ip_allocation_policy {}
}

resource "google_container_node_pool" "primary_nodes" {
  name       = "primary-node-pool"
  cluster    = google_container_cluster.primary.name
  location   = var.zone

  node_config {
    machine_type = "e2-medium"
    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform",
    ]
  }

  initial_node_count = 2
}

// Cloud SQL instances (one per service). Example for menu DB.
resource "google_sql_database_instance" "menu-db" {
  name             = "menu-db-instance"
  database_version = "POSTGRES_14"
  region           = var.region

  settings {
    tier = "db-f1-micro"
  }
}

// Create other Cloud SQL instances for each microservice: order, bill, review
resource "google_sql_database_instance" "order-db" {
  name             = "order-db-instance"
  database_version = "POSTGRES_14"
  region           = var.region

  settings {
    tier = "db-f1-micro"
  }
}

resource "google_sql_database_instance" "bill-db" {
  name             = "bill-db-instance"
  database_version = "POSTGRES_14"
  region           = var.region

  settings {
    tier = "db-f1-micro"
  }
}

resource "google_sql_database_instance" "review-db" {
  name             = "review-db-instance"
  database_version = "POSTGRES_14"
  region           = var.region

  settings {
    tier = "db-f1-micro"
  }
}

output "kubernetes_cluster_name" {
  value = google_container_cluster.primary.name
}
