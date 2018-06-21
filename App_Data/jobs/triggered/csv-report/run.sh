#!/bin/sh

curl -X POST http://$WEBSITE_HOSTNAME/payments/email-pay-reports -d {}

printf "\nFinished generating csv reports"
