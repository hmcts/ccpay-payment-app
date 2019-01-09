variable "product" {
  type    = "string"
  default = "payment"
}

variable "component" {
  type    = "string"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}
variable "subscription" {
  type = "string"
}

variable "ilbIp"{}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type                        = "string"
  description                 = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "microservice" {
  type = "string"
  default = "payment-app"
}

variable "database_name" {
  type    = "string"
  default = "payment"
}

variable "postgresql_user" {
  type    = "string"
  default = "payment"
}

variable "vault_section" {
  default = "test"
}

variable "idam_api_url" {
  default = "http://betaDevBccidamAppLB.reform.hmcts.net"
}

variable "gov_pay_url" {
  default = "https://publicapi.payments.service.gov.uk/v1/payments"
}

variable "liberata_oauth2_base_url" {
  default = "https://bpacustomerportal.liberata.com/pba/public/api/v2"
}

variable "liberata_api_account_url" {
  default = "https://bpacustomerportal.liberata.com/pba/public/api/v2/account"
}

variable "liberata_oauth2_authorize_url" {
  default = "https://bpacustomerportal.liberata.com/pba/public/oauth/authorize"
}

variable "liberata_oauth2_token_url" {
  default = "https://bpacustomerportal.liberata.com/pba/public/oauth/token"
}

variable "gov_pay_operational_services" {
  default = "ccd_gw,api_gw"
}

variable "spring_mail_host" {
  default = "mta.reform.hmcts.net"
}
variable "spring_mail_port" {
  default = "25"
}
variable "spring_mail_properties_mail_smtp_starttls_enable" {
  default = "true"
}
variable "spring_mail_properties_email_smtp_ssl_trust" {
  default = "*"
}

variable "card_payments_report_scheduler_enabled" {
  default = "true"
}
variable "card_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "card_payments_email_subject" {
  default = "CNP Test Env: Card Payments Reconciliation Report"
}
variable "card_payments_email_message" {
  default = "Hi <br/><br/>Please find attached today''s reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"
}

variable "pba_cmc_payments_report_scheduler_enabled" {
  default = "true"
}
variable "pba_cmc_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "pba_cmc_payments_email_subject" {
  default = "CNP Test Env : PBA Reconciliation Report for CMC"
}
variable "pba_cmc_payments_email_message" {
  default = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"
}

variable "pba_divorce_payments_report_scheduler_enabled" {
  default = "true"
}
variable "pba_divorce_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "pba_divorce_payments_email_subject" {
  default = "CNP Test Env : PBA Reconciliation Report for Divorce"
}
variable "pba_divorce_payments_email_message" {
  default = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"
}

variable "payment_audit_file" {
  default = "%HOME%\\LogFiles\\payment-audit.log"
}

variable "capacity" {
  default = "1"
}

variable "feature_payments_search" {
  default = "true"
}

variable "feature_credit_account_payment_liberata_check" {
  default = "false"
}

variable "external_host_name" {
  default = "payment.nonprod.platform.hmcts.net"
}

variable "common_tags" {
  type = "map"
}

variable "core_product" {
  type    = "string"
  default = "ccpay"
}

variable "test_frontend_url" {
  type = "string"
  default = "https://moneyclaims.aat.platform.hmcts.net"
  description = "Optional front end URL to use for building redirect URI in idam tests "
}
