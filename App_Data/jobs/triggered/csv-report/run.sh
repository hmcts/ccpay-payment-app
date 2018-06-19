#!/bin/sh

curl -X POST http://{WEBSITE_HOSTNAME}/payment-csv-reports

echo "finished generating csv reports"
