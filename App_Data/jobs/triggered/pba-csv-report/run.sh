#!/bin/sh

if [ "$SLOT" == "PRODUCTION" ]
then
    printf "Invoking email PBA reports on :%s \n" $PAYMENT_SERVER_URL
    curl -X POST $PAYMENT_SERVER_URL/jobs/email-pay-reports?payment_method=PBA&service_name=CMC -d {}
    curl -X POST $PAYMENT_SERVER_URL/jobs/email-pay-reports?payment_method=PBA&service_name=DIVORCE -d {}
    printf "\nFinished emailing PBA csv reports"
else
    printf "Unsupported app slot:%s to run this job. \n" $SLOT
fi


