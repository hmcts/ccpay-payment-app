
output "vaultUri" {
  value = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

output "s2s_url" {
  value = "${local.s2sUrl}"
}

# this variable will be accessible to tests as API_GATEWAY_URL environment variable
output "api_gateway_url" {
  value = "https://core-api-mgmt-${var.env}.azure-api.net/${local.api_base_path}"
}
