provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

provider "azurerm" {
  alias = "sendgrid"
  features {}
  subscription_id = var.env != "prod" ? local.sendgrid_subscription.nonprod : local.sendgrid_subscription.prod
}

locals {

  vaultName = join("-", [var.core_product, var.env])

  s2sUrl = "http://rpe-service-auth-provider-${var.env}.service.core-compute-${var.env}.internal"

  #region API gateway
  thumbprints_in_quotes     = formatlist("&quot;%s&quot;", var.telephony_api_gateway_certificate_thumbprints)
  thumbprints_in_quotes_str = join(",", local.thumbprints_in_quotes)
  api_base_path             = "telephony-api"
  db_server_name            = join("-", [var.product, "postgres-db-v15"])
  # endregion

  sendgrid_subscription = {
    prod    = "8999dec3-0104-4a27-94ee-6588559729d1"
    nonprod = "1c4f0704-a29e-403d-b719-b90c34ef14c9"
  }
}

data "azurerm_key_vault" "payment_key_vault" {
  name                = local.vaultName
  resource_group_name = join("-", [var.core_product, var.env])
}

data "azurerm_key_vault_secret" "gov_pay_keys_cmc" {
  name         = "gov-pay-keys-cmc"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

//created seperate kv for cmc claim store and copied claim store value
resource "azurerm_key_vault_secret" "gov-pay-keys-cmc-claim-store" {
  name         = "gov-pay-keys-cmc-claim-store"
  value        = data.azurerm_key_vault_secret.gov_pay_keys_cmc.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

module "payment-database-v15" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  product              = var.product
  component            = var.component
  business_area        = "cft"
  name                 = local.db_server_name
  location             = var.location
  env                  = var.env
  pgsql_admin_username = var.postgresql_user

  # Setup Access Reader db user
  force_user_permissions_trigger = "1"

  pgsql_databases = [
    {
      name : var.database_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "pg_stat_statements,pg_buffercache,hypopg"
    }
  ]
  pgsql_sku            = var.flexible_sku_name
  admin_user_object_id = var.jenkins_AAD_objectId
  common_tags          = var.common_tags
  pgsql_version        = var.postgresql_flexible_sql_version

  action_group_name           = join("-", [var.db_monitor_action_group_name, local.db_server_name, var.env])
  email_address_key           = var.db_alert_email_address_key
  email_address_key_vault_id  = data.azurerm_key_vault.payment_key_vault.id
}

# Populate Vault with DB info
resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name = join("-", [var.component, "POSTGRES-USER"])
  value        = module.payment-database-v15.username
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name = join("-", [var.component, "POSTGRES-PASS"])
  value        = module.payment-database-v15.password
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name = join("-", [var.component, "POSTGRES-HOST"])
  value        = module.payment-database-v15.fqdn
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name = join("-", [var.component, "POSTGRES-PORT"])
  value        = var.postgresql_flexible_server_port
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name = join("-", [var.component, "POSTGRES-DATABASE"])
  value        = var.database_name
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

# Populate Vault with SendGrid API token
data "azurerm_key_vault" "sendgrid" {
  provider = azurerm.sendgrid
  name                = var.env != "prod" ? "sendgridnonprod" : "sendgridprod"
  resource_group_name = var.env != "prod" ? "SendGrid-nonprod" : "SendGrid-prod"
}

data "azurerm_key_vault_secret" "sendgrid-api-key" {
  provider     = azurerm.sendgrid
  name         = "hmcts-payment-api-key"
  key_vault_id = data.azurerm_key_vault.sendgrid.id
}

resource "azurerm_key_vault_secret" "spring-mail-password" {
  name         = "spring-mail-password"
  value        = data.azurerm_key_vault_secret.sendgrid-api-key.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

# region API (gateway)
data "azurerm_key_vault_secret" "s2s_client_secret" {
  name         = "gateway-s2s-client-secret"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "azurerm_key_vault_secret" "s2s_client_id" {
  name         = "gateway-s2s-client-id"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}
