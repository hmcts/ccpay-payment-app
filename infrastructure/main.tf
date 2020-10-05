provider "azurerm" {
  version = "=2.20.0"
  features {}
}

locals {

  vaultName = "${var.core_product}-${var.env}"

  s2sUrl = "http://rpe-service-auth-provider-${var.env}.service.core-compute-${var.env}.internal"

  #region API gateway
  thumbprints_in_quotes = "${formatlist("&quot;%s&quot;", var.telephony_api_gateway_certificate_thumbprints)}"
  thumbprints_in_quotes_str = "${join(",", local.thumbprints_in_quotes)}"
  api_policy = "${replace(file("template/api-policy.xml"), "ALLOWED_CERTIFICATE_THUMBPRINTS", local.thumbprints_in_quotes_str)}"
  api_base_path = "telephony-api"
  # endregion
}

data "azurerm_key_vault" "payment_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${var.core_product}-${var.env}"
}

data "azurerm_key_vault_secret" "gov_pay_keys_cmc" {
  name = "gov-pay-keys-cmc"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

//created seperate kv for cmc claim store and copied claim store value
resource "azurerm_key_vault_secret" "gov-pay-keys-cmc-claim-store" {
  name = "gov-pay-keys-cmc-claim-store"
  value = data.azurerm_key_vault_secret.gov_pay_keys_cmc.value
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}


module "payment-database" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${var.product}-postgres-db"
  location = var.location
  env = var.env
  postgresql_user = var.postgresql_user
  database_name = var.database_name
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  common_tags = var.common_tags
  subscription = var.subscription
}

# Populate Vault with DB info

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name      = join("-", [var.component, "POSTGRES-USER"])
  value     = module.payment-database.user_name
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name      = join("-", [var.component, "POSTGRES-PASS"])
  value     = module.payment-database.postgresql_password
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name      = join("-", [var.component, "POSTGRES-HOST"])
  value     = module.payment-database.host_name
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name      = join("-", [var.component, "POSTGRES-PORT"])
  value     = module.payment-database.postgresql_listen_port
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name      = join("-", [var.component, "POSTGRES-DATABASE"])
  value     = module.payment-database.postgresql_database
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}


# region API (gateway)

data "azurerm_key_vault_secret" "s2s_client_secret" {
  name = "gateway-s2s-client-secret"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "azurerm_key_vault_secret" "s2s_client_id" {
  name = "gateway-s2s-client-id"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "template_file" "policy_template" {
  template = "${file("${path.module}/template/api-policy.xml")}"

  vars = {
    allowed_certificate_thumbprints = "${local.thumbprints_in_quotes_str}"
    s2s_client_id = "${data.azurerm_key_vault_secret.s2s_client_id.value}"
    s2s_client_secret = "${data.azurerm_key_vault_secret.s2s_client_secret.value}"
    s2s_base_url = "${local.s2sUrl}"
  }
}

data "template_file" "api_template" {
  template = "${file("${path.module}/template/api.json")}"
}

resource "azurerm_template_deployment" "telephony_api" {
  template_body       = data.template_file.api_template.rendered
  name                = "telephony-api-${var.env}"
  deployment_mode     = "Incremental"
  resource_group_name = "core-infra-${var.env}"
  count               = var.env != "preview" ? 1: 0

  parameters = {
    apiManagementServiceName  = "core-api-mgmt-${var.env}"
    apiName                   = "telephony-api"
    apiProductName            = "telephony"
    serviceUrl                = "http://payment-api-${var.env}.service.core-compute-${var.env}.internal"
    apiBasePath               = local.api_base_path
    policy                    = data.template_file.policy_template.rendered
  }
}

# endregion
