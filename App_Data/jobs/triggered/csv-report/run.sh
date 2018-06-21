#!/bin/sh

printf "Invoke email-pay-reports endpoint on :%s\n" $PAYMENT_SERVER_URL

curl -X POST $PAYMENT_SERVER_URL/payments/email-pay-reports -d {}

printf "\nFinished emailing csv reports"
