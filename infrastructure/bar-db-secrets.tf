//////////////////////////////////////////////////////
// Copy Digital BAR DB secrets into ccpay Key Vault //
//////////////////////////////////////////////////////

data "azurerm_key_vault" "bar_key_vault" {
  name                = "bar-${var.env}"
  resource_group_name = "bar-${var.env}-rg"
}

data "azurerm_key_vault_secret" "bar_api_postgres_host" {
  name         = "api-POSTGRES-HOST"
  key_vault_id = data.azurerm_key_vault.bar_key_vault.id
}

data "azurerm_key_vault_secret" "bar_api_postgres_port" {
  name         = "api-POSTGRES-PORT"
  key_vault_id = data.azurerm_key_vault.bar_key_vault.id
}

data "azurerm_key_vault_secret" "bar_api_postgres_database" {
  name         = "api-POSTGRES-DATABASE"
  key_vault_id = data.azurerm_key_vault.bar_key_vault.id
}

data "azurerm_key_vault_secret" "bar_api_postgres_user" {
  name         = "api-POSTGRES-USER"
  key_vault_id = data.azurerm_key_vault.bar_key_vault.id
}

data "azurerm_key_vault_secret" "bar_api_postgres_pass" {
  name         = "api-POSTGRES-PASS"
  key_vault_id = data.azurerm_key_vault.bar_key_vault.id
}

resource "azurerm_key_vault_secret" "ccpay_bar_api_postgres_host" {
  name         = "bar-api-POSTGRES-HOST"
  value        = data.azurerm_key_vault_secret.bar_api_postgres_host.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "ccpay_bar_api_postgres_port" {
  name         = "bar-api-POSTGRES-PORT"
  value        = data.azurerm_key_vault_secret.bar_api_postgres_port.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "ccpay_bar_api_postgres_database" {
  name         = "bar-api-POSTGRES-DATABASE"
  value        = data.azurerm_key_vault_secret.bar_api_postgres_database.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "ccpay_bar_api_postgres_user" {
  name         = "bar-api-POSTGRES-USER"
  value        = data.azurerm_key_vault_secret.bar_api_postgres_user.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "ccpay_bar_api_postgres_pass" {
  name         = "bar-api-POSTGRES-PASS"
  value        = data.azurerm_key_vault_secret.bar_api_postgres_pass.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}
