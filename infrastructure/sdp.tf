provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "mi_vault"
  subscription_id            = local.sdp_environment_ids[local.payment_environment].subscription
}

locals {
  sdp_cft_environments_map = {
    sandbox  = "sbox"
    aat      = "dev"
    perftest = "test"
    demo     = "dev"
  }

  payment_environment = lookup(local.sdp_cft_environments_map, var.env, var.env)

  sdp_environment_ids = {
    sbox = {
      subscription = "a8140a9e-f1b0-481f-a4de-09e2ee23f7ab"
    }
    dev = {
      subscription = "867a878b-cb68-4de5-9741-361ac9e178b6"
    }
    test = {
      subscription = "3eec5bde-7feb-4566-bfb6-805df6e10b90"
    }
    ithc = {
      subscription = "ba71a911-e0d6-4776-a1a6-079af1df7139"
    }
    stg = {
      subscription = "74dacd4f-a248-45bb-a2f0-af700dc4cf68"
    }
    prod = {
      subscription = "5ca62022-6aa2-4cee-aaa7-e7536c8d566c"
    }
  }
}

//Only adding read user for the replica created by module wa_task_management_api_database_flexible_replica. Server name has environment suffix due to duplicate mapping to dev.
module "sdp_db_user" {

  providers = {
    azurerm.sdp_vault = azurerm.mi_vault
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
    payment = ["public"]
  }

  common_tags = var.common_tags

  depends_on = [
    module.payment-database-v15
  ]

  count = var.env == "perftest" || var.env == "ithc" ? 0 : 1
}
