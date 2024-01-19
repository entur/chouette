terraform {
  required_version = ">= 0.13.2"
}

provider "google" {
  version = "~> 4.84.0"
}
provider "kubernetes" {
  version = "~> 2.13.1"
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
      value = "30000"
    }
    database_flags {
      name = "log_min_duration_statement"
      value = "200"
    }
    insights_config {
      query_insights_enabled = true
      query_string_length = 4500
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





