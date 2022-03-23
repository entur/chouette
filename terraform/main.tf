# Contains main description of bulk of terraform?
terraform {
  required_version = ">= 0.13.2"
}

provider "google" {
  version = "~> 4.13.0"
}
provider "kubernetes" {
  load_config_file = var.load_config_file
  version = "~> 1.13.4"
}

# Create bucket
resource "google_storage_bucket" "storage_bucket" {
  name               = "${var.bucket_instance_prefix}-${var.bucket_instance_suffix}"
  location           = var.bucket_location
  project            = var.gcp_resources_project
  storage_class      = var.bucket_storage_class
  labels             = merge(var.labels, {offsite_enabled = "false"})
  uniform_bucket_level_access = true


  lifecycle_rule {

    condition {
      age = var.bucket_retention_period
      with_state = "ANY"
    }
    action {
      type = "Delete"
    }
  }
}

# create service account
resource "google_service_account" "chouette_service_account" {
  account_id   = "${var.labels.team}-${var.labels.app}-sa"
  display_name = "${var.labels.team}-${var.labels.app} service account"
  project = var.gcp_resources_project
}

# add service account as member to the cloudsql client
resource "google_project_iam_member" "cloudsql_iam_member" {
  project = var.gcp_resources_project
  role    = var.service_account_cloudsql_role
  member = "serviceAccount:${google_service_account.chouette_service_account.email}"
}

# add service account as member to storage bucket
resource "google_storage_bucket_iam_member" "storage_bucket_iam_member" {
  bucket = google_storage_bucket.storage_bucket.name
  role   = var.service_account_bucket_role
  member = "serviceAccount:${google_service_account.chouette_service_account.email}"
}

# create key for service account
resource "google_service_account_key" "chouette_service_account_key" {
  service_account_id = google_service_account.chouette_service_account.name
}

  # Add SA key to to k8s
resource "kubernetes_secret" "chouette_service_account_credentials" {
  metadata {
    name      = "${var.labels.team}-${var.labels.app}-sa-key"
    namespace = var.kube_namespace
  }
  data = {
    "credentials.json" = base64decode(google_service_account_key.chouette_service_account_key.private_key)
  }
}

resource "kubernetes_secret" "ror-chouette-secret" {
  metadata {
    name      = "${var.labels.team}-${var.labels.app}-secret"
    namespace = var.kube_namespace
  }

  data = {
    "chouette-db-username"     = var.ror-chouette-db-username
    "chouette-db-password"     = var.ror-chouette-db-password
    "chouette-iev-db-username"     = var.ror-chouette-iev-db-username
    "chouette-iev-db-password"     = var.ror-chouette-iev-db-password
    "chouette-admin-initial-encrypted-password"     = var.ror-chouette-admin-initial-encrypted-password
    "chouette-user-initial-encrypted-password"     = var.ror-chouette-user-initial-encrypted-password
  }
}

resource "google_sql_database_instance" "db_instance" {
  name = "chouette-db-pg13"
  database_version = "POSTGRES_13"
  project = var.gcp_resources_project
  region = var.db_region

  settings {
    location_preference {
      zone = var.db_zone
    }
    tier = var.db_tier
    user_labels = var.labels
    availability_type = var.db_availability
    backup_configuration {
      enabled = true
      // 01:00 UTC
      start_time = "01:00"
    }
    maintenance_window {
      // Sunday
      day = 7
      // 02:00 UTC
      hour = 2
    }
    ip_configuration {
      require_ssl = true
    }
    database_flags {
      name = "work_mem"
      value = "15000"
    }
    database_flags {
      name = "log_min_duration_statement"
      value = "200"
    }
  }
}

resource "google_sql_database" "db-chouette" {
  name = "chouette"
  project = var.gcp_resources_project
  instance = google_sql_database_instance.db_instance.name
}

resource "google_sql_database" "db-iev" {
  name = "iev"
  project = var.gcp_resources_project
  instance = google_sql_database_instance.db_instance.name
}

resource "google_sql_user" "db-user" {
  name = var.ror-chouette-db-username
  project = var.gcp_resources_project
  instance = google_sql_database_instance.db_instance.name
  password = var.ror-chouette-db-password
}