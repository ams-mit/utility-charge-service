# terraform/outputs.tf
# Values printed after Terraform apply — useful for verifying what was created.

output "container_app_url" {
  description = "Public HTTPS URL of the utility-charge-service"
  value       = "https://${azurerm_container_app.utility_service.ingress[0].fqdn}"
}

output "acr_login_server" {
  description = "ACR login server URL — used by GitHub Actions to push images"
  value       = azurerm_container_registry.acr.login_server
}

output "mysql_host" {
  description = "MySQL server hostname"
  value       = azurerm_mysql_flexible_server.mysql.fqdn
}

output "resource_group_name" {
  description = "Resource group containing all resources"
  value       = azurerm_resource_group.main.name
}