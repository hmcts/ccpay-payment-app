# API Gateway Thumbprint
# Last three thumbprints are for the new API Gateway
# "7744A2F56BD3B73C0D7FED61309E1C65AF08538C" - Shravan test cert
# "BFE89B4BA1F47E048CFDF125C2E1BB4E2CC26083" - Dave test cert
# "3D4A8AD0F5EF4779347B0E448ABC1ADC4D61BDF9" - Exela Cert (old)
# "792265A947D0C76D4F67A0878B1D06E60976DFDA" - Exela Cert (current)
# "7620DCB455C20A072D8B613434CED819E48BD843" - New Exela Cert (testing app-gateway)
aks_subscription_id                           = "d025fece-ce99-4df2-b7a9-b649d3ff2060"
telephony_api_gateway_certificate_thumbprints = ["B1BF8007527F85085D7C4A3DC406A9A6D124D721", "68EDF481C5394D65962E9810913455D3EC635FA5", "13D1848E8B050CE55E4D41A35A60FF4A17E686A6", "C46826BF1E82DF37664F7A3678E6498D056DA4A9", "B660C97A7CC2734ABD41FBF9F6ADAA61B0C399D4", "BFE89B4BA1F47E048CFDF125C2E1BB4E2CC26083", "7620DCB455C20A072D8B613434CED819E48BD843"]
sku_name                                      = "GP_Gen5_2"
flexible_sku_name                             = "GP_Standard_D2s_v3"
sku_capacity                                  = "2"
additional_databases = [
  "postgresql-db2"
]
