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

variable "database_name" {
  type    = "string"
  default = "payment"
}

variable "postgresql_user" {
  type    = "string"
  default = "payment"
}

variable "common_tags" {
  type = "map"
}

variable "core_product" {
  type    = "string"
  default = "ccpay"
}

# thumbprint of the SSL certificate for API gateway tests
variable telephony_api_gateway_certificate_thumbprints {
  type = "list"
  default = []
}
