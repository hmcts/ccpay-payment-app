provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "sdp_vault"
  subscription_id            = local.sdp_environment_ids[local.payment_environment].subscription
}

locals {
  sdp_cft_environments_map = {
    aat      = "dev"
    //perftest = "test"
    demo     = "dev"
  }

  payment_environment = lookup(local.sdp_cft_environments_map, var.env, var.env)

  sdp_environment_ids = {
    dev = {
      subscription = "1c4f0704-a29e-403d-b719-b90c34ef14c9"
    }
    //test = {
   //   subscription = "7a4e3bd5-ae3a-4d0c-b441-2188fee3ff1c"
   // }
  //  ithc = {
  //    subscription = "7a4e3bd5-ae3a-4d0c-b441-2188fee3ff1c"
  //  }
  //  prod = {
  //    subscription = "8999dec3-0104-4a27-94ee-6588559729d1"
  //  }
  }
}

//Only adding read user for the replica created by module wa_task_management_api_database_flexible_replica. Server name has environment suffix due to duplicate mapping to dev.
module "sdp_db_user" {

  providers = {
    azurerm.sdp_vault = azurerm.sdp_vault
  }

  source = "git@github.com:hmcts/terraform-module-sdp-db-user?ref=master"
  env    = local.payment_environment

  server_name       = "${var.product}-postgres-db-v15-${var.env}"
  server_fqdn       = module.payment-database-v15.fqdn
  server_admin_user = module.payment-database-v15.username
  server_admin_pass = module.payment-database-v15.password

  databases = [
    {
      name : var.database_name
    }
  ]

  database_schemas = {
    cft_task_db = ["payment"]
  }

  common_tags = var.common_tags

  depends_on = [
    module.payment-database-v15
  ]
}
