variable "product" {
  type    = string
  default = "payment"
}

variable "component" {
  type = string
}

variable "location" {
  type    = string
  default = "UK South"
}

variable "env" {
  type = string
}
variable "subscription" {
  type = string
}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type        = string
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "microservice" {
  type    = string
  default = "payment-app"
}

variable "database_name" {
  type    = string
  default = "payment"
}

variable "postgresql_user" {
  type    = string
  default = "payment"
}

variable "common_tags" {
  type = map(string)
}

variable "core_product" {
  type    = string
  default = "ccpay"
}

variable "product_name" {
  type    = string
  default = "telephony"
}

# thumbprint of the SSL certificate for API gateway tests
variable "telephony_api_gateway_certificate_thumbprints" {
  type    = list(string)
  default = []
}


variable "postgresql_flexible_sql_version" {
  default = "15"
}

variable "postgresql_flexible_server_port" {
  default = "5432"
}

variable "flexible_sku_name" {
  default = "GP_Standard_D2s_v3"
}

variable "aks_subscription_id" {}

variable "additional_databases" {
  default = []
}

variable "apim_suffix" {
  default = ""
}

variable "db_monitor_action_group_name" {
  description = "The name of the Action Group to create."
  type        = string
  default     = "db_monitor_ag"
}

variable "db_alert_email_address_key" {
  description = "Email address key in azure Key Vault."
  type        = string
  default     = "db-alert-monitoring-email-address"
}
