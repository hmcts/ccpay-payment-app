#!/bin/sh

curl -X POST http://$WEBSITE_HOSTNAME/payments/email-pay-reports

echo "finished generating csv reports"
