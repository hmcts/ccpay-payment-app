variable "product" {
  type    = "string"
  default = "payment"
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

variable "vault_section" {
  default = "test"
}

variable "idam_api_url" {
  default = "http://betaDevBccidamAppLB.reform.hmcts.net"
}

variable "s2s_url" {
  default = "http://betaDevBccidamS2SLB.reform.hmcts.net"
}

variable "gov_pay_url" {
  default = "https://publicapi.payments.service.gov.uk/v1/payments"
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
variable "card_payments_report_schedule" {
  default = "0 */30 * * * ?"
}
variable "card_payments_report_scheduler_enabled" {
  default = "false"
}
variable "card_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "card_payments_email_subject" {
  default = "Test Env: Card Payments Reconciliation Report"
}
variable "card_payments_email_message" {
  default = "Hi <br/><br/>Please find attached today''s reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"
}
variable "pba_payments_report_schedule" {
  default = "0 */30 * * * ?"
}
variable "pba_payments_report_scheduler_enabled" {
  default = "false"
}
variable "pba_payments_email_from" {
  default = "no-reply@reform.hmcts.net"
}
variable "pba_payments_email_subject" {
  default = "Test Env : PBA Reconciliation Report'"
}
variable "pba_payments_email_message" {
  default = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"
}

variable "fees_register_url" {
  default = "https://test.fees-register.reform.hmcts.net:4431"
}

variable "payment_audit_file" {
  default = "%HOME%\\LogFiles\\payment-audit.log"
}
