#!/bin/sh

printf "Invoke status-update endpoint on :%s\n" $PAYMENT_SERVER_URL

curl -X PATCH $PAYMENT_SERVER_URL/jobs/card-payments-status-update -d {}

printf "\nFinished updating status"
