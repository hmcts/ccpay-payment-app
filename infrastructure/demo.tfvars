telephony_api_gateway_certificate_thumbprints = ["B1BF8007527F85085D7C4A3DC406A9A6D124D721","68EDF481C5394D65962E9810913455D3EC635FA5", "13D1848E8B050CE55E4D41A35A60FF4A17E686A6","C46826BF1E82DF37664F7A3678E6498D056DA4A9", "B660C97A7CC2734ABD41FBF9F6ADAA61B0C399D4"]
sku_name = "GP_Gen5_2"
sku_capacity = "2"

variable "additional_databases" {
    default = [
    "postgresql-db2"
  ]
}

additional_databases = [
    "postgresql-db2"
]
