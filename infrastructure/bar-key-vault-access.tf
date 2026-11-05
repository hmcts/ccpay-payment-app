// bar-shared-infrastructure is archived, so grant the running Jenkins MI read access to the bar vault here.
data "azurerm_client_config" "current" {}

data "azurerm_key_vault" "bar_key_vault_access" {
  name                = "bar-${var.env}"
  resource_group_name = "bar-${var.env}-rg"
}

resource "azurerm_key_vault_access_policy" "jenkins_bar_secrets_read" {
  key_vault_id = data.azurerm_key_vault.bar_key_vault_access.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azurerm_client_config.current.object_id

  secret_permissions = ["Get", "List"]
}
