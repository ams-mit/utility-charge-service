# terraform/backend.tf
#
# WHY REMOTE STATE?
# When GitHub Actions runs Terraform, it needs to know what was already created.
# Without remote state, every pipeline run would think nothing exists and try to
# create everything from scratch. Storing state in Azure Blob Storage means
# every run can see what was already done.
#
# NOTE: The storage account for the state file is created MANUALLY once
# in Phase 4 (bootstrap step). It is the only resource you create manually.
# Everything else is created by Terraform.

terraform {
  backend "azurerm" {
    resource_group_name  = "ams-g3-tfstate-rg"
    storage_account_name = "amsg3tfstate"
    container_name       = "tfstate"
    key                  = "utility-charge-service.terraform.tfstate"
  }
}