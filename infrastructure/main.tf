locals {
  aseName = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.aseName}"

  previewVaultName = "payment-shared-aat"
  nonPreviewVaultName = "payment-shared-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

  s2sUrl = "http://rpe-service-auth-provider-${local.local_env}.service.${local.local_ase}.internal"
  fees_register_url = "http://fees-register-api-${local.local_env}.service.${local.local_ase}.internal"

  website_url = "http://${var.product}-api-${local.local_env}.service.${local.local_ase}.internal"
}

data "azurerm_key_vault" "payment_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault_secret" "gov_pay_keys_reference" {
  name = "gov-pay-keys-reference"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "gov_pay_keys_cmc" {
  name = "gov-pay-keys-cmc"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "gov_pay_keys_probate" {
  name = "gov-pay-keys-probate"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "gov_pay_keys_divorce" {
  name = "gov-pay-keys-divorce"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}
data "azurerm_key_vault_secret" "card_payments_email_to" {
  name = "card-payments-email-to"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}
data "azurerm_key_vault_secret" "pba_payments_email_to" {
  name = "pba-payments-email-to"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

module "payment-api" {
  source   = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product  = "${var.product}-api"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend  = false
  capacity = "${var.capacity}"

  app_settings = {
    # db
    SPRING_DATASOURCE_USERNAME = "${module.payment-database.user_name}"
    SPRING_DATASOURCE_PASSWORD = "${module.payment-database.postgresql_password}"
    SPRING_DATASOURCE_URL = "jdbc:postgresql://${module.payment-database.host_name}:${module.payment-database.postgresql_listen_port}/${module.payment-database.postgresql_database}?ssl=true"

    # idam
    AUTH_IDAM_CLIENT_BASEURL = "${var.idam_api_url}"
    # service-auth-provider
    AUTH_PROVIDER_SERVICE_CLIENT_BASEURL = "${local.s2sUrl}"

    # gov pay keys
    GOV_PAY_URL = "${var.gov_pay_url}"
    GOV_PAY_AUTH_KEY_REFERENCE = "${data.azurerm_key_vault_secret.gov_pay_keys_reference.value}"
    GOV_PAY_AUTH_KEY_CMC = "${data.azurerm_key_vault_secret.gov_pay_keys_cmc.value}"
    GOV_PAY_AUTH_KEY_PROBATE_FRONTEND = "${data.azurerm_key_vault_secret.gov_pay_keys_probate.value}"
    GOV_PAY_AUTH_KEY_DIVORCE_FRONTEND = "${data.azurerm_key_vault_secret.gov_pay_keys_divorce.value}"
    GOV_PAY_OPERATIONAL_SERVICES = "${var.gov_pay_operational_services}"

    SPRING_MAIL_HOST = "${var.spring_mail_host}"
    SPRING_MAIL_PORT = "${var.spring_mail_port}"
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "${var.spring_mail_properties_mail_smtp_starttls_enable}"
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_TRUST = "${var.spring_mail_properties_email_smtp_ssl_trust}"

    CARD_PAYMENTS_REPORT_SCHEDULE = "${var.card_payments_report_schedule}"
    CARD_PAYMENTS_REPORT_SCHEDULER_ENABLED = "${var.card_payments_report_scheduler_enabled}"
    CARD_PAYMENTS_EMAIL_FROM = "${var.card_payments_email_from}"
    CARD_PAYMENTS_EMAIL_TO = "${data.azurerm_key_vault_secret.card_payments_email_to.value}"
    CARD_PAYMENTS_EMAIL_SUBJECT = "${var.card_payments_email_subject}"
    CARD_PAYMENTS_EMAIL_MESSAGE = "${var.card_payments_email_message}"

    PBA_PAYMENTS_REPORT_SCHEDULE = "${var.pba_payments_report_schedule}"
    PBA_PAYMENTS_REPORT_SCHEDULER_ENABLED = "${var.pba_payments_report_scheduler_enabled}"
    PBA_PAYMENTS_EMAIL_FROM = "${var.pba_payments_email_from}"
    PBA_PAYMENTS_EMAIL_TO = "${data.azurerm_key_vault_secret.pba_payments_email_to.value}"
    PBA_PAYMENTS_EMAIL_SUBJECT = "${var.pba_payments_email_subject}"
    PBA_PAYMENTS_EMAIL_MESSAGE = "${var.pba_payments_email_message}"

    FEES_REGISTER_URL = "${local.fees_register_url}"
    FEATURE_PAYMENTS_SEARCH = "${var.feature_payments_search}"

    PAYMENT_SERVER_URL = "${local.website_url}"

    # logging vars
    REFORM_SERVICE_NAME = "payment-api"
    REFORM_TEAM = "cc"
    REFORM_ENVIRONMENT = "${var.env}"
    ROOT_APPENDER = "JSON_CONSOLE"

    PAYMENT_AUDIT_FILE = "${var.payment_audit_file}"
  }
}

module "payment-database" {
  source = "git@github.com:hmcts/moj-module-postgres?ref=cnp-449-tactical"
  product = "${var.product}-postgres-db"
  location = "${var.location}"
  env = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
}
