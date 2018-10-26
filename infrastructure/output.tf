output "vaultUri" {
  value = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "idam_api_url" {
  value = "${var.idam_api_url}"
}

output "s2s_url" {
  value = "${local.s2sUrl}"
}

output "OAUTH2_REDIRECT_URI" {
  value = "${var.test_frontend_url}/oauth2"
}
