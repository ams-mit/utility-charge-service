# terraform/main.tf

terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.90"
    }
  }
  required_version = ">= 1.5.0"
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}

# ─────────────────────────────────────────────────────────────────────
# RESOURCE GROUP
# A container for all resources. Deleting the resource group deletes
# everything inside it — useful for cleanup after the project.
# ─────────────────────────────────────────────────────────────────────
resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location

  tags = {
    project     = "AMS-Group3"
    service     = "utility-charge-service"
    environment = "production"
    managed_by  = "terraform"
  }
}

# ─────────────────────────────────────────────────────────────────────
# AZURE CONTAINER REGISTRY (ACR)
# Private Docker registry. GitHub Actions pushes the image here.
# Container Apps pulls from here to run your service.
# Basic SKU = cheapest tier, fine for a student project.
# ─────────────────────────────────────────────────────────────────────
resource "azurerm_container_registry" "acr" {
  name                = var.acr_name
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "Basic"
  admin_enabled       = true    # enables username/password auth for GitHub Actions push

  tags = azurerm_resource_group.main.tags
}

# ─────────────────────────────────────────────────────────────────────
# MYSQL FLEXIBLE SERVER
# Azure's managed MySQL 8.0 service.
# Burstable B1ms = smallest tier, uses minimal credits.
# backup_retention_days = 1 minimises cost.
# ─────────────────────────────────────────────────────────────────────
resource "azurerm_mysql_flexible_server" "mysql" {
  name                   = var.mysql_server_name
  resource_group_name    = azurerm_resource_group.main.name
  location               = azurerm_resource_group.main.location
  administrator_login    = var.mysql_admin_user
  administrator_password = var.mysql_admin_password
  sku_name = "GP_Standard_D2ds_v4"
  version                = "8.0.21"

  backup_retention_days        = 1
  geo_redundant_backup_enabled = false

  storage {
    size_gb = 20
    iops    = 396
  }

  tags = azurerm_resource_group.main.tags
}

# Allow Container Apps to reach MySQL
# This rule allows all Azure services to connect.
resource "azurerm_mysql_flexible_server_firewall_rule" "allow_azure_services" {
  name                = "AllowAzureServices"
  resource_group_name = azurerm_resource_group.main.name
  server_name         = azurerm_mysql_flexible_server.mysql.name
  start_ip_address    = "0.0.0.0"
  end_ip_address      = "0.0.0.0"
}

# Create the utility_db database inside the MySQL server
resource "azurerm_mysql_flexible_database" "utility_db" {
  name                = var.db_name
  resource_group_name = azurerm_resource_group.main.name
  server_name         = azurerm_mysql_flexible_server.mysql.name
  charset             = "utf8mb4"
  collation           = "utf8mb4_unicode_ci"
}

# ─────────────────────────────────────────────────────────────────────
# LOG ANALYTICS WORKSPACE
# Required by Container Apps Environment.
# Stores your application logs so you can view them in Azure portal.
# ─────────────────────────────────────────────────────────────────────
resource "azurerm_log_analytics_workspace" "logs" {
  name                = "ams-g3-utility-logs"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = azurerm_resource_group.main.tags
}

# ─────────────────────────────────────────────────────────────────────
# CONTAINER APPS ENVIRONMENT
# The shared network environment where Container Apps run.
# Think of it as the "namespace" that groups your containers.
# ─────────────────────────────────────────────────────────────────────
resource "azurerm_container_app_environment" "env" {
  name                       = "ams-g3-utility-env"
  resource_group_name        = azurerm_resource_group.main.name
  location                   = azurerm_resource_group.main.location
  log_analytics_workspace_id = azurerm_log_analytics_workspace.logs.id

  tags = azurerm_resource_group.main.tags
}

# ─────────────────────────────────────────────────────────────────────
# CONTAINER APP
# Runs your Docker image. Exposes port 8082. Gets a public HTTPS URL.
# min_replicas = 0 means it scales to zero when not in use → saves credits.
# max_replicas = 1 is enough for a student project demo.
# ─────────────────────────────────────────────────────────────────────
resource "azurerm_container_app" "utility_service" {
  name                         = var.container_app_name
  resource_group_name          = azurerm_resource_group.main.name
  container_app_environment_id = azurerm_container_app_environment.env.id
  revision_mode                = "Single"

  # ACR credentials so Container Apps can pull your image
  registry {
    server               = azurerm_container_registry.acr.login_server
    username             = azurerm_container_registry.acr.admin_username
    password_secret_name = "acr-password"
  }

  secret {
    name  = "acr-password"
    value = azurerm_container_registry.acr.admin_password
  }

  template {
    min_replicas = 0
    max_replicas = 1

    container {
      name   = "utility-charge-service"
      image  = "${azurerm_container_registry.acr.login_server}/${var.container_app_name}:${var.image_tag}"
      cpu    = 0.5
      memory = "1Gi"

      # ── Spring application environment variables ──────────────────
      # These map to your application.yml ${ENV_VAR} placeholders.
      # Injected from Terraform variables (which come from GitHub Secrets).

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      env {
        name  = "SERVICE_NAME"
        value = "utility-charge-service"
      }
      env {
        name  = "SERVER_PORT"
        value = "8082"
      }
      env {
        name  = "DB_URL"
        value = "jdbc:mysql://${azurerm_mysql_flexible_server.mysql.fqdn}:3306/${var.db_name}?useSSL=true&requireSSL=true&serverTimezone=UTC"
      }
      env {
        name  = "DB_USER"
        value = var.mysql_admin_user
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      env {
        name        = "GATEWAY_JWT_PUBLIC_KEY"
        secret_name = "gateway-jwt-public-key"
      }
      env {
        name        = "SERVICE_JWT_PRIVATE_KEY"
        secret_name = "service-jwt-private-key"
      }
      env {
        name  = "SERVICE_JWT_EXPIRES_IN"
        value = "5m"
      }
      env {
        name  = "API_GATEWAY_URL"
        value = var.api_gateway_url
      }
      env {
        name  = "CORS_ALLOWED_ORIGINS"
        value = var.cors_allowed_origins
      }

      # Health check maps to your Dockerfile HEALTHCHECK
      liveness_probe {
        transport = "HTTP"
        path      = "/actuator/health"
        port      = 8082

        initial_delay           = 30
        interval_seconds        = 30
        timeout                 = 5
        failure_count_threshold = 3
      }

      readiness_probe {
        transport = "HTTP"
        path      = "/actuator/health"
        port      = 8082

        interval_seconds        = 10
        timeout                 = 5
        failure_count_threshold = 3
      }
    }
  }

  # Secrets for sensitive values — NOT passed as plain env vars
  secret {
    name  = "db-password"
    value = var.mysql_admin_password
  }
  secret {
    name  = "gateway-jwt-public-key"
    value = var.gateway_jwt_public_key
  }
  secret {
    name  = "service-jwt-private-key"
    value = var.service_jwt_private_key
  }

  # Ingress — makes the Container App publicly accessible on HTTPS
  ingress {
    external_enabled = true
    target_port      = 8082
    transport        = "http"

    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }

  tags = azurerm_resource_group.main.tags
}