# Subscription keys for the CFT APIM

# Internal subscription - Fee and Payment DTS Team
resource "azurerm_api_management_subscription" "fee_pay_team_telephony_subscription" {
  api_management_name = local.cft_api_mgmt_name
  resource_group_name = local.cft_api_mgmt_rg
  product_id          = module.cft_api_mgmt_product.id
  display_name        = "Telephony API - Fee and Pay DTS Team Subscription"
  state               = "active"
  provider            = azurerm.aks-cftapps
}

resource "azurerm_key_vault_secret" "fee_pay_team_bulk_scan_subscription_key" {
  name         = "fee-pay-team-telephony-cft-apim-subscription-key"
  value        = azurerm_api_management_subscription.fee_pay_team_telephony_subscription.primary_key
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

# Supplier subscription - PCI Pal
resource "azurerm_api_management_subscription" "pcipal_supplier_subscription" {
  api_management_name = local.cft_api_mgmt_name
  resource_group_name = local.cft_api_mgmt_rg
  product_id          = module.cft_api_mgmt_product.id
  display_name        = "Telephony API - PCIPal Subscription"
  state               = "active"
  provider            = azurerm.aks-cftapps
}

resource "azurerm_key_vault_secret" "pcipal_supplier_subscription_key" {
  name         = "pcipal-cft-apim-subscription-key"
  value        = azurerm_api_management_subscription.pcipal_supplier_subscription.primary_key
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}
