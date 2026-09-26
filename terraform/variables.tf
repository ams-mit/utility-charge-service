# terraform/variables.tf
#
# All sensitive values come from GitHub Secrets via environment variables.
# No secrets are ever hardcoded in .tf files.

variable "subscription_id" {
  description = "Azure Subscription ID"
  type        = string
}

variable "location" {
  description = "Azure region for all resources"
  type        = string
  default     = "southeastasia"
}

variable "resource_group_name" {
  description = "Resource group that contains all AMS G3 resources"
  type        = string
  default     = "ams-g3-utility-rg"
}

variable "acr_name" {
  description = "Azure Container Registry name — must be globally unique, alphanumeric only"
  type        = string
  default     = "amsg3utilityacr"
}

variable "mysql_server_name" {
  description = "MySQL Flexible Server name — must be globally unique"
  type        = string
  default     = "ams-g3-utility-mysql"
}

variable "mysql_admin_user" {
  description = "MySQL administrator username"
  type        = string
  default     = "amsadmin"
}

variable "mysql_admin_password" {
  description = "MySQL administrator password — injected from GitHub Secrets"
  type        = string
  sensitive   = true
}

variable "db_name" {
  description = "MySQL database name"
  type        = string
  default     = "utility_db"
}

variable "container_app_name" {
  description = "Azure Container App name"
  type        = string
  default     = "utility-charge-service"
}

variable "image_tag" {
  description = "Docker image tag to deploy — injected by GitHub Actions (git SHA)"
  type        = string
  default     = "latest"
}

# Spring application environment variables — injected from GitHub Secrets
variable "gateway_jwt_public_key" {
  description = "RSA public key for verifying Gateway JWTs"
  type        = string
  sensitive   = true
}

variable "service_jwt_private_key" {
  description = "RSA private key for signing outgoing Service JWTs"
  type        = string
  sensitive   = true
}

variable "api_gateway_url" {
  description = "URL of the API Gateway — used for inter-service calls"
  type        = string
}

variable "cors_allowed_origins" {
  description = "Allowed CORS origins for the frontend"
  type        = string
  default     = "*"
}