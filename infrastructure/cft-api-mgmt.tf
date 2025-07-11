# Note for API docs see - https://github.com/hmcts/cnp-api-docs/tree/master/docs/specs

locals {
  cft_api_mgmt_suffix = var.apim_suffix == "" ? var.env : var.apim_suffix
  cft_api_mgmt_name   = join("-", ["cft-api-mgmt", local.cft_api_mgmt_suffix])
  cft_api_mgmt_rg     = join("-", ["cft", var.env, "network-rg"])
}

data "template_file" "cft_policy_template" {
  template = file(join("", [path.module, "/template/cft-api-policy.xml"]))

  vars = {
    allowed_certificate_thumbprints = local.thumbprints_in_quotes_str
    s2s_base_url                    = local.s2sUrl
  }
}

module "cft_api_mgmt_product" {
  source                        = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"
  name                          = var.product_name
  api_mgmt_name                 = local.cft_api_mgmt_name
  api_mgmt_rg                   = local.cft_api_mgmt_rg
  approval_required             = "false"
  subscription_required         = "true"
  product_access_control_groups = ["developers"]
  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "cft_api_mgmt_api" {
  source        = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"
  name          = join("-", [var.product_name, "api"])
  display_name  = "Telephony API"
  api_mgmt_name = local.cft_api_mgmt_name
  api_mgmt_rg   = local.cft_api_mgmt_rg
  product_id    = module.cft_api_mgmt_product.product_id
  path          = local.api_base_path
  service_url   = "http://payment-api-${var.env}.service.core-compute-${var.env}.internal"
  swagger_url   = "https://raw.githubusercontent.com/hmcts/cnp-api-docs/master/docs/specs/ccpay-payment-app.telephony.json"
  protocols     = ["http", "https"]
  revision      = "1"
  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "cft_api_mgmt_policy" {
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = local.cft_api_mgmt_name
  api_mgmt_rg            = local.cft_api_mgmt_rg
  api_name               = module.cft_api_mgmt_api.name
  api_policy_xml_content = data.template_file.cft_policy_template.rendered
  providers = {
    azurerm = azurerm.aks-cftapps
  }
}
