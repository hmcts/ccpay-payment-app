provider "azurerm" {
  version = "1.19.0"
}

locals {
  app_full_name = "${var.product}-${var.component}"
  aseName = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.aseName}"

  previewVaultName = "${var.core_product}-aat"
  nonPreviewVaultName = "${var.core_product}-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

  s2sUrl = "http://rpe-service-auth-provider-${local.local_env}.service.${local.local_ase}.internal"
  fees_register_url = "http://fees-register-api-${local.local_env}.service.${local.local_ase}.internal"

  website_url = "http://${var.product}-api-${local.local_env}.service.${local.local_ase}.internal"

  asp_name = "${var.env == "prod" ? "payment-api-prod" : "${var.core_product}-${var.env}"}"

  #region API gateway
  api_policy = "${file("template/api-policy.xml")}"
  api_base_path = "telephony-api"
  # endregion
}

data "azurerm_key_vault" "payment_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${var.core_product}-${local.local_env}"
}

data "azurerm_key_vault_secret" "pci_pal_account_id_cmc" {
  name = "pci-pal-account-id-cmc"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "pci_pal_account_id_probate" {
  name = "pci-pal-account-id-probate"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "pci_pal_account_id_divorce" {
  name = "pci-pal-account-id-divorce"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "pci_pal_api_url" {
  name = "pci-pal-api-url"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "pci_pal_api_key" {
  name = "pci-pal-api-key"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "liberata_keys_oauth2_client_id" {
  name = "liberata-keys-oauth2-client-id"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "liberata_keys_oauth2_client_secret" {
  name = "liberata-keys-oauth2-client-secret"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "liberata_keys_oauth2_username" {
  name = "liberata-keys-oauth2-username"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "liberata_keys_oauth2_password" {
  name = "liberata-keys-oauth2-password"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
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

data "azurerm_key_vault_secret" "pba_cmc_payments_email_to" {
  name = "pba-payments-email-to"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "pba_divorce_payments_email_to" {
  name = "pba-divorce-payments-email-to"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "webjob_s2s_client_secret" {
  name = "gateway-s2s-client-secret"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "webjob_s2s_client_id" {
  name = "gateway-s2s-client-id"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

module "payment-api" {
  source   = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product  = "${var.product}-api"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend  = "${var.env != "preview" ? 1: 0}"
  additional_host_name = "${var.env != "preview" ? var.external_host_name : "null"}"
  https_only="false"
  capacity = "${var.capacity}"
  common_tags     = "${var.common_tags}"
  asp_name = "${local.asp_name}"
  asp_rg = "${local.asp_name}"

  app_settings = {
    # db
    SPRING_DATASOURCE_USERNAME = "${module.payment-database.user_name}"
    SPRING_DATASOURCE_PASSWORD = "${module.payment-database.postgresql_password}"
    SPRING_DATASOURCE_URL = "jdbc:postgresql://${module.payment-database.host_name}:${module.payment-database.postgresql_listen_port}/${module.payment-database.postgresql_database}?sslmode=require"

    # disabled liquibase at startup as there is a separate pipleline step (enableDbMigration)
    SPRING_LIQUIBASE_ENABLED = "false"

    # idam
    AUTH_IDAM_CLIENT_BASEURL = "${var.idam_api_url}"
    # service-auth-provider
    AUTH_PROVIDER_SERVICE_CLIENT_BASEURL = "${local.s2sUrl}"

    # PCI PAL
    PCI_PAL_ACCOUNT_ID_CMC = "${data.azurerm_key_vault_secret.pci_pal_account_id_cmc.value}"
    PCI_PAL_ACCOUNT_ID_PROBATE = "${data.azurerm_key_vault_secret.pci_pal_account_id_probate.value}"
    PCI_PAL_ACCOUNT_ID_DIVORCE = "${data.azurerm_key_vault_secret.pci_pal_account_id_divorce.value}"
    PCI_PAL_API_URL = "${data.azurerm_key_vault_secret.pci_pal_api_url.value}"
    PCI_PAL_API_KEY = "${data.azurerm_key_vault_secret.pci_pal_api_key.value}"

    # liberata
    LIBERATA_OAUTH2_CLIENT_ID = "${data.azurerm_key_vault_secret.liberata_keys_oauth2_client_id.value}"
    LIBERATA_OAUTH2_CLIENT_SECRET = "${data.azurerm_key_vault_secret.liberata_keys_oauth2_client_secret.value}"
    LIBERATA_OAUTH2_USERNAME = "${data.azurerm_key_vault_secret.liberata_keys_oauth2_username.value}"
    LIBERATA_OAUTH2_PASSWORD = "${data.azurerm_key_vault_secret.liberata_keys_oauth2_password.value}"
    LIBERATA_API_ACCOUNT_URL = "${var.liberata_api_account_url}"
    LIBERATA_OAUTH2_BASE_URL = "${var.liberata_oauth2_base_url}"
    LIBERATA_OAUTH2_AUTHORIZE_URL = "${var.liberata_oauth2_authorize_url}"
    LIBERATA_OAUTH2_TOKEN_URL = "${var.liberata_oauth2_token_url}"

    # gov pay keys
    GOV_PAY_URL = "${var.gov_pay_url}"
    GOV_PAY_AUTH_KEY_REFERENCE = "${data.azurerm_key_vault_secret.gov_pay_keys_reference.value}"
    GOV_PAY_AUTH_KEY_CMC = "${data.azurerm_key_vault_secret.gov_pay_keys_cmc.value}"
    GOV_PAY_AUTH_KEY_PROBATE_FRONTEND = "${data.azurerm_key_vault_secret.gov_pay_keys_probate.value}"
    GOV_PAY_AUTH_KEY_DIVORCE_FRONTEND = "${data.azurerm_key_vault_secret.gov_pay_keys_divorce.value}"
    GOV_PAY_OPERATIONAL_SERVICES = "${var.gov_pay_operational_services}"

    # S2S trusted services
    TRUSTED_S2S_SERVICE_NAMES="cmc,probate_frontend,divorce_frontend,ccd_gw,bar_api,api_gw,pui_webapp,finrem_payment_service,ccpay_bubble,jui_webapp"

    SPRING_MAIL_HOST = "${var.spring_mail_host}"
    SPRING_MAIL_PORT = "${var.spring_mail_port}"
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "${var.spring_mail_properties_mail_smtp_starttls_enable}"
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_TRUST = "${var.spring_mail_properties_email_smtp_ssl_trust}"

    CARD_PAYMENTS_REPORT_SCHEDULER_ENABLED = "${var.card_payments_report_scheduler_enabled}"
    CARD_PAYMENTS_EMAIL_FROM = "${var.card_payments_email_from}"
    CARD_PAYMENTS_EMAIL_TO = "${data.azurerm_key_vault_secret.card_payments_email_to.value}"
    CARD_PAYMENTS_EMAIL_SUBJECT = "${var.card_payments_email_subject}"
    CARD_PAYMENTS_EMAIL_MESSAGE = "${var.card_payments_email_message}"

    PBA_CMC_PAYMENTS_REPORT_SCHEDULER_ENABLED = "${var.pba_cmc_payments_report_scheduler_enabled}"
    PBA_CMC_PAYMENTS_EMAIL_FROM = "${var.pba_cmc_payments_email_from}"
    PBA_CMC_PAYMENTS_EMAIL_TO = "${data.azurerm_key_vault_secret.pba_cmc_payments_email_to.value}"
    PBA_CMC_PAYMENTS_EMAIL_SUBJECT = "${var.pba_cmc_payments_email_subject}"
    PBA_CMC_PAYMENTS_EMAIL_MESSAGE = "${var.pba_cmc_payments_email_message}"

    PBA_DIVORCE_PAYMENTS_REPORT_SCHEDULER_ENABLED = "${var.pba_divorce_payments_report_scheduler_enabled}"
    PBA_DIVORCE_PAYMENTS_EMAIL_FROM = "${var.pba_divorce_payments_email_from}"
    PBA_DIVORCE_PAYMENTS_EMAIL_TO = "${data.azurerm_key_vault_secret.pba_divorce_payments_email_to.value}"
    PBA_DIVORCE_PAYMENTS_EMAIL_SUBJECT = "${var.pba_divorce_payments_email_subject}"
    PBA_DIVORCE_PAYMENTS_EMAIL_MESSAGE = "${var.pba_divorce_payments_email_message}"

    FEES_REGISTER_URL = "${local.fees_register_url}"
    FEATURE_PAYMENTS_SEARCH = "${var.feature_payments_search}"
    FEATURE_CREDIT_ACCOUNT_PAYMENT_LIBERATA_CHECK = "${var.feature_credit_account_payment_liberata_check}"

    PAYMENT_SERVER_URL = "${local.website_url}"

    # logging vars
    REFORM_SERVICE_NAME = "payment-api"
    REFORM_TEAM = "cc"
    REFORM_ENVIRONMENT = "${var.env}"
    ROOT_APPENDER = "JSON_CONSOLE"

    PAYMENT_AUDIT_FILE = "${var.payment_audit_file}"
    # webjob security
    WEBJOB_S2S_CLIENT_ID = "${data.azurerm_key_vault_secret.webjob_s2s_client_id.value}"
    WEBJOB_S2S_CLIENT_SECRET = "${data.azurerm_key_vault_secret.webjob_s2s_client_secret.value}"

    ASB_CONNECTION_STRING ="${data.terraform_remote_state.shared_infra.topic_primary_send_and_listen_connection_string}"
  }
}

module "payment-database" {
  source = "git@github.com:hmcts/moj-module-postgres?ref=master"
  product = "${var.product}-postgres-db"
  location = "${var.location}"
  env = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  common_tags     = "${var.common_tags}"
}

# Populate Vault with DB info

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name      = "${local.app_full_name}-POSTGRES-USER"
  value     = "${module.payment-database.user_name}"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name      = "${local.app_full_name}-POSTGRES-PASS"
  value     = "${module.payment-database.postgresql_password}"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name      = "${local.app_full_name}-POSTGRES-HOST"
  value     = "${module.payment-database.host_name}"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name      = "${local.app_full_name}-POSTGRES-PORT"
  value     = "${module.payment-database.postgresql_listen_port}"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name      = "${local.app_full_name}-POSTGRES-DATABASE"
  value     = "${module.payment-database.postgresql_database}"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}


# region API (gateway)

data "azurerm_key_vault_secret" "s2s_client_secret" {
  name = "gateway-s2s-client-secret"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "s2s_client_id" {
  name = "gateway-s2s-client-id"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "template_file" "policy_template" {
  template = "${file("${path.module}/template/api-policy.xml")}"

  vars {
    s2s_client_id = "${data.azurerm_key_vault_secret.s2s_client_id.value}"
    s2s_client_secret = "${data.azurerm_key_vault_secret.s2s_client_secret.value}"
    s2s_base_url = "${local.s2sUrl}"
  }
}

data "template_file" "api_template" {
  template = "${file("${path.module}/template/api.json")}"
}

resource "azurerm_template_deployment" "telephony_api" {
  template_body       = "${data.template_file.api_template.rendered}"
  name                = "telephony-api-${var.env}"
  deployment_mode     = "Incremental"
  resource_group_name = "core-infra-${var.env}"
  count               = "${var.env != "preview" ? 1: 0}"

  parameters = {
    apiManagementServiceName  = "core-api-mgmt-${var.env}"
    apiName                   = "telephony-api"
    apiProductName            = "telephony"
    serviceUrl                = "http://payment-api-${var.env}.service.core-compute-${var.env}.internal"
    apiBasePath               = "${local.api_base_path}"
    policy                    = "${data.template_file.policy_template.rendered}"
  }
}

# endregion
