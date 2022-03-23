#Enviroment variables

variable "gcp_resources_project" {
  description = "The GCP project hosting the project resources"
}

variable "kube_namespace" {
  description = "The Kubernetes namespace"
}

variable "labels" {
  description = "Labels used in all resources"
  type        = map(string)
     default = {
       manager = "terraform"
       team    = "ror"
       slack   = "talk-ror"
       app     = "chouette"
     }
}

variable "bucket_location" {
  description = "GCP bucket location"
  default = "europe-west1"
}

variable "bucket_instance_suffix" {
  description = "A suffix for the bucket instance, may be changed if environment is destroyed and then needed again (name collision workaround) - also bucket names must be globally unique"
}

variable "bucket_instance_prefix" {
  description = "A prefix for the bucket instance, may be changed if environment is destroyed and then needed again (name collision workaround) - also bucket names must be globally unique"
  default     = "ror-chouette"
}

variable "bucket_storage_class" {
  description = "GCP storage class"
  default     = "REGIONAL"
}

variable "bucket_retention_period" {
  description = "Retention period for GCS objects, in days"
  default     = "105"
}

variable "load_config_file" {
  description = "Do not load kube config file"
  default     = false
}

variable "service_account_cloudsql_role" {
  description = "Role of the Service Account - more about roles https://cloud.google.com/pubsub/docs/access-control"
  default     = "roles/cloudsql.client"
}

variable "service_account_bucket_role" {
  description = "Role of the Service Account - more about roles https://cloud.google.com/storage/docs/access-control/iam-roles"
  default     = "roles/storage.objectViewer"
}

variable "ror-chouette-db-username" {
  description = "chouette database username"
}

variable "ror-chouette-db-password" {
  description = "chouette database password"
}

variable "ror-chouette-iev-db-username" {
  description = "chouette IEV database username"
}

variable "ror-chouette-iev-db-password" {
  description = "chouette IEV database password"
}

variable "ror-chouette-admin-initial-encrypted-password" {
  description = "chouette admin initial encrypted password"
}

variable "ror-chouette-user-initial-encrypted-password" {
  description = "chouette user initial encrypted password"
}

variable "db_region" {
  description = "GCP  region"
  default = "europe-west1"
}

variable "db_zone" {
  description = "GCP zone letter"
  default = "europe-west1-b"
}

variable "db_tier" {
  description = "Database instance tier"
  default = "db-custom-4-19200"
}

variable "db_availability" {
  description = "Database availablity"
  default = "ZONAL"
}