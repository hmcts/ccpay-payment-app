#!/bin/sh

printf "Invoke status-update endpoint on :%s\n" $PAYMENT_SERVER_URL

curl -X POST $PAYMENT_SERVER_URL/payments/update -d {}

printf "\nFinished updating status"
