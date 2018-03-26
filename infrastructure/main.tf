provider "vault" {
  // # tactical vault - for example: use `data "vault_generic_secret" "s2s_secret" {`
  address = "https://vault.reform.hmcts.net:6200"
}

data "vault_generic_secret" "gov_pay_keys_reference" {
  path = "secret/${var.vault_section}/cc/payment/api/gov-pay-keys/reference"
}

data "vault_generic_secret" "gov_pay_keys_cmc" {
  path = "secret/${var.vault_section}/cc/payment/api/gov-pay-keys/cmc"
}

data "vault_generic_secret" "gov_pay_keys_probate" {
  path = "secret/${var.vault_section}/cc/payment/api/gov-pay-keys/probate"
}

data "vault_generic_secret" "gov_pay_keys_divorce" {
  path = "secret/${var.vault_section}/cc/payment/api/gov-pay-keys/divorce"
}

module "payment-api" {
  source   = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product  = "${var.product}-api"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend  = false

  app_settings = {
    # db
    SPRING_DATASOURCE_USERNAME = "${module.payment-database.user_name}"
    SPRING_DATASOURCE_PASSWORD = "${module.payment-database.postgresql_password}"
    SPRING_DATASOURCE_URL = "jdbc:postgresql://${module.payment-database.host_name}:${module.payment-database.postgresql_listen_port}/${module.payment-database.postgresql_database}?ssl=true"

    # idam
    AUTH_IDAM_CLIENT_BASEURL = "${var.idam_api_url}"
    # service-auth-provider
    AUTH_PROVIDER_SERVICE_CLIENT_BASEURL = "${var.s2s_url}"

    # gov pay keys
    GOV_PAY_URL = "${var.gov_pay_url}"
    GOV_PAY_AUTH_KEY_REFERENCE = "${data.vault_generic_secret.gov_pay_keys_reference.data["value"]}"
    GOV_PAY_AUTH_KEY_CMC = "${data.vault_generic_secret.gov_pay_keys_cmc.data["value"]}"
    GOV_PAY_AUTH_KEY_PROBATE_FRONTEND = "${data.vault_generic_secret.gov_pay_keys_probate.data["value"]}"
    GOV_PAY_AUTH_KEY_DIVORCE_FRONTEND = "${data.vault_generic_secret.gov_pay_keys_divorce.data["value"]}"

    SPRING_MAIL_HOST = "${var.spring_mail_host}"
    SPRING_MAIL_PORT = "${var.spring_mail_port}"
    SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "${var.spring_mail_properties_mail_smtp_starttls_enable}"
    EMAIL_SMTP_SSL_TRUST = "${var.spring_mail_properties_email_smtp_ssl_trust}"

    CARD_PAYMENTS_REPORT_SCHEDULE = "${var.card_payments_report_schedule}"
    CARD_PAYMENTS_REPORT_SCHEDULER_ENABLED = "${var.card_payments_report_scheduler_enabled}"
    CARD_PAYMENTS_EMAIL_FROM = "${var.card_payments_email_from}"
    CARD_PAYMENTS_EMAIL_TO = "${var.card_payments_email_to}"
    CARD_PAYMENTS_EMAIL_SUBJECT = "${var.card_payments_email_subject}"
    CARD_PAYMENTS_EMAIL_MESSAGE = "${var.card_payments_email_message}"

    PBA_PAYMENTS_REPORT_SCHEDULE = "${var.pba_payments_report_schedule}"
    PBA_PAYMENTS_REPORT_SCHEDULER_ENABLED = "${var.pba_payments_report_scheduler_enabled}"
    PBA_PAYMENTS_EMAIL_FROM = "${var.pba_payments_email_from}"
    PBA_PAYMENTS_EMAIL_TO = "${var.pba_payments_email_to}"
    PBA_PAYMENTS_EMAIL_SUBJECT = "${var.pba_payments_email_subject}"
    PBA_PAYMENTS_EMAIL_MESSAGE = "${var.pba_payments_email_message}"

    FEES_REGISTER_URL = "${var.fees_register_url}"

    # logging vars
    REFORM_SERVICE_NAME = "payment-api"
    REFORM_TEAM = "cc"
    REFORM_ENVIRONMENT = "${var.env}"
    ROOT_APPENDER = "JSON_CONSOLE"

  }
}

module "payment-database" {
  source              = "git@github.com:hmcts/moj-module-postgres?ref=master"
  product             = "${var.product}"
  location            = "West Europe"
  env                 = "${var.env}"
  postgresql_user   = "fradmin"
}

module "key-vault" {
  source              = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  product             = "${var.product}"
  env                 = "${var.env}"
  tenant_id           = "${var.tenant_id}"
  object_id           = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${module.payment-api.resource_group_name}"
  product_group_object_id = "56679aaa-b343-472a-bb46-58bbbfde9c3d"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name      = "payment-POSTGRES-USER"
  value     = "${module.payment-database.user_name}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name      = "payment-POSTGRES-PASS"
  value     = "${module.payment-database.postgresql_password}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name      = "payment-POSTGRES-HOST"
  value     = "${module.payment-database.host_name}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name      = "payment-POSTGRES-PORT"
  value     = "${module.payment-database.postgresql_listen_port}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name      = "payment-POSTGRES-DATABASE"
  value     = "${module.payment-database.postgresql_database}"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

