#!/bin/sh
source $WEBROOT_PATH/App_Data/jobs/triggered/s2sToken.sh

if [ "$SLOT" == "PRODUCTION" ]
then
    AUTH_TOKEN=$(s2sToken)
    printf "Invoking email PBA reports on :%s \n" $PAYMENT_SERVER_URL
    curl -X POST $PAYMENT_SERVER_URL/jobs/email-pay-reports?payment_method=PBA'&'service_name=CMC -H "ServiceAuthorization: Bearer $AUTH_TOKEN" -d {}
    curl -X POST $PAYMENT_SERVER_URL/jobs/email-pay-reports?payment_method=PBA'&'service_name=DIVORCE -H "ServiceAuthorization: Bearer $AUTH_TOKEN" -d {}
    printf "\nFinished emailing PBA csv reports"
else
    printf "Unsupported app slot:%s to run this job. \n" $SLOT
fi


