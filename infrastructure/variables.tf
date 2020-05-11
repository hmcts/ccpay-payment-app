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

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default = ""
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

variable "paybubble_home_url" {
  default = "https://ccpay-bubble-frontend-aat.service.core-compute-aat.internal"
}

variable "pci_pal_callback_url" {
  default = "https://core-api-mgmt-aat.azure-api.net/telephony-api/telephony/callback"
}

variable "core_case_data_api_url" {
  default = "http://ccd-data-store-api-aat.service.core-compute-aat.internal"
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

variable "callback_payments_cutoff_time_in_minutes" {
  default = "0"
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

variable "bar_payments_report_scheduler_enabled" {
  default = "true"
}
variable "bar_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "bar_payments_email_subject" {
  default = "CNP Test Env: Bar Payments Reconciliation Report"
}
variable "bar_payments_email_message" {
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

variable "pba_probate_payments_report_scheduler_enabled" {
  default = "true"
}
variable "pba_probate_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "pba_probate_payments_email_subject" {
  default = "CNP Test Env : PBA Reconciliation Report for PROBATE"
}
variable "pba_probate_payments_email_message" {
  default = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"
}

variable "pba_finrem_payments_report_scheduler_enabled" {
  default = "true"
}
variable "pba_finrem_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "pba_finrem_payments_email_subject" {
  default = "CNP Test Env : PBA Reconciliation Report for FINREM"
}
variable "pba_finrem_payments_email_message" {
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
  default = "Hi <br/><br/>Please find attached today's Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team <br/><br/>"
}

variable "pba_fpl_payments_report_scheduler_enabled" {
  default = "true"
}
variable "pba_fpl_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "pba_fpl_payments_email_subject" {
  default = "CNP Test Env : PBA FPL Reconciliation Report"
}
variable "pba_fpl_payments_email_message" {
  default = "Hi <br/><br/>Please find attached today's Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team <br/><br/>"
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
  default = "true"
}

variable "feature_duplicate_payment_check" {
  default = "true"
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

# thumbprint of the SSL certificate for API gateway tests
variable telephony_api_gateway_certificate_thumbprints {
  type = "list"
  default = []
}

variable "spring_profiles_active" {
  default = "default"
}
