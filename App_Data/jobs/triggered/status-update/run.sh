#!/bin/sh
source ../s2sToken.sh

AUTH_TOKEN=$(s2sToken)
printf "Invoke status-update endpoint on :%s\n" $PAYMENT_SERVER_URL

curl -X PATCH $PAYMENT_SERVER_URL/jobs/card-payments-status-update -H "ServiceAuthorization: Bearer $AUTH_TOKEN" -d {}

printf "\nFinished updating status"
